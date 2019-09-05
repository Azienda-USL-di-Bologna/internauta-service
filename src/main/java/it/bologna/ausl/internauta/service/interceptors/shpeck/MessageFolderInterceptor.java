package it.bologna.ausl.internauta.service.interceptors.shpeck;

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
import it.bologna.ausl.model.entities.shpeck.Folder.FolderType;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus & jdieme
 */
@Component
@NextSdrInterceptor(name = "messagefolder-interceptor")
public class MessageFolderInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFolderInterceptor.class);
   
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    KrintShpeckService krintShpeckService;
    
    @Override
    public Class getTargetEntityClass() {
        return MessageFolder.class;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        // TODO controllare che chi sta facendo sto update abbia almeno un permesso sulla casella del folder.
        // deve fare il contorllo una volta per più update (per via del batch che fa spostare più message in una volta sola?)
        
//        MessageFolder beforeupdateMessageFolder = (MessageFolder) beforeUpdateEntity;
//        MessageFolder messageFolder = (MessageFolder) entity;
//        messageFolder.setIdPreviousFolder(beforeupdateMessageFolder.getIdFolder());
//        return messageFolder;
        // Se sto spostando nel cestino devo avere il peremsso elimina
        MessageFolder messageFolder = (MessageFolder) entity;
        MessageFolder beforeMessageFolder = (MessageFolder) beforeUpdateEntity;
        
        if (messageFolder.getIdFolder().getType().equals(FolderType.TRASH.toString())) {
            try {
                lanciaEccezioneSeNonHaPermessoDiEliminaMessage(messageFolder.getIdMessage());
            } catch (BlackBoxPermissionException | Http403ResponseException ex) {
                throw new AbortSaveInterceptorException();
            }
        }
        
        if (!messageFolder.getIdFolder().getId().equals(beforeMessageFolder.getIdFolder().getId()) && KrintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeFolderChanged(messageFolder.getIdMessage(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_SPOSTAMENTO, messageFolder.getIdFolder(), beforeMessageFolder.getIdFolder());
        }
        
        return entity;
    }
    
    
    private void lanciaEccezioneSeNonHaPermessoDiEliminaMessage(Message message) throws AbortSaveInterceptorException, BlackBoxPermissionException, Http403ResponseException {
        // Prendo l'utente loggato
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
        
        // Prendo i permessi pec
        Map<Integer, List<String>> permessiPec;
        permessiPec = userInfoService.getPermessiPec(persona);
        
        // Controllo che ci sia almeno il RISPONDE sulla pec interessata
        List<String> permessiTrovati = permessiPec.get(message.getIdPec().getId());
        List<String> permessiSufficienti = new ArrayList();
        permessiSufficienti.add(InternautaConstants.Permessi.Predicati.ELIMINA.toString());
        if (Collections.disjoint(permessiTrovati, permessiSufficienti)) {
            throw new Http403ResponseException("1", "Non hai il permesso di eliminare mail");
        }
    }
    
}
