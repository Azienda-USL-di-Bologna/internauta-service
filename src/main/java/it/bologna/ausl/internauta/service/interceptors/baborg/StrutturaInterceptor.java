package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.StoricoRelazioneRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Ambiti;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Predicati;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Tipi;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "struttura-interceptor")
@Order(1)
public class StrutturaInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrutturaInterceptor.class);

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    PermissionRepositoryAccess permissionRepositoryAccess;

    @Autowired
    StoricoRelazioneRepository storicoRelazioneRepository;

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    ParametriAziende parametriAziende;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    InternautaUtils internautaUtils;

    @Autowired
    public ObjectMapper objectMapper;

    @Override
    public Class getTargetEntityClass() {
        return Struttura.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        boolean isCA = userInfoService.isCA(utente);
        boolean isCI = userInfoService.isCI(utente);
        boolean isSD = userInfoService.isSD(utente);

        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case GetPermessiStrutturePec:
                        /* Nel caso di GetPermessiStrutturePec in Data avremo l'id della PEC della quale si chiedono i permessi */
                        String idPec = additionalData.get(AdditionalData.Keys.idPec.toString());
                        Pec pec = new Pec(Integer.parseInt(idPec));
                        try {
                            List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(
                                    Arrays.asList(new Pec[]{pec}),
                                    Arrays.asList(new String[]{Predicati.SPEDISCE.toString(), Predicati.SPEDISCE_PRINCIPALE.toString()}),
                                    Arrays.asList(new String[]{Ambiti.PECG.toString()}),
                                    Arrays.asList(new String[]{Tipi.PEC.toString()}), false);
                            if (subjectsWithPermissionsOnObject == null) {
                                initialPredicate = Expressions.FALSE.eq(true);
                            } else {
                                BooleanExpression permessoFilter = QStruttura.struttura.id.in(
                                        subjectsWithPermissionsOnObject
                                                .stream()
                                                .map(p -> p.getSoggetto().getIdProvenienza()).collect(Collectors.toList()));
                                initialPredicate = permessoFilter.and(initialPredicate);
                            }
                            /* Conserviamo i dati estratti dalla BlackBox */
                            this.httpSessionData.putData(HttpSessionData.Keys.StruttureWithPecPermissions, subjectsWithPermissionsOnObject);
                        } catch (BlackBoxPermissionException ex) {
                            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                            throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                        }
                        break;
                    case FilterStrutturePoolsRuolo:
                        try {
                        String ruoliNomeBreveString = additionalData.get(InternautaConstants.AdditionalData.Keys.ruoli.toString());
                        if (!isCA && !isCI && !isSD && !StringUtils.isEmpty(ruoliNomeBreveString)) {
                            List<ParametroAziende> filtraResponsabiliParams = parametriAziende.getParameters("AccessoPoolFiltratoPerRuolo", new Integer[]{utente.getIdAzienda().getId()});
                            if (filtraResponsabiliParams != null && !filtraResponsabiliParams.isEmpty() && parametriAziende.getValue(filtraResponsabiliParams.get(0), Boolean.class)) {
                                Integer mascheraBit = internautaUtils.getSommaMascheraBit(ruoliNomeBreveString);

                                Map<String, Integer> struttureRuoloEFiglie = objectMapper.convertValue(
                                        storicoRelazioneRepository.getStruttureRuoloEFiglie(mascheraBit, utente.getId(), ZonedDateTime.now()).get("result"),
                                        new TypeReference<Map<String, Integer>>() {
                                }
                                );
                                if (struttureRuoloEFiglie != null && !struttureRuoloEFiglie.isEmpty()) {
                                    List<Integer> struttureResposabilita = struttureRuoloEFiglie.keySet().stream().map(s -> Integer.valueOf(s)).collect(Collectors.toList());
                                    BooleanExpression filterRuolo = QStruttura.struttura.idStrutturaPadre.id.in(struttureResposabilita);
                                    initialPredicate = filterRuolo.and(initialPredicate);
                                } else {
                                    initialPredicate = Expressions.FALSE.eq(true);
                                }
                            } else {
                                initialPredicate = Expressions.FALSE.eq(true);
                            }
                        }
                    } catch (Exception ex) {
                        throw new AbortLoadInterceptorException("errore nella chiamata alla funzione db get_strutture_ruolo_e_figlie", ex);
                    }
                    break;
                }
            }
        }
        return initialPredicate;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        Struttura struttura = (Struttura) entity;
        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case GetPermessiStrutturePec:
                        List<PermessoEntitaStoredProcedure> struttureConPermesso
                                = (List<PermessoEntitaStoredProcedure>) this.httpSessionData.getData(HttpSessionData.Keys.StruttureWithPecPermissions);
                        if (struttureConPermesso != null && !struttureConPermesso.isEmpty()) {
                            List<PermessoEntitaStoredProcedure> permessiStruttura
                                    = struttureConPermesso.stream().filter(p
                                            -> p.getSoggetto().getIdProvenienza()
                                            .equals(struttura.getId()))
                                            .collect(Collectors.toList());
                            struttura.setPermessi(permessiStruttura);
                        }
                        break;
                }
            }
        }
        return struttura;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            if (operationsRequested.contains(AdditionalData.OperationsRequested.GetPermessiStrutturePec)) {
                if (this.httpSessionData.getData(HttpSessionData.Keys.StruttureWithPecPermissions) != null) {
                    for (Object entity : entities) {
                        entity = afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
                    }
                }
            }
        }
        return entities;
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            if (operationsRequested.contains(AdditionalData.OperationsRequested.SvuotaStruttureConnesseUfficio)) {
                try {
                    Struttura struttura = (Struttura) entity;
                    if (!struttura.getAttributiStruttura().getIdTipologiaStruttura().getAssociaStrutture()) {
                        List<PermessoEntitaStoredProcedure> permessiAttuali = permissionManager.getSubjectsWithPermissionsOnObject(
                                struttura,
                                Arrays.asList(Predicati.CONNESSO.toString()),
                                Arrays.asList(Ambiti.BABORG.toString()),
                                Arrays.asList(Tipi.UFFICIO.toString()),
                                false);
                        if (permessiAttuali != null && !permessiAttuali.isEmpty()) {
                            permessiAttuali.forEach(permessoEntitaStoredProcedure -> {
                                permessoEntitaStoredProcedure.getCategorie().forEach(categoria -> {
                                    categoria.setPermessi(new ArrayList<>());
                                });
                            });
                            permissionRepositoryAccess.managePermissions(permessiAttuali, null);
                        }
                    }
                } catch (BlackBoxPermissionException ex) {
                    LOGGER.error("Errore salvataggio dei permessi", ex);
                    throw new AbortSaveInterceptorException("Errore salvataggio dei permessi", ex);
                }
            }
        }
        Struttura strutturaNuova = (Struttura) entity;
        Struttura strutturaVecchia = (Struttura) beforeUpdateEntity;
        aggiungiSistemaStoricoRelazione(strutturaNuova, strutturaVecchia);
        return entity;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Struttura struttura = (Struttura) entity;
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        boolean isCA = userInfoService.isCA(utente);
        boolean isCI = userInfoService.isCI(utente);
        boolean isSD = userInfoService.isSD(utente);
        
        if (struttura.getUfficio() && struttura.getIdStrutturaPadre() == null) {
            //setto la data di attivazione al primo momento del giorno 
            struttura.setDataAttivazione(ZonedDateTime.of(LocalDate.now(), LocalTime.MIN, ZoneId.systemDefault()));
            // In caso di creazione ufficio vogliamo arbitrariamente popolare la struttura padre
            if (isCA || isCI || isSD) {
                // In questo caso setto la radice dell'organigramma
                //Non vogliamo più settare la radice dell'organigramma, ma vogliamo che sia selezionata dall'utente.
//                BooleanExpression findRadice = 
//                        QStruttura.struttura.idStrutturaPadre.isNull()
//                        .and(QStruttura.struttura.idAzienda.id.eq(struttura.getIdAzienda().getId()))
//                        .and(QStruttura.struttura.attiva.eq(Boolean.TRUE));
//                Struttura strutturaRadice = strutturaRepository.findOne(findRadice).get();
//                struttura.setIdStrutturaPadre(strutturaRadice);
            } else {
                Integer mascheraBit = internautaUtils.getSommaMascheraBit(Ruolo.CodiciRuolo.R.toString());

                Map<String, Integer> struttureResponsabile = objectMapper.convertValue(
                        storicoRelazioneRepository.getStruttureRuolo(mascheraBit, utente.getId(), ZonedDateTime.now()).get("result"),
                        new TypeReference<Map<String, Integer>>() {
                }
                );

                if (struttureResponsabile.size() == 0) {
                    throw new AbortSaveInterceptorException("Utente non autorizzato alla creazione di uffici");
                }

                Iterator<String> struttureRespIterator = struttureResponsabile.keySet().iterator();
                Integer idStrutturaResp = null;
                if (struttureRespIterator.hasNext()) {
                    idStrutturaResp = Integer.parseInt(struttureRespIterator.next());
                }

                BooleanExpression findStrutturaResponsabile
                        = QStruttura.struttura.id.eq(idStrutturaResp);
                Struttura strutturaResponsabile = strutturaRepository.findOne(findStrutturaResponsabile).get();
                struttura.setIdStrutturaPadre(strutturaResponsabile);
            }
        }
        return struttura;
    }

    private void aggiungiSistemaStoricoRelazione(Struttura strutturaNuova, Struttura strutturaVecchia) {
        if (strutturaVecchia.getIdStrutturaPadre() == null && strutturaNuova.getIdStrutturaPadre()!= null) {
            StoricoRelazione storicoRelazione = buildStoricoRelazione(strutturaNuova);
            storicoRelazioneRepository.save(storicoRelazione);
        } else if (strutturaVecchia.getIdStrutturaPadre() != null && !strutturaVecchia.getIdStrutturaPadre().getId().equals(strutturaNuova.getIdStrutturaPadre().getId())) {
            ZonedDateTime now = ZonedDateTime.now();
            StoricoRelazione storicoRelazioneVecchia = storicoRelazioneRepository.findOne(
                    QStoricoRelazione.storicoRelazione.idStrutturaFiglia.id.eq(strutturaVecchia.getId()).and(
                            (QStoricoRelazione.storicoRelazione.attivaDal.after(now).and(
                                    (QStoricoRelazione.storicoRelazione.attivaAl.isNull().or(QStoricoRelazione.storicoRelazione.attivaAl.before(now)))
                            ))
                    )).get();
            if (storicoRelazioneVecchia.getAttivaDal().toLocalDate().equals(now.toLocalDate())) {
                storicoRelazioneRepository.deleteById(storicoRelazioneVecchia.getId());
                StoricoRelazione storicoRelazione = buildStoricoRelazione(strutturaNuova);
                storicoRelazioneRepository.save(storicoRelazione);
            }else{
                storicoRelazioneVecchia.setAttivaAl(now);
                storicoRelazioneRepository.save(storicoRelazioneVecchia);
                storicoRelazioneRepository.save(buildStoricoRelazione(strutturaNuova));
            }
        }
    }

    private StoricoRelazione buildStoricoRelazione(Struttura strutturaNuova) {
        StoricoRelazione storicoRelazione = new StoricoRelazione();
        ZonedDateTime now = ZonedDateTime.of(LocalDate.now(), LocalTime.MIN, ZoneId.systemDefault());
        storicoRelazione.setAttivaDal(now);
        storicoRelazione.setIdStrutturaFiglia(strutturaNuova);
        storicoRelazione.setIdStrutturaPadre(strutturaNuova.getIdStrutturaPadre());
        storicoRelazione.setAttivaAl(null);
        return storicoRelazione;
    }

}
