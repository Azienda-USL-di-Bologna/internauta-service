package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "azienda-interceptor")
public class AziendaInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AziendaInterceptor.class);
    
    @Override
    public Class getTargetEntityClass() {
        return Azienda.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        //getAuthenticatedUserProperties();  
        
        // sarebbe giusto che solo gli utenti superaziendali vedessero tutte le aziende. 
        // questo però cozza con le strutture unificate. L'albero nell'organigramma ha bisogno
        // di quest'informazione -> quindi non posso mettere nessun filtro in base ai ruoli
        //
        // solo i CI possono vedere tutte le aziende o tutti i ruoli superaziendali
//        if (!(isCI(user) || isAS(user) || isSD(user))) {
//            Persona persona = personaRepository.getOne(person.getId());
//            List<Integer> idAziendePersona = userInfoService.getAziendePersona(persona)
//                    .stream().map(azienda -> azienda.getId()).collect(Collectors.toList());
//            
//            BooleanExpression customFilter = QAzienda.azienda.id.in(idAziendePersona);
//            initialPredicate = customFilter.and(initialPredicate);                                   
//        }
        
//        /**
//         * Gestistisco eventuali additional data
//         */
//        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
//        if (operationsRequested != null && !operationsRequested.isEmpty()) {
//            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
//                switch (operationRequested) {
//                    case GetUltimoStatoRibaltone:
//                    List<Integer> ultimi = ribaltoneDaLanciareRepository.getUltimoStato();
//                        BooleanExpression ultimoStato = QAzienda.azienda.ribaltoneDaLanciareList.any().id.in(
//                            ultimi
//                        );
////                        BooleanExpression ultimoStato = QAzienda.azienda.strutturaList.any().id.in(ultimi);
////                        BooleanExpression ultimoStato = QAzienda.azienda.attivitaList.any().id.in(ultimi);
//                        initialPredicate = ultimoStato.and(initialPredicate);
//                    break;
//                }
//            }
//        }
        return initialPredicate;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        Azienda azienda = (Azienda) entity;
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        
        AziendaParametriJson aziendaParametriJson = azienda.getParametri();
        
        if (authenticatedSessionData.isFromInternet()) {
            try {
                
                aziendaParametriJson.setBasePath(aziendaParametriJson.getInternetBasePath());
                aziendaParametriJson.setLogoutUrl(aziendaParametriJson.getInternetLogoutUrl());
                azienda.setParametri(aziendaParametriJson);
            } catch (Exception ex) {
                LOGGER.error("errore nel reperimento di isFromInternet", ex);
            }
        }
        
        // Non voglio che al fontend arrivino i dati della colonna dei parametri se non solo alcuni specifici
        AziendaParametriJson nonHiddenParameters = new AziendaParametriJson();
        nonHiddenParameters.setBasePath(aziendaParametriJson.getBasePath());
        nonHiddenParameters.setLogoutUrl(aziendaParametriJson.getLogoutUrl());
        
        azienda.setParametri(nonHiddenParameters);
        
        return azienda;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        for (Object entity : entities) {
            //Azienda azienda = (Azienda) entity;
            afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
        }
        return entities;
    }
    
    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        throw new AbortSaveInterceptorException("Non si può cancellare una azienda");
    }

    
}
    

