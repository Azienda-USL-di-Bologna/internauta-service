package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import edu.emory.mathcs.backport.java.util.Arrays;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPec;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author g.russo@nsi.it with collaboration of GDM and Gus
 */
@Component
@NextSdrInterceptor(name = "pec-interceptor")
public class PecInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PecInterceptor.class);
    
    @Autowired
    PermissionManager permissionManager;
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Override
    public Class getTargetEntityClass() {
        return Pec.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case FilterPecPerPermissionOfSubject:
                        /* Nel caso di FilterPerSubjectPermission in Data avremo l'id della Struttura per la quale si chiede di filtrare le pec */
                        String idStruttura = additionalData.get(InternautaConstants.AdditionalData.Keys.Data.toString());
                        Struttura struttura = new Struttura(Integer.parseInt(idStruttura));
                        try {
                            List<PermessoEntitaStoredProcedure> getPermissionsOfSubject = permissionManager.getPermissionsOfSubject(
                                struttura,
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.SPEDISCE.toString(), InternautaConstants.Permessi.Predicati.SPEDISCE_PRINCIPALE.toString()}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}), true);
                            if (getPermissionsOfSubject == null){
                                initialPredicate = Expressions.FALSE.eq(true);
                            }
                            else {
                                BooleanExpression permessoFilter = QPec.pec.id.in(
                                    getPermissionsOfSubject
                                        .stream()
                                        .map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList()));
                                initialPredicate = permessoFilter.and(initialPredicate);
                            }
                            /* Conserviamo i dati estratti dalla BlackBox */
                            this.httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.PecOfSubject, getPermissionsOfSubject);
                        } catch (BlackBoxPermissionException ex) {
                            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                            throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                        }
                    break;
                }
            }
        }
        
        // se loggedUser è CI o CA restituisco tutte le caselle pec, senza limite di azienda, altrimenti non restituisco niente
        
        return initialPredicate;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        Pec pec = (Pec) entity;        
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case AddPermissionsOnPec:
                        List<PermessoEntitaStoredProcedure> pecConPermesso = 
                                (List<PermessoEntitaStoredProcedure>) this.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.PecOfSubject);
                        if (pecConPermesso != null && !pecConPermesso.isEmpty()) {
                            List<PermessoEntitaStoredProcedure> permessiPec = 
                                    pecConPermesso.stream().filter(p -> 
                                            p.getOggetto().getIdProvenienza()
                                            .equals(pec.getId()))
                                            .collect(Collectors.toList());
                            pec.setPermessi(permessiPec);
                        }
                    break;
                    case AddGestoriOnPec:
                        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject;
                        
                        try {
                            subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(
                                Arrays.asList(new Pec[]{pec}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.ELIMINA.toString(), InternautaConstants.Permessi.Predicati.LEGGE.toString(), InternautaConstants.Permessi.Predicati.RISPONDE.toString()}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}), false);
                        } catch (BlackBoxPermissionException ex) {
                            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                            throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                        }
                        
                        if (subjectsWithPermissionsOnObject != null) {
                            List<Persona> gestori = new ArrayList();
                            subjectsWithPermissionsOnObject.stream().forEach((t) -> {
                                Optional<Persona> findById = personaRepository.findById(t.getSoggetto().getIdProvenienza());
                                if (findById.isPresent())
                                    gestori.add(findById.get());
                            });
                            pec.setGestori(gestori);
                        }
                    break;
                }
            }
        }          
        return pec;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            if ((operationsRequested.contains(InternautaConstants.AdditionalData.OperationsRequested.AddPermissionsOnPec) 
                    && this.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.PecOfSubject) != null) || 
                    operationsRequested.contains(InternautaConstants.AdditionalData.OperationsRequested.AddGestoriOnPec)) {
                for (Object entity : entities) {
                    entity = afterSelectQueryInterceptor(entity, additionalData, request);
                }
            }
        }
        return entities;
    }
}
