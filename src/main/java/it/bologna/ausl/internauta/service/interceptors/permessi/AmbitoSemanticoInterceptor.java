package it.bologna.ausl.internauta.service.interceptors.permessi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.permessi.AmbitoSemantico;
import it.bologna.ausl.model.entities.permessi.QAmbitoSemantico;
import it.bologna.ausl.model.entities.permessi.QPredicatoAmbito;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
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
 * @author mdor
 */
@Component
@NextSdrInterceptor(name = "ambitosemantico-interceptor")
public class AmbitoSemanticoInterceptor extends InternautaBaseInterceptor{
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbitoSemanticoInterceptor.class);
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
  
    @Autowired
    ObjectMapper objectMapper;
    
    @Override
    public Class getTargetEntityClass() {
        return AmbitoSemantico.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();  
        Utente utente = authenticatedUserProperties.getUser();
        boolean isCA = userInfoService.isCA(utente);
        boolean isCI = userInfoService.isCI(utente);
        
        List<String> ruoli = utente.getMappaRuoli().get(Ruolo.ModuliRuolo.MATRINT.toString()).stream().map(ruolo -> ruolo.getNomeBreve().toString()).collect(Collectors.toList());

        if (!isCA && !isCI) {
            BooleanTemplate booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", QAmbitoSemantico.ambitoSemantico.ruoliGestori, String.join(",", ruoli));
            initialPredicate=booleanTemplate.and(initialPredicate);
                                  
        }
        return initialPredicate;
    }
}