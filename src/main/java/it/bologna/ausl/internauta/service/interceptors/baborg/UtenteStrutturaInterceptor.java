package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintBaborgService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
@NextSdrInterceptor(name = "utentestruttura-interceptor")
public class UtenteStrutturaInterceptor extends InternautaBaseInterceptor {

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProjectionFactory projectionFactory;
    
    @Autowired
    private KrintBaborgService krintBaborgService;
    
    @Autowired
    private KrintUtils krintUtils;

    private static final String FILTER_COMBO = "filterCombo";

    @Override
    public Class getTargetEntityClass() {
        return UtenteStruttura.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) {
        System.out.println("in: beforeSelectQueryInterceptor di UtenteStruttura");

        String filterComboValue = null;
        ZonedDateTime dataRiferimento = null;
        List<Integer> doNotInclude = new ArrayList<Integer>();
        if (additionalData != null && additionalData.containsKey(FILTER_COMBO)) {
            filterComboValue = additionalData.get(FILTER_COMBO);
        }
        String key = InternautaConstants.AdditionalData.Keys.dataRiferimento.toString();
        if (additionalData != null && additionalData.containsKey(key)) {
            dataRiferimento = Instant.ofEpochMilli(Long.parseLong(additionalData.get(key))).atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS);
        }
        String keyToDoNotInclude = InternautaConstants.AdditionalData.Keys.doNotInclude.toString();
        if (additionalData != null && additionalData.containsKey(keyToDoNotInclude)) {
//            String [] doNotIncludeString = additionalData.get(keyToDoNotInclude).toString().split(";");
            for (String field : additionalData.get(keyToDoNotInclude).toString().split(";")){
                doNotInclude.add(Integer.parseInt(field));
            }
        }

        if (filterComboValue != null) {
            BooleanExpression customFilter = QUtenteStruttura.utenteStruttura.idUtente.idPersona.cognome
                    .concat(" ")
                    .concat(QUtenteStruttura.utenteStruttura.idUtente.idPersona.nome)
                    .containsIgnoreCase(filterComboValue);
            initialPredicate = customFilter.and(initialPredicate);
        }
        if (doNotInclude.size() > 0) {
            BooleanExpression customFilter = QUtenteStruttura.utenteStruttura.idUtente.idPersona.id.notIn(doNotInclude);
            initialPredicate = customFilter.and(initialPredicate);
        }
        if (dataRiferimento != null && !dataRiferimento.toLocalDate().isEqual(LocalDate.now())) {
            QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
            BooleanExpression filter = qUtenteStruttura.attivoDal.loe(dataRiferimento)
                    .and((qUtenteStruttura.attivoAl.isNull()).or(qUtenteStruttura.attivoAl.goe(dataRiferimento)));
            filter = filter.and(qUtenteStruttura.idStruttura.dataAttivazione.loe(dataRiferimento)
                    .and((qUtenteStruttura.idStruttura.dataCessazione.isNull()).or(qUtenteStruttura.idStruttura.dataCessazione.goe(dataRiferimento))));
            initialPredicate = filter.and(initialPredicate);
        } else {
            /* vogliamo che per default, se non si passa una data di riferimento, oppure si passa come data di riferimento la data odierna,
            * si cerchino le righe con campo attivo = true
            * NB: il front-end a volte lo mette già nei filtri dell'initialPredicate
             */
            BooleanExpression customFilterUtenteStrutturaAttivo = QUtenteStruttura.utenteStruttura.attivo.eq(true);
            customFilterUtenteStrutturaAttivo = customFilterUtenteStrutturaAttivo.and(QUtenteStruttura.utenteStruttura.idStruttura.attiva.eq(true));
            initialPredicate = customFilterUtenteStrutturaAttivo.and(initialPredicate);
        }

        return initialPredicate;
    }

    private Object getUtenteStruttura(Map<String, Object> map, Class projection) {
        UtenteStruttura utenteStruttura = utenteStrutturaRepository.getOne((Integer) map.get("id"));
        Object res = projectionFactory.createProjection(projection, utenteStruttura);
        return res;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request,
            boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        // if richiede di caricare sotto resp
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested
                = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operation : operationsRequested) {
                switch (operation) {
                    case CaricaSottoResponsabili:
                        //idProvenienzaOggetto=28618
                        String idStrutturaString = additionalData.get(InternautaConstants.AdditionalData.Keys.idProvenienzaOggetto.toString());
                        LocalDateTime dataRiferimento = null;
                        String key = InternautaConstants.AdditionalData.Keys.dataRiferimento.toString();
                        if (additionalData.containsKey(key)) {
                            dataRiferimento = Instant.ofEpochMilli(Long.parseLong(additionalData.get(key))).atZone(ZoneId.systemDefault()).toLocalDateTime();
                        }
                        if (StringUtils.hasText(idStrutturaString)) {
                            Integer idStruttura = Integer.parseInt(idStrutturaString);
                            List<Map<String, Object>> utentiStrutturaSottoResponsabili;
                            try {
                                utentiStrutturaSottoResponsabili = strutturaRepository.getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(idStruttura, dataRiferimento);
                            } catch (Exception ex) {
                                throw new AbortLoadInterceptorException("errore nell'estrazione dei sotto resposabili", ex);
                            }
//                            System.out.println("aaaaaaaaaa");
                            try {
                                System.out.println(objectMapper.writeValueAsString(utentiStrutturaSottoResponsabili));
                            } catch (JsonProcessingException ex) {
                                Logger.getLogger(UtenteStrutturaInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            List<Object> res = utentiStrutturaSottoResponsabili.stream().map(utenteStrutturaMap -> {
                                Object utenteStruttura = this.getUtenteStruttura(utenteStrutturaMap, projectionClass);
                                //return factory.createProjection(UtenteStrutturaWithIdUtente.class, utenteStruttura);
                                return utenteStruttura;
                            }).collect(Collectors.toList());
                            System.out.println("res");
                            try {
                                System.out.println(objectMapper.writeValueAsString(res));
                            } catch (JsonProcessingException ex) {
                                Logger.getLogger(UtenteStrutturaInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            entities.addAll(res);
//                            List entitiesList = new ArrayList(entities);
//                            Collections.sort(entitiesList, (UtenteStrutturaWithIdUtente us1, UtenteStrutturaWithIdUtente us2) -> {
//                                return ((Utente)(us1.getIdUtente())).getIdPersona().getDescrizione().compareToIgnoreCase(((Utente)us2.getIdUtente()).getIdPersona().getDescrizione());
//                            });
//                            entities = entitiesList;
                        }
                        break;

                }
            }
        }
        return entities;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return entity;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        UtenteStruttura utenteStruttura = (UtenteStruttura) entity;
        
        if (krintUtils.doIHaveToKrint(request)) {
            if (utenteStruttura.getIdStruttura().getUfficio()) {
                krintBaborgService.writeUfficioUpdate(utenteStruttura.getIdStruttura(), OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_UTENTE_STRUTTURA_LIST_ADD, utenteStruttura);
            }
        }
        
        return entity; //To change body of generated methods, choose Tools | Templates.
    }
    
    

//    @Override
//    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
//
//        UtenteStruttura utenteStruttura = (UtenteStruttura) entity;
//        utenteStruttura.setAttivoDal(ZonedDateTime.now())
//
//        return utenteStruttura;
//    }
    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        UtenteStruttura utenteStrutturaNuovo = (UtenteStruttura) entity;
        List<UtenteStruttura> utenteStrutturaVecchioList = new ArrayList<UtenteStruttura>();
        try {
            beforeUpdateEntityApplier.beforeUpdateApply(oldEntity -> {
                utenteStrutturaVecchioList.add((UtenteStruttura) oldEntity);
            });
        } catch (Exception ex) {
            throw new AbortSaveInterceptorException("errore nel reperire la vecchia struttura", ex);
        }
        if (utenteStrutturaVecchioList.get(0) != null && utenteStrutturaVecchioList.get(0).getAttivo() && !utenteStrutturaNuovo.getAttivo()) {
            if (krintUtils.doIHaveToKrint(request)) {
               if (utenteStrutturaNuovo.getIdStruttura().getUfficio()) {
                   krintBaborgService.writeUfficioUpdate(utenteStrutturaNuovo.getIdStruttura(), OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_UTENTE_STRUTTURA_LIST_REMOVE, utenteStrutturaNuovo);
               }
            }
            utenteStrutturaNuovo.setAttivoAl(ZonedDateTime.now());
        }
        return utenteStrutturaNuovo;
    }
}
