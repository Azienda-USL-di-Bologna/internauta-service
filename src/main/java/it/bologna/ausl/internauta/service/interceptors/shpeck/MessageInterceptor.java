package it.bologna.ausl.internauta.service.interceptors.shpeck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.Folder.FolderType;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
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
    ShpeckUtils shpeckUtils;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KrintShpeckService krintShpeckService;

    @Autowired
    MessageInterceptorUtils messageInterceptorUtils;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Override
    public Class getTargetEntityClass() {
        return Message.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedUserProperties = super.getAuthenticatedUserProperties();
//        Persona persona = authenticatedUserProperties.getPerson();
//        BooleanExpression messageInPecWithPermission;
//
//        try {
//            Map<Integer, List<String>> permessiPec = userInfoService.getPermessiPec(persona);
//            if (!permessiPec.isEmpty()) {
//                List<Integer> myPec = new ArrayList<Integer>();
//                myPec.addAll(permessiPec.keySet());
//                messageInPecWithPermission = QMessage.message.idPec.id.in(myPec);
//                Expressions.FALSE.eq(true);
//            } else {
//                messageInPecWithPermission = Expressions.FALSE.eq(true);
//            }
//        } catch (BlackBoxPermissionException ex) {
//            throw new AbortLoadInterceptorException("errore nella lettura del permessi sulle caselle pec", ex);
//        }
//
//        initialPredicate = messageInPecWithPermission.and(initialPredicate);
        initialPredicate = messageInterceptorUtils.messageInPecWithPermission(authenticatedUserProperties, Message.class).and(initialPredicate);

        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case FiltraSuTuttiFolderTranneTrash:
                        QMessage qMessage = QMessage.message;
                        BooleanExpression filtro = qMessage.messageFolderList.any().idFolder.isNotNull()
                                .and(qMessage.messageFolderList.any().idFolder.type.ne(FolderType.TRASH.toString()));
                        initialPredicate = filtro.and(initialPredicate);
                        break;
                }
            }
        }
        return initialPredicate;
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
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {

        Message message = (Message) entity;

        Message mBefore;
        try {
            mBefore = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, Message.class);

        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }
         // Se ho cambiato il seen lo loggo
        if (mainEntity && (!mBefore.getSeen().equals(message.getSeen())) && krintUtils.doIHaveToKrint(request)) {
            if (message.getSeen()) {
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
