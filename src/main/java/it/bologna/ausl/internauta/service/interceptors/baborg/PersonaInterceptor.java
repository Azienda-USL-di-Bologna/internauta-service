package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import edu.emory.mathcs.backport.java.util.Arrays;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.*;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Ambiti;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Predicati;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Tipi;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it> with GDM and Gus collaboration
 */
@Component
@NextSdrInterceptor(name = "persona-interceptor")
@Order(1)
public class PersonaInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonaInterceptor.class);
    
    @Autowired
    PermissionManager permissionManager;
    
    @Override
    public Class getTargetEntityClass() {
        return Persona.class;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        return super.beforeCreateEntityInterceptor(entity, additionalData, request); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        return super.beforeUpdateEntityInterceptor(entity, beforeUpdateEntity, additionalData, request); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        super.beforeDeleteEntityInterceptor(entity, additionalData, request); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
                
        AdditionalData.OperationsRequested operationRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationRequested != null) {        
            switch (operationRequested) {
                case GetPermessiGestoriPec: 
                    /* Nel caso di GetPermessiGestoriPec in Data avremo l'id della PEC della quale si chiedono i permessi */
                    String idPec = additionalData.get(AdditionalData.Keys.Data.toString());
                    Pec pec = new Pec(Integer.parseInt(idPec));
                    try {
                        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(
                            Arrays.asList(new Pec[]{pec}),
                            Arrays.asList(new String[]{Predicati.ELIMINA.toString(), Predicati.LEGGE.toString(), Predicati.RISPONDE.toString()}),
                            Arrays.asList(new String[]{Ambiti.PECG.toString()}),
                            Arrays.asList(new String[]{Tipi.PEC.toString()}), false);
                        if (subjectsWithPermissionsOnObject == null){
                            initialPredicate = Expressions.FALSE.eq(true);
                        }
                        else {
                            BooleanExpression permessoFilter = QPersona.persona.id.in(
                                subjectsWithPermissionsOnObject
                                    .stream()
                                    .map(p -> p.getSoggetto().getIdProvenienza()).collect(Collectors.toList()));
                            initialPredicate = permessoFilter.and(initialPredicate);
                        }
                        /* Conserviamo i dati estratti dalla BlackBox */
                        this.httpSessionData.putData(HttpSessionData.Keys.PersoneWithPecPermissions, subjectsWithPermissionsOnObject);
                    } catch (BlackBoxPermissionException ex) {
                        LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                        throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                    }
                    break;
            }
        }
        return initialPredicate;
    }
   
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
//        permissionManager.getPermission(entity, additionalData, ambiti, tipi);
        Persona persona = (Persona) entity;        
        AdditionalData.OperationsRequested operationRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationRequested != null) {        
            switch (operationRequested) {
                case GetPermessiGestoriPec:
                    List<PermessoEntitaStoredProcedure> personeConPermesso = 
                            (List<PermessoEntitaStoredProcedure>) this.httpSessionData.getData(HttpSessionData.Keys.PersoneWithPecPermissions);
                    if (personeConPermesso != null && !personeConPermesso.isEmpty()) {
                        List<PermessoEntitaStoredProcedure> permessiPersona = 
                                personeConPermesso.stream().filter(p -> 
                                        p.getSoggetto().getIdProvenienza()
                                        .equals(persona.getId()))
                                        .collect(Collectors.toList());
                        persona.setPermessi(permessiPersona);
                    }
                    break;
            }
        }          
        return persona;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        
        AdditionalData.OperationsRequested operationRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationRequested != null) {        
            switch (operationRequested) {
                case GetPermessiGestoriPec: 
                    if (this.httpSessionData.getData(HttpSessionData.Keys.PersoneWithPecPermissions) != null) {
                        for (Object entity : entities) {
                            entity = afterSelectQueryInterceptor(entity, additionalData, request);
                        }
                    }
                    break;
            }
        }
        return entities;
    }
}
