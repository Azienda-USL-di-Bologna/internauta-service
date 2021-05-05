package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.ribaltoneutils.RibaltoneDaLanciareRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "azienda-interceptor")
public class AziendaInterceptor extends InternautaBaseInterceptor{
    private static final Logger LOGGER = LoggerFactory.getLogger(AziendaInterceptor.class);
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    RibaltoneDaLanciareRepository ribaltoneDaLanciareRepository;
    
    @Autowired
    ObjectMapper objectMapper;
    
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
//        LOGGER.info("authenticatedSessionData.isFromInternet(): " + authenticatedSessionData.isFromInternet());
        if (authenticatedSessionData.isFromInternet()) {
            try {
                AziendaParametriJson aziendaParametriJson = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
                aziendaParametriJson.setBasePath(aziendaParametriJson.getInternetBasePath());
                aziendaParametriJson.setLogoutUrl(aziendaParametriJson.getInternetLogoutUrl());
                azienda.setParametri(AziendaParametriJson.dumpToString(objectMapper, aziendaParametriJson));
            } catch (Exception ex) {
                LOGGER.error("errore nel reperimento di isFromInternet", ex);
            }
        }
//        try {
//            LOGGER.info(objectMapper.writeValueAsString(azienda));
//        } catch (JsonProcessingException ex) {
//            LOGGER.error("errore nella stampa dell'azienda", ex);
//        }
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
    
    
}
    

