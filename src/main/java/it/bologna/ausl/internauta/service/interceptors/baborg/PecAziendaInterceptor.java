package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
@NextSdrInterceptor(name = "pecazienda-interceptor")
public class PecAziendaInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PecAziendaInterceptor.class);
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Override
    public Class getTargetEntityClass() {
        return PecAzienda.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        // TODO: Se non sono ne CA ne CI posso vedere le associazioni pec-aziende?
        return super.beforeSelectQueryInterceptor(initialPredicate, additionalData, request, mainEntity, projectionClass);
    }
    
    /*
     * Condizioni per l'INSERT.
     * Il CI può inserire qualsiasi associazioni.
     * Il CA può inserire solo associazioni con la/e sua/e azienda/e.
     */
    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        LOGGER.info("in: beforeCreateEntityInterceptor di PecAzienda");
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        
        if (!isCI(authenticatedSessionData.getUser())) {
            Persona persona = personaRepository.getOne(authenticatedSessionData.getPerson().getId());
            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(persona).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());
            
            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                // Non sono ne CA ne CI fermo tutto.
                throw new AbortSaveInterceptorException();
            } else {
                PecAzienda pa = (PecAzienda) entity;

                if (!idAziendeCA.contains(pa.getIdAzienda().getId())) {
                    // Pur essendo CA non lo sono di questa azienda.
                    throw new AbortSaveInterceptorException();
                }
            }
        }
        
        return entity;
    }
    
    /*
     * Condizioni per l'UPDATE.
     * L'UPDATE è permesso solo se è un finto UDPDATE. 
     * L'azienda e la PEC dell'entity devono essere le stesse del beforeUpdateEntity.
     */
    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        PecAzienda dopo = (PecAzienda) entity;
        PecAzienda prima = (PecAzienda) beforeUpdateEntity;
        
        if (!(dopo.getIdAzienda().getId().equals(prima.getIdAzienda().getId())) || !(dopo.getIdPec().getId().equals(prima.getIdPec().getId()))) {
            throw new AbortSaveInterceptorException();
        }
        
        return entity;
    }
    
    /*
     * Condizioni per la DELETE.
     * Il CI può cancellare qualsiasi associazione.
     * Il CA può cancellare solo associazioni con la/e sua/e azienda/e.
     */
    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();

        if (!isCI(authenticatedSessionData.getUser())) {
            Persona persona = personaRepository.getOne(authenticatedSessionData.getPerson().getId());
            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(persona).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());
            
            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                // Non sono ne CA ne CI fermo tutto.
                throw new AbortSaveInterceptorException();
            } else {
                PecAzienda pa = (PecAzienda) entity;
                
                if (!idAziendeCA.contains(pa.getIdAzienda().getId())) {
                    // Pur essendo CA non lo sono di questa azienda.
                    throw new AbortSaveInterceptorException();
                }
            }
        }
    }    
}
