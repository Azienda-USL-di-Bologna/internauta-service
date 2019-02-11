/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.nextsw.common.annotations.NextSdrInterceptor;
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
 * @author Utente
 */
@Component
@NextSdrInterceptor(name = "pec-interceptor")
public class AziendaInterceptor extends InternautaBaseInterceptor{
    private static final Logger LOGGER = LoggerFactory.getLogger(AziendaInterceptor.class);
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Override
    public Class getTargetEntityClass() {
        return Azienda.class;
    }
    
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
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
        return initialPredicate;
    }
            
}
    

