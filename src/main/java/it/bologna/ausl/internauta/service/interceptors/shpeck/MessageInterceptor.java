package it.bologna.ausl.internauta.service.interceptors.shpeck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Template;
import com.querydsl.core.types.TemplateFactory;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it> with GDM and Gus collaboration
 */
@Component
@NextSdrInterceptor(name = "message-interceptor")
public class MessageInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageInterceptor.class);
    
    @Autowired
    PermissionManager permissionManager;
    
    @Autowired
    ProjectionFactory factory;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    KrintShpeckService krintShpeckService;
    
    @Override
    public Class getTargetEntityClass() {
        return Message.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {

        try {
            Template a = TemplateFactory.DEFAULT.create("to_tsquery('italian', '$${0}:*$$') {1} tscol");
            String value = "middleware";
            String field = "tscol";
            BooleanExpression booleanTemplate = Expressions.booleanTemplate("FUNCTION('fts_match', italian, {0}, {1})= true", Expressions.stringPath(value), QMessage.message.tscol);
            super.getAuthenticatedUserProperties();
            return initialPredicate;
            
//        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
//        if (operationsRequested != null && !operationsRequested.isEmpty()) {
//            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
//                switch (operationRequested) {
//                    case GetPermessiGestoriPec: 
//                        /* Nel caso di GetPermessiGestoriPec in Data avremo l'id della PEC della quale si chiedono i permessi */
//                        String idPec = additionalData.get(AdditionalData.Keys.idPec.toString());
//                        Pec pec = new Pec(Integer.parseInt(idPec));
//                        String idAzienda = additionalData.get(AdditionalData.Keys.idAzienda.toString());
//                        if(StringUtils.hasText(idAzienda)){                           
//                            BooleanExpression aziendaFilter = QPersona.persona.utenteList.any().idAzienda.id.eq(Integer.parseInt(idAzienda));
//                            initialPredicate = aziendaFilter.and(initialPredicate);
//                        }
//                        try {
//                            List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(
//                                Arrays.asList(new Pec[]{pec}),
//                                Arrays.asList(new String[]{Predicati.ELIMINA.toString(), Predicati.LEGGE.toString(), Predicati.RISPONDE.toString()}),
//                                Arrays.asList(new String[]{Ambiti.PECG.toString()}),
//                                Arrays.asList(new String[]{Tipi.PEC.toString()}), false);
//                            if (subjectsWithPermissionsOnObject == null){
//                                initialPredicate = Expressions.FALSE.eq(true);
//                            }
//                            else {
//                                BooleanExpression permessoFilter = QPersona.persona.id.in(
//                                    subjectsWithPermissionsOnObject
//                                        .stream()
//                                        .map(p -> p.getSoggetto().getIdProvenienza()).collect(Collectors.toList()))
//                                        .and(initialPredicate);
//                                initialPredicate = permessoFilter.and(initialPredicate);
//                            }                            
//                            /* Conserviamo i dati estratti dalla BlackBox */
//                            this.httpSessionData.putData(HttpSessionData.Keys.PersoneWithPecPermissions, subjectsWithPermissionsOnObject);
//                        } catch (BlackBoxPermissionException ex) {
//                            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
//                            throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
//                        }
//                        break;
//                }
//            }
//        }
        } catch (Exception ex) {
            throw new AbortLoadInterceptorException(ex);
        }
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Message message = (Message) entity;
        try {
            lanciaEccezioneSeNonHaPermessoDiEliminaMessage(message);
        } catch (BlackBoxPermissionException | Http403ResponseException ex) {
            throw new AbortSaveInterceptorException();
        }
        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); 
    }
    
    private void lanciaEccezioneSeNonHaPermessoDiEliminaMessage(Message message) throws AbortSaveInterceptorException, BlackBoxPermissionException, Http403ResponseException {
        // Prendo l'utente loggato
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
        
        // Prendo i permessi pec
        Map<Integer, List<String>> permessiPec = null;
        permessiPec = userInfoService.getPermessiPec(persona);
        
        // Controllo che ci sia almeno il RISPONDE sulla pec interessata
        List<String> permessiTrovati = permessiPec.get(message.getIdPec().getId());
        List<String> permessiSufficienti = new ArrayList();
        permessiSufficienti.add(InternautaConstants.Permessi.Predicati.ELIMINA.toString());
        if (Collections.disjoint(permessiTrovati, permessiSufficienti)) {
            throw new Http403ResponseException("1", "Non hai il permesso di eliminare mail");
        }
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        
        Message message = (Message) entity;
        Message mBefore = (Message) beforeUpdateEntity;
        
        // Se ho cambiato il seen lo loggo
        if(mainEntity && (mBefore.getSeen() != message.getSeen()) && KrintUtils.doIHaveToKrint(request)){
            if(message.getSeen()){
                // ha settato "Letto"
                krintShpeckService.writeSeenOrNotSeen(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_LETTO);
            } else {
                // ha settato "da leggere"
                krintShpeckService.writeSeenOrNotSeen(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_DA_LEGGERE);
            }
        }
        
        return entity;
               
    }
    
    
}
