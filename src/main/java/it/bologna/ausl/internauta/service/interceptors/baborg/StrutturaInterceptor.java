package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.*;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Ambiti;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Predicati;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Tipi;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.Arrays;
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
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "struttura-interceptor")
@Order(1)
public class StrutturaInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StrutturaInterceptor.class);
    
    @Autowired
    PermissionManager permissionManager;
    
    @Override
    public Class getTargetEntityClass() {
        return Struttura.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
                
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
                            if (subjectsWithPermissionsOnObject == null){
                                initialPredicate = Expressions.FALSE.eq(true);
                            }
                            else {
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
                        List<PermessoEntitaStoredProcedure> struttureConPermesso = 
                                (List<PermessoEntitaStoredProcedure>) this.httpSessionData.getData(HttpSessionData.Keys.StruttureWithPecPermissions);
                        if (struttureConPermesso != null && !struttureConPermesso.isEmpty()) {
                            List<PermessoEntitaStoredProcedure> permessiStruttura = 
                                    struttureConPermesso.stream().filter(p -> 
                                            p.getSoggetto().getIdProvenienza()
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
}
