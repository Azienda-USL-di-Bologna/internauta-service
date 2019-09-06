package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPec;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    
    @Autowired
    StrutturaRepository strutturaRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Override
    public Class getTargetEntityClass() {
        return Pec.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        String idAzienda = null;
        String idStruttura = null;
        String idPersona = null;
        
        Azienda azienda = null;
        Struttura struttura = null;
        Persona persona = null;

        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested == null || operationsRequested.isEmpty()) {
//            List<PermessoEntitaStoredProcedure> pecWithStandardPermissions;
//            try {
//                pecWithStandardPermissions = permissionManager.getPermissionsOfSubject(
//                        super.person,
//                        Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.LEGGE.toString(), InternautaConstants.Permessi.Predicati.RISPONDE.toString(), InternautaConstants.Permessi.Predicati.ELIMINA.toString()}),
//                        Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
//                        Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}), false);
//            } catch (BlackBoxPermissionException ex) {
//                LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
//                throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
//            }
//        
//            if (pecWithStandardPermissions == null){
//                initialPredicate = Expressions.FALSE.eq(true);
//            } else {
//                BooleanExpression pecFilter = QPec.pec.id.in(
//                    pecWithStandardPermissions
//                        .stream()
//                        .map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList()));
//                initialPredicate = pecFilter.and(initialPredicate);
//            }
        } else {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case FilterPecPerPermissionOfSubject:
                        /* Nel caso di FilterPerSubjectPermission in Data avremo l'id della Struttura per la quale si chiede di filtrare le pec */
                        idStruttura = additionalData.get(InternautaConstants.AdditionalData.Keys.idStruttura.toString());
                        struttura = new Struttura(Integer.parseInt(idStruttura));
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
                    case FilterPecPerStandardPermissions:
                        List<PermessoEntitaStoredProcedure> pecWithStandardPermissions;
                        try {
                            pecWithStandardPermissions = permissionManager.getPermissionsOfSubject(
                                    authenticatedSessionData.getPerson(),
                                    Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.LEGGE.toString(), InternautaConstants.Permessi.Predicati.RISPONDE.toString(), InternautaConstants.Permessi.Predicati.ELIMINA.toString()}),
                                    Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                                    Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}), false);
                        } catch (BlackBoxPermissionException ex) {
                            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                            throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                        }

                        if (pecWithStandardPermissions == null){
                            initialPredicate = Expressions.FALSE.eq(true);
                        } else {
                            BooleanExpression pecFilter = QPec.pec.id.in(
                                pecWithStandardPermissions
                                    .stream()
                                    .map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList()));
                            initialPredicate = pecFilter.and(initialPredicate);
                        }
                    break;
                    case LoadDataPerInterfacciaElencoPec:
                        idAzienda = additionalData.get(InternautaConstants.AdditionalData.Keys.idAzienda.toString());
                        idStruttura = additionalData.get(InternautaConstants.AdditionalData.Keys.idStruttura.toString());
                        idPersona = additionalData.get(InternautaConstants.AdditionalData.Keys.idPersona.toString());
                        if(idAzienda != null){
                            azienda = new Azienda(Integer.parseInt(idAzienda));
                        }
                        if(idStruttura != null){
                            struttura = new Struttura(Integer.parseInt(idStruttura));
                        }
                        if(idPersona != null){
                            persona = new Persona(Integer.parseInt(idPersona));
                        }
                        
                        
                        List<Integer> pecDellaStruttura = null;
                        List<Integer> pecDellaPersona = null;
                        

                    {
                        if(struttura != null){                     
                            try {
                                List<PermessoEntitaStoredProcedure> permessiDellaStruttura = permissionManager.getPermissionsOfSubject(
                                        struttura,
                                        Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.SPEDISCE.toString(), InternautaConstants.Permessi.Predicati.SPEDISCE_PRINCIPALE.toString()}),
                                        Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                                        Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}),
                                        true);
                                if(permessiDellaStruttura != null){                                   
                                    pecDellaStruttura = permessiDellaStruttura.stream().map(permesso -> permesso.getOggetto().getIdProvenienza()).collect(Collectors.toList());
                                }
                            } catch (BlackBoxPermissionException ex) {
                                LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                                throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                            }
                        }
                        
                        if(persona != null){
                            try {
                                List<PermessoEntitaStoredProcedure> permessiDellaPersona = permissionManager.getPermissionsOfSubject(
                                        persona,
                                        Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.LEGGE.toString(), InternautaConstants.Permessi.Predicati.RISPONDE.toString(), InternautaConstants.Permessi.Predicati.ELIMINA.toString()}),
                                        Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                                        Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}),
                                        false);
                                if(permessiDellaPersona != null){                                   
                                    pecDellaPersona = permessiDellaPersona.stream().map(permesso -> permesso.getOggetto().getIdProvenienza()).collect(Collectors.toList());
                                }
                                
                            } catch (BlackBoxPermissionException ex) {
                                LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                                throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                            }
                        }
                    }
                    
                    // interezione delle due liste di permessi
                    Set<Integer> pecVisibili = new HashSet<>();

                    if(pecDellaStruttura != null){                        
                        pecVisibili.addAll(pecDellaStruttura);
                        if(pecDellaPersona != null){                        
                            pecVisibili.retainAll(pecDellaPersona);
                        }
                    }
                    else if(pecDellaPersona != null){                        
                        pecVisibili.addAll(pecDellaPersona);
                    }

                    if (idAzienda != null){
                        BooleanExpression aziendaFilter = QPec.pec.pecAziendaList.any().idAzienda.id.eq(Integer.parseInt(idAzienda)).or(QPec.pec.pecAziendaList.isEmpty());
                        initialPredicate = aziendaFilter.and(initialPredicate);
                    }
                    if(idStruttura != null || idPersona != null){
                        BooleanExpression permessoFilter = null;
                        if(!pecVisibili.isEmpty()){                            
                            permessoFilter = QPec.pec.id.in(pecVisibili);
                        } else {
                            permessoFilter = Expressions.FALSE.eq(true);
                        }
                        initialPredicate = permessoFilter.and(initialPredicate);
                    }                                        
                                                        
                    break;

                }
            }
        }
        return initialPredicate;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Pec pec = (Pec) entity;
        String idAzienda = null;
        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject;
        

        
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
                        idAzienda = additionalData.get(InternautaConstants.AdditionalData.Keys.idAzienda.toString());                        
                        
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
                                                      
                            List<Persona> gestoriAzienda = new ArrayList<>();
                                                                                    
                            if(idAzienda != null){                 
                                for(Persona persona: gestori){
                                    for(Utente utente: persona.getUtenteList()){
                                        if(utente.getIdAzienda().getId().toString().equals(idAzienda)){
                                            gestoriAzienda.add(persona);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                gestoriAzienda = gestori;
                            }
                           
                            Collections.sort(gestoriAzienda, Comparator.comparing(Persona::getDescrizione));
                            
                            pec.setGestori(gestoriAzienda);
                        }
                    break;
                    case LoadDataPerInterfacciaElencoPec:    
                                                
                        idAzienda = additionalData.get(InternautaConstants.AdditionalData.Keys.idAzienda.toString());                        

                        // Aggiungo i gestori (stessa parte del case AddGestoriOnPec)                                            
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
                                                      
                            List<Persona> gestoriAzienda = new ArrayList<>();
                                                                                    
                            if(idAzienda != null){                 
                                for(Persona persona: gestori){
                                    for(Utente utente: persona.getUtenteList()){
                                        if(utente.getIdAzienda().getId().toString().equals(idAzienda)){
                                            gestoriAzienda.add(persona);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                gestoriAzienda = gestori;
                            }
                           
                            Collections.sort(gestoriAzienda, Comparator.comparing(Persona::getDescrizione));
                            
                            pec.setGestori(gestoriAzienda);
                        }
                        
                        
                        // Aggiungo le strutture
                        try {
                            subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(
                                Arrays.asList(new Pec[]{pec}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.SPEDISCE.toString(), InternautaConstants.Permessi.Predicati.SPEDISCE_PRINCIPALE.toString()}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}), false);
                        } catch (BlackBoxPermissionException ex) {
                            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                            throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
                        }
                        if (subjectsWithPermissionsOnObject != null) {
                            List<Struttura> strutture = new ArrayList();
                            for(PermessoEntitaStoredProcedure subjectWithPermission: subjectsWithPermissionsOnObject){
                                Optional<Struttura> findById = strutturaRepository.findById(subjectWithPermission.getSoggetto().getIdProvenienza());
                                if (findById.isPresent()) {
                                    Struttura struttura = findById.get();
                                    Boolean propaga = subjectWithPermission.getCategorie().get(0).getPermessi().get(0).getPropagaSoggetto();
                                    Boolean principale = subjectWithPermission.getCategorie().get(0).getPermessi().get(0).getPredicato().equals(InternautaConstants.Permessi.Predicati.SPEDISCE_PRINCIPALE.toString());                                                                        
                                    struttura.setPropagaPermessoPec(propaga);
                                    struttura.setIsPermessoPecPrincipale(principale);
                                    strutture.add(struttura);
                                }
                            }
                                                         
                            List<Struttura> struttureAzienda = new ArrayList<>();
                                                                                    
                            if(idAzienda != null){                
                                for(Struttura struttura: strutture){
                                    if(struttura.getIdAzienda().getId().toString().equals(idAzienda)){
                                        struttureAzienda.add(struttura);
                                    }                                   
                                }
                            } else {
                                struttureAzienda = strutture;
                            }
                           
                            Collections.sort(struttureAzienda, Comparator.comparing(Struttura::getNome));
                            
                           pec.setStrutture(struttureAzienda);
                        }
                    break;
                }
            }
        }
        
        //Se non ho diritti particolari su una pec metto la password a null
        if (!isCI(authenticatedSessionData.getUser())) {
            Persona persona = personaRepository.getOne(authenticatedSessionData.getPerson().getId());
            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(persona).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());
            
            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                pec.setPassword(null);
            } else {
                List<Integer> idAziendePec = pec.getPecAziendaList().stream().map(pecAzienda -> pecAzienda.getIdAzienda().getId()).collect(Collectors.toList());
                
                if (!idAziendeCA.containsAll(idAziendePec)) {
                    pec.setPassword(null);
                }
            }
        }
        
        return pec;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
//        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
//        if (operationsRequested != null && !operationsRequested.isEmpty()) {
//            if ((operationsRequested.contains(InternautaConstants.AdditionalData.OperationsRequested.AddPermissionsOnPec) 
//                    && this.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.PecOfSubject) != null) || 
//                    operationsRequested.contains(InternautaConstants.AdditionalData.OperationsRequested.AddGestoriOnPec)) {
//                for (Object entity : entities) {
//                    entity = afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
//                }
//            }
//        }
        for (Object entity : entities) {
            entity = afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
        }
        return entities;
    }
    
    /*
     * Condizioni per l'INSERT.
     * Il CI può sempre inserire una PEC.
     * Una persona che è CA in almeno un azienda può sempre inserire una PEC.
     * NB: Il CA non può inserire una PEC con la pecAziendaList che contiene aziende non sue, ma questo controllo avviene nel PecAziendaInterceptor
     */
    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        LOGGER.info("in: beforeCreateEntityInterceptor di Pec");
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();

        if (!isCI(authenticatedSessionData.getUser())) {
            Persona persona = personaRepository.getOne(authenticatedSessionData.getPerson().getId());
            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(persona).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());
            
            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                // Non sono ne CA ne CI fermo tutto.
                throw new AbortSaveInterceptorException();
            }
        }
        
        return entity;
    }

    /*
     * Condizioni per l'UPDATE.
     * Il CI può fare l'UPDATE su qualsiasi PEC.
     * Il CA può fare l'UPDATE sulle PEC che sono associate alla azienda di cui è CA e le stesse non devono essere associate con altre aziende.
     * -- TODO: Condizione attualmente non vera: Oppure il CA può fare l'UPDATE se sta intervenendo solo su pecAziendaList. (Quali aziende ha aggiunto/cancellato lo controllo nel PecAziendaInterceptor)
     * In qualsiasi altro caso l'UPDATE è impedito.
     */
    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        LOGGER.info("in: beforeUpdateEntityInterceptor di Pec");
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        
        Pec pec = (Pec) entity;
        
        if (!isCI(authenticatedSessionData.getUser())) {
            Persona persona = personaRepository.getOne(authenticatedSessionData.getPerson().getId());
            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(persona).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());
            
            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                // Non sono ne CA ne CI fermo tutto.
                throw new AbortSaveInterceptorException();
            } else {
                // Qui devo controllare che la pec sia attaccata ad aziende di cui sono CA, oppure che non sia attaccata ad alcuna azienda.
                // Se pecAziendaList è conenuta nella lista di aziendeCA l'UPDATE è permesso.
                List<Integer> idAziendePec = pec.getPecAziendaList().stream().map(pecAzienda -> pecAzienda.getIdAzienda().getId()).collect(Collectors.toList());
                
                if (!idAziendeCA.containsAll(idAziendePec)) {
                    // Non sono CA di tutte le aziende associate con la PEC
                    // TODO: Quando avremo il Clone() nuovo delle entiries dovrà valere: L'UPDATE è permesso solo se non sto cambiando alcun campo tranne la pecAziendaList.
                    throw new AbortSaveInterceptorException();
                }
                
            }
        }
        
        return entity;
    }
    
    /*
     * Condizioni per la DELETE.
     * In nessuna circostanza permetto la DELETE.
     */
    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        LOGGER.info("in: beforeDeleteEntityInterceptor di Pec");
        throw new AbortSaveInterceptorException();
    }
    
    
}
