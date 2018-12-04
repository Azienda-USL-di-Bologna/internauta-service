package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 *
 * @author g.russo@nsi.it with collaboration of GDM and Gus
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
        
        this.httpSessionData.putData("testPut", "ciao a tutti");
        
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
                            Arrays.asList(new String[]{Predicati.ELIMINA.toString(), Predicati.LEGGE.toString(), Predicati.SCRIVE.toString()}),
                            Arrays.asList(new String[]{Ambiti.PECG.toString()}),
                            Arrays.asList(new String[]{Tipi.PEC.toString()}));
                        
                        BooleanExpression permessoFilter = QPersona.persona.id.in(
                            subjectsWithPermissionsOnObject
                                .stream()
                                .map(p -> Integer.parseInt(p.getSoggetto().getIdProvenienza())).collect(Collectors.toList()));
                        initialPredicate = permessoFilter.and(initialPredicate);
                        
                        /* Conserviamo i dati estratti dalla BlackBox */
                        this.httpSessionData.putData("personeWithPecPermissions", subjectsWithPermissionsOnObject);
                        
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
        AdditionalData.OperationsRequested operationRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationRequested != null) {        
            switch (operationRequested) {
                case GetPermessiGestoriPec:
                    List<PermessoEntitaStoredProcedure> personeConPermesso =
                            Arrays.asList((PermessoEntitaStoredProcedure[])this.httpSessionData.getData("personeWithPecPermissions"));
                    if (personeConPermesso != null && !personeConPermesso.isEmpty()) {
                        Persona persona = (Persona) entity;
                        List<PermessoEntitaStoredProcedure> permessiPersona = new ArrayList();
                        for (PermessoEntitaStoredProcedure p : personeConPermesso) {
                            if (Integer.parseInt(p.getSoggetto().getIdProvenienza()) == persona.getId()) 
                                permessiPersona.add(p);
                        }
                        persona.setPermessi(permessiPersona);
                    }
                    break;
            }
        }          
        return super.afterSelectQueryInterceptor(entity, additionalData, request); //To change body of generated methods, choose Tools | Templates.
 //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        
        AdditionalData.OperationsRequested operationRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationRequested != null) {        
            switch (operationRequested) {
                case GetPermessiGestoriPec: 
                    if (this.httpSessionData.getData("personeWithPecPermissions") != null) {
                        for (Object entity : entities) {
                            entity = afterSelectQueryInterceptor(entity, additionalData, request);
                        }
                    } else {
                        throw new AbortLoadInterceptorException("Errore nella gestione dei permessi PEC");
                    }
            }
        }
        return super.afterSelectQueryInterceptor(entities, additionalData, request); //To change body of generated methods, choose Tools | Templates.
    }
}
