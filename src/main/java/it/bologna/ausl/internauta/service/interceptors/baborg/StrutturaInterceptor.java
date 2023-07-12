package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintBaborgService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StoricoRelazioneRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Ambiti;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Predicati;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Tipi;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.AttributiStruttura;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.QAttributiStruttura;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QTipologiaStruttura;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
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
    private PermissionManager permissionManager;

    @Autowired
    private PermissionRepositoryAccess permissionRepositoryAccess;

    @Autowired
    private StoricoRelazioneRepository storicoRelazioneRepository;

    @Autowired
    private StrutturaRepository strutturaRepository;

    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private InternautaUtils internautaUtils;
    
    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    private KrintBaborgService krintBaborgService;

    @Autowired
    private KrintUtils krintUtils;
    
    @Autowired
    private EntityManager em;

    @Override
    public Class getTargetEntityClass() {
        return Struttura.class;
    }
//
//    @Override
//    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
//        BooleanExpression filtroStandard = QStruttura.struttura.id.eq(1835494);
//        initialPredicate = filtroStandard.and(initialPredicate);
//        return initialPredicate;
//    }
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        boolean isCA = userInfoService.isCA(utente);
        boolean isCI = userInfoService.isCI(utente);
        boolean isSD = userInfoService.isSD(utente);
        if(mainEntity) {

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
                                        Arrays.asList(new String[]{Tipi.PEC.toString()}), false, false);
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
                            String predicatiNomeBreveString = additionalData.get(InternautaConstants.AdditionalData.Keys.predicati.toString());
                            LOGGER.info("Guardo se ci sono dei filtri legati a ruoli e predicati");
                            if (!isCA && !isCI && !isSD) {
                                BooleanExpression filterRuolo = Expressions.FALSE.eq(true);
                                BooleanExpression filterSegreteriaPoolAssegnamento = Expressions.FALSE.eq(true);
                                List<ParametroAziende> filtraResponsabiliParams = parametriAziende.getParameters("AccessoPoolFiltratoPerRuolo", new Integer[]{utente.getIdAzienda().getId()});
                                if (filtraResponsabiliParams != null && !filtraResponsabiliParams.isEmpty() && parametriAziende.getValue(filtraResponsabiliParams.get(0), Boolean.class)) {
                                    if(!StringUtils.isEmpty(ruoliNomeBreveString)) {
                                        Integer mascheraBit = internautaUtils.getSommaMascheraBit(ruoliNomeBreveString);

                                        Map<String, Integer> struttureRuoloEFiglie = objectMapper.convertValue(
                                                storicoRelazioneRepository.getStruttureRuoloEFiglie(mascheraBit, utente.getId(), ZonedDateTime.now()).get("result"),
                                                new TypeReference<Map<String, Integer>>() {
                                        }
                                        );

                                        if (struttureRuoloEFiglie != null && !struttureRuoloEFiglie.isEmpty()) {
                                            List<Integer> struttureResposabilita = struttureRuoloEFiglie.keySet().stream().map(s -> Integer.valueOf(s)).collect(Collectors.toList());
                                            filterRuolo = QStruttura.struttura.idStrutturaPadre.id.in(struttureResposabilita);
        //                                    initialPredicate = filterRuolo.and(initialPredicate);
                                        }
                                    }
                                    if (!StringUtils.isEmpty(predicatiNomeBreveString) && predicatiNomeBreveString.contains("SEGR")) {
                                        List<Integer> struttureSegretario = Arrays.asList(personaRepository.getStruttureDelSegretario(utente.getIdPersona().getId()));
                                        if (struttureSegretario != null && !struttureSegretario.isEmpty()) {
    //                                        BooleanExpression filterSegreteriaPoolAssegnamento = QStruttura.struttura.idStrutturaPadre.id.in(struttureSegretario)
    //                                                .and(QStruttura.struttura.attributiStruttura.)

                                            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
                                            List<Integer> listaPoolAssegnamento = jPAQueryFactory
                                                    .select(QStruttura.struttura.id)
                                                    .from(QStruttura.struttura)
                                                    .join(QAttributiStruttura.attributiStruttura).on(QAttributiStruttura.attributiStruttura.idStruttura.id.eq(QStruttura.struttura.id))
                                                    .join(QTipologiaStruttura.tipologiaStruttura).on(QTipologiaStruttura.tipologiaStruttura.id.eq(QAttributiStruttura.attributiStruttura.idTipologiaStruttura.id))
                                                    .where(QTipologiaStruttura.tipologiaStruttura.tipologia.eq("Assegnamento")
                                                            .and(QStruttura.struttura.idStrutturaPadre.id.in(struttureSegretario)))
                                                    .fetch();
                                            filterSegreteriaPoolAssegnamento = QStruttura.struttura.id.in(listaPoolAssegnamento);
                                            initialPredicate = filterSegreteriaPoolAssegnamento.and(initialPredicate);
                                        } 
                                        initialPredicate = (filterSegreteriaPoolAssegnamento.or(filterRuolo)).and(initialPredicate);

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
                        case FilterPerAssegnamentoMassivo:
                            try {
                            String idStruttura = additionalData.get("idStruttura");
                            Integer idStrutturaInteger = Integer.parseInt(idStruttura);
                            Struttura struttura = strutturaRepository.getById(idStrutturaInteger);
                            String stringaDaCercare = additionalData.get("stringaDaCercare");
                            List<Integer> idStruttureFiglieECugine = strutturaRepository.getStruttureFiglieECugine(struttura.getId());
                            LOGGER.info("Assegnamento massivo, idStruttura : " + struttura.getId().toString() + ", stringa da cercare: " + stringaDaCercare);
                            List<Integer> poolDiTipoAssegnamento = strutturaRepository.getPoolAssegnamento();
    //                            Qui filtro per i pool che sono figli della struttura
                            BooleanExpression poolFilter = Expressions.TRUE.eq(true)
                                    .and(QStruttura.struttura.ufficio.eq(Boolean.TRUE)
                                            .and(QStruttura.struttura.attiva.eq(Boolean.TRUE)
                                                    .and(QStruttura.struttura.id.in(poolDiTipoAssegnamento)
                                                            .and(QStruttura.struttura.idStrutturaPadre.id.eq(struttura.getId())
                                                                    .and(QStruttura.struttura.nome.containsIgnoreCase(stringaDaCercare))))));

    //                            Qui filtro per le strutture figlie e cugine che non sono uffici e sono attive
                            BooleanExpression struttureFiglieFilter = Expressions.TRUE.eq(false);
                            if (!idStruttureFiglieECugine.isEmpty() && idStruttureFiglieECugine != null) {
                                struttureFiglieFilter = Expressions.TRUE.eq(true)
                                        .and(QStruttura.struttura.id.in(idStruttureFiglieECugine)
                                                .and(QStruttura.struttura.nome.containsIgnoreCase(stringaDaCercare)
                                                        .and(QStruttura.struttura.ufficio.eq(Boolean.FALSE)
                                                                .and(QStruttura.struttura.attiva.eq(Boolean.TRUE)))));

                            }

    //                            Mi recupero i pool connessi
                            List<Integer> idPoolConnessi = new ArrayList<Integer>();
                            List<PermessoEntitaStoredProcedure> permessiAttuali = permissionManager.getSubjectsWithPermissionsOnObject(
                                    struttura,
                                    Arrays.asList(Predicati.CONNESSO.toString()),
                                    Arrays.asList(Ambiti.BABORG.toString()),
                                    Arrays.asList(Tipi.UFFICIO.toString()),
                                    false);
                            if (permessiAttuali != null && !permessiAttuali.isEmpty()) {
                                permessiAttuali.forEach(permessoEntitaStoredProcedure -> {
                                    idPoolConnessi.add(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza());
                                });
                            }
    //                            Qui filtro per i pool connessi di tipo assegnamento
                            BooleanExpression filterConnessi = Expressions.TRUE.eq(false);
                            if (!idPoolConnessi.isEmpty()) {
                                filterConnessi = Expressions.TRUE.eq(true)
                                        .and(QStruttura.struttura.ufficio.eq(Boolean.TRUE)
                                                .and(QStruttura.struttura.attiva.eq(Boolean.TRUE)
                                                        .and(QStruttura.struttura.id.in(poolDiTipoAssegnamento)
                                                                .and(QStruttura.struttura.id.in(idPoolConnessi)
                                                                        .and(QStruttura.struttura.nome.containsIgnoreCase(stringaDaCercare))))));

                            }

                            BooleanExpression filtroStandard = poolFilter
                                    .or(struttureFiglieFilter) // Filtro 4
                                    .or(filterConnessi); // Filtro 5
                            initialPredicate = filtroStandard.and(initialPredicate);

                        } catch (Exception ex) {
                            throw new AbortLoadInterceptorException("errore nella chiamata alla funzione db get_strutture_figlie_e_cugine", ex);
                        }
                        break;
                    }
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
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);

        Struttura struttura = (Struttura) entity;
        Struttura strutturaOld;
        try {
            strutturaOld = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, Struttura.class);

        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }
        boolean isEliminata = (!struttura.getAttiva() && (strutturaOld.getAttiva()));
        boolean isChangedStrutturaPadre = struttura.getIdStrutturaPadre() == null ? strutturaOld.getIdStrutturaPadre() != null : !struttura.getIdStrutturaPadre().equals(strutturaOld.getIdStrutturaPadre());
        boolean isChangedNome = struttura.getNome() == null ? strutturaOld.getNome() != null : !struttura.getNome().equals(strutturaOld.getNome());
        boolean isChangedAttributiStruttura = struttura.getAttributiStruttura() == null ? strutturaOld.getAttributiStruttura() != null : !struttura.getAttributiStruttura().equals(strutturaOld.getAttributiStruttura());
        if (krintUtils.doIHaveToKrint(request)) {
            if (struttura.getUfficio()) {
                if (isChangedNome) {
                    String nomeOld = strutturaOld.getNome();
                    krintBaborgService.writeUfficioUpdate(struttura, OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_NOME_UPDATE, nomeOld);
                }
                if (isChangedStrutturaPadre && !isChangedNome) {
                    Struttura strutturaPadre = struttura.getIdStrutturaPadre();
                    krintBaborgService.writeUfficioUpdate(struttura, OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_STRUTTURA_PADRE_UPDATE, strutturaPadre);
                }
                if (isChangedAttributiStruttura) {
                    AttributiStruttura attributiStruttura = struttura.getAttributiStruttura();
                    krintBaborgService.writeUfficioUpdate(struttura, OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_ATTRIBUTI_STRUTTURA_UPDATE, attributiStruttura);
                }
                if (isEliminata) {
                    krintBaborgService.writeUfficioDelete(struttura, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DELETE);
                }
            }
        }

        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            if (operationsRequested.contains(AdditionalData.OperationsRequested.SvuotaStruttureConnesseUfficio)) {
                //entro solo nel caso del pool quando cerco di eliminarne uno
                try {
                    Struttura pool = (Struttura) entity;
                    if (pool != null && pool.getAttributiStruttura() != null && pool.getAttributiStruttura().getIdTipologiaStruttura() != null && pool.getAttributiStruttura().getIdTipologiaStruttura().getAssociaStrutture() != null && !pool.getAttributiStruttura().getIdTipologiaStruttura().getAssociaStrutture()) {
                        List<PermessoEntitaStoredProcedure> permessiAttuali = permissionManager.getSubjectsWithPermissionsOnObject(
                                pool,
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
        ArrayList<Struttura> listaFarlocca = new ArrayList();
        try {
//                    Struttura strutturaVecchia = (Struttura) beforeUpdateEntity;
            beforeUpdateEntityApplier.beforeUpdateApply(oldEntity -> {
                Struttura strutturaVecchia = (Struttura) oldEntity;
                listaFarlocca.add(strutturaVecchia.getIdStrutturaPadre());
//                aggiungiSistemaStoricoRelazione(strutturaNuova, strutturaVecchia);
            });
        } catch (Exception ex) {
            throw new AbortSaveInterceptorException("errore nel reperire la vecchia struttura", ex);
        }
        Struttura strutturaPadreVecchia = listaFarlocca.get(0);
        aggiungiSistemaStoricoRelazione(strutturaNuova, strutturaPadreVecchia);

        spegniUtentiStrutturaEspegniPermessiStruttureConnesseAPool(strutturaNuova);
        return entity;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Struttura struttura = (Struttura) entity;
        if (krintUtils.doIHaveToKrint(request)) {
            if (struttura.getUfficio() && struttura.getIdStrutturaPadre() == null) {
                krintBaborgService.writeUfficioCreation(struttura, OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_CREATION);
            }

        }
        return entity; //To change body of generated methods, choose Tools | Templates.
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
//            if (isCA || isCI || isSD) {
            // In questo caso setto la radice dell'organigramma
            //Non vogliamo più settare la radice dell'organigramma, ma vogliamo che sia selezionata dall'utente.
//                BooleanExpression findRadice =
//                        QStruttura.struttura.idStrutturaPadre.isNull()
//                        .and(QStruttura.struttura.idAzienda.id.eq(struttura.getIdAzienda().getId()))
//                        .and(QStruttura.struttura.attiva.eq(Boolean.TRUE));
//                Struttura strutturaRadice = strutturaRepository.findOne(findRadice).get();
//                struttura.setIdStrutturaPadre(strutturaRadice);
//            } else {
            if (!isCA && !isCI && !isSD) {
                Integer mascheraBit = internautaUtils.getSommaMascheraBit(Ruolo.CodiciRuolo.R.toString());

                Map<String, Integer> struttureResponsabile = objectMapper.convertValue(
                        storicoRelazioneRepository.getStruttureRuolo(mascheraBit, utente.getId(), ZonedDateTime.now()).get("result"),
                        new TypeReference<Map<String, Integer>>() {
                }
                );
                List<Integer> idStruttureSegretario = Arrays.asList(personaRepository.getStruttureDelSegretario(utente.getIdPersona().getId()));


                if (struttureResponsabile != null && struttureResponsabile.isEmpty() && idStruttureSegretario != null && idStruttureSegretario.isEmpty()) {
                    throw new AbortSaveInterceptorException("Utente non autorizzato alla creazione di uffici");
                }
                
                
                
                if(struttureResponsabile != null && struttureResponsabile.isEmpty()) {
                    Iterator<String> struttureRespIterator = struttureResponsabile.keySet().iterator();
                    Integer idStrutturaResp = null;
                    if (struttureRespIterator.hasNext()) {
                        idStrutturaResp = Integer.parseInt(struttureRespIterator.next());
                    }

                    BooleanExpression findStrutturaResponsabile
                            = QStruttura.struttura.id.eq(idStrutturaResp);

                    Struttura strutturaResponsabile = strutturaRepository.findOne(findStrutturaResponsabile).get();
                    struttura.setIdStrutturaPadre(strutturaResponsabile);
                } else {
//                    BooleanExpression findStrutturaSegretario
//                        = QStruttura.struttura.id.in(idStruttureSegretario).and(QStruttura.struttura.ufficio.eq(Boolean.FALSE));
//                    List<Struttura> struttureSegretario = (List<Struttura>) strutturaRepository.findAll(findStrutturaSegretario);
//                    struttura.setIdStrutturaPadre(struttureSegretario.get(0));
                }

                
            }
        }
        return struttura;
    }

    private void aggiungiSistemaStoricoRelazione(Struttura strutturaNuova, Struttura strutturaPadreVecchia) throws AbortSaveInterceptorException {
        if (!strutturaNuova.getAttiva()) {
            ZonedDateTime now = ZonedDateTime.now();
            if (strutturaNuova.getIdStrutturaPadre() != null) {
                try {

                    Optional<StoricoRelazione> storicoRelazioneVecchia = storicoRelazioneRepository.findOne(
                            QStoricoRelazione.storicoRelazione.idStrutturaFiglia.id.eq(strutturaNuova.getId()).and(QStoricoRelazione.storicoRelazione.attivaAl.isNull())
                    );
                    if (storicoRelazioneVecchia.isPresent()) {
                        if (storicoRelazioneVecchia.get().getAttivaDal().toLocalDate().equals(now.toLocalDate())) {
                            storicoRelazioneRepository.deleteById(storicoRelazioneVecchia.get().getId());
                        } else {
                            now = now.truncatedTo(ChronoUnit.DAYS).minusSeconds(1);
                            storicoRelazioneVecchia.get().setAttivaAl(now);
                            storicoRelazioneRepository.save(storicoRelazioneVecchia.get());
                        }
                    }
                } catch (Exception ex) {
                    throw new AbortSaveInterceptorException("Relazioni da spegnere non trovate", ex);
                }
            }
        } else {
            if (strutturaPadreVecchia == null && strutturaNuova.getIdStrutturaPadre() != null) {
                StoricoRelazione storicoRelazione = buildStoricoRelazione(strutturaNuova);
                storicoRelazioneRepository.save(storicoRelazione);
            } else if (strutturaPadreVecchia != null && strutturaNuova.getIdStrutturaPadre() != null && !strutturaPadreVecchia.getId().equals(strutturaNuova.getIdStrutturaPadre().getId())) {
                ZonedDateTime now = ZonedDateTime.now();
                Optional<StoricoRelazione> storicoRelazioneVecchia = storicoRelazioneRepository.findOne(
                        QStoricoRelazione.storicoRelazione.idStrutturaFiglia.id.eq(strutturaNuova.getId()).and(
                                (QStoricoRelazione.storicoRelazione.attivaDal.before(now).and(
                                        QStoricoRelazione.storicoRelazione.attivaAl.isNull()
                                ))
                        ));
                if (storicoRelazioneVecchia.isPresent()) {
                    if (storicoRelazioneVecchia.get().getAttivaDal().toLocalDate().equals(now.toLocalDate())) {
                        storicoRelazioneRepository.deleteById(storicoRelazioneVecchia.get().getId());
                        StoricoRelazione storicoRelazione = buildStoricoRelazione(strutturaNuova);
                        storicoRelazioneRepository.save(storicoRelazione);
                    } else {
                        now = now.truncatedTo(ChronoUnit.DAYS).minusSeconds(1);
                        storicoRelazioneVecchia.get().setAttivaAl(now);
                        storicoRelazioneRepository.save(storicoRelazioneVecchia.get());
                        storicoRelazioneRepository.save(buildStoricoRelazione(strutturaNuova));
                    }
                }
            } else if (strutturaPadreVecchia != null && strutturaNuova.getIdStrutturaPadre() == null) {
                ZonedDateTime now = ZonedDateTime.now();
                Optional<StoricoRelazione> storicoRelazioneVecchia = storicoRelazioneRepository.findOne(
                        QStoricoRelazione.storicoRelazione.idStrutturaFiglia.id.eq(strutturaNuova.getId()).and(
                                (QStoricoRelazione.storicoRelazione.attivaDal.before(now).and(
                                        QStoricoRelazione.storicoRelazione.attivaAl.isNull()
                                ))
                        ));
                if (storicoRelazioneVecchia.isPresent()) {
                    if (storicoRelazioneVecchia.get().getAttivaDal().toLocalDate().equals(now.toLocalDate())) {
                        storicoRelazioneRepository.deleteById(storicoRelazioneVecchia.get().getId());
                    } else {
                        now = now.truncatedTo(ChronoUnit.DAYS).minusSeconds(1);
                        storicoRelazioneVecchia.get().setAttivaAl(now);
                        storicoRelazioneRepository.save(storicoRelazioneVecchia.get());
                    }
                }
            }
        }
    }

    private StoricoRelazione buildStoricoRelazione(Struttura strutturaNuova) {
        StoricoRelazione storicoRelazione = new StoricoRelazione();
        ZonedDateTime now = ZonedDateTime.of(LocalDate.now(), LocalTime.MIN, ZoneId.systemDefault());
        storicoRelazione.setAttivaDal(now);
//        Struttura strutturaNuovaReloaded = strutturaRepository.getById(strutturaNuova.getIdStrutturaPadre().getId());
//        Struttura strutturaNuovaPadre = strutturaRepository.getById(strutturaNuova.getIdStrutturaPadre().getId());
        Struttura strutturaNuovaPadre = strutturaNuova.getIdStrutturaPadre();
        storicoRelazione.setIdStrutturaFiglia(strutturaNuova);
        storicoRelazione.setIdStrutturaPadre(strutturaNuovaPadre);
        storicoRelazione.setAttivaAl(null);
        return storicoRelazione;
    }

    private void spegniUtentiStrutturaEspegniPermessiStruttureConnesseAPool(Struttura poolNuovo) throws AbortSaveInterceptorException {
        //si tratta di un pool, perchè è l'unica struttura che si può spegnere
        if (!poolNuovo.getAttiva()) {
            ZonedDateTime dataSpegnimento = ZonedDateTime.now();
            poolNuovo.setDataCessazione(dataSpegnimento);

            //spengo gli utenti
            List<UtenteStruttura> utentiPool = poolNuovo.getUtenteStrutturaList();
            for (int i = 0; i < utentiPool.size(); i++) {
                utentiPool.get(i).setAttivo(false);
                utentiPool.get(i).setAttivoAl(dataSpegnimento);
                utenteStrutturaRepository.save(utentiPool.get(i));
            }

            //spengo i permessi
            try {
                List<PermessoEntitaStoredProcedure> oggettoneListStruttureConnesse = permissionManager.getSubjectsWithPermissionsOnObject(
                        poolNuovo,
                        Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.CONNESSO.toString()}),
                        Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.BABORG.toString()}),
                        Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.UFFICIO.toString()}),
                        false);

                if (oggettoneListStruttureConnesse != null) {
                    oggettoneListStruttureConnesse.forEach(permessoEntitaStoredProcedure -> {
                        permessoEntitaStoredProcedure.getCategorie().forEach(categoria -> {
                            categoria.setPermessi(new ArrayList<>());
                        });
                    });
                    permissionManager.managePermissions(oggettoneListStruttureConnesse, null);
                }
            } catch (BlackBoxPermissionException ex) {
                LOGGER.error("Errore nel caricamento dei permessi  dalla BlackBox", ex);
                throw new AbortSaveInterceptorException("Errore nel caricamento dei permessi dalla BlackBox", ex);
            }
        }
    }
}
