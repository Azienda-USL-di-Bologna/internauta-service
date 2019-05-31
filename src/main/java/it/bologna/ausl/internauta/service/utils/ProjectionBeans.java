package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.interceptors.ribaltoneutils.RibaltoneDaLanciareInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.CustomAziendaLogin;
import it.bologna.ausl.model.entities.baborg.projections.CustomPersonaWithImpostazioniApplicazioniList;
import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteLogin;
import it.bologna.ausl.model.entities.baborg.projections.RibaltoneDaLanciareCustom;
import it.bologna.ausl.model.entities.baborg.projections.UtenteStrutturaWithIdAfferenzaStrutturaCustom;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecAziendaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.configuration.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configuration.projections.generated.ImpostazioniApplicazioniWithPlainFields;
import it.bologna.ausl.model.entities.scrivania.projections.generated.AttivitaWithIdPersona;
import it.bologna.ausl.model.entities.shpeck.Address;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageAddress;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.projections.generated.AddressWithPlainFields;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageAddressWithIdAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageFolderWithIdFolder;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageTagWithIdTag;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author guido
 */
@Component
public class ProjectionBeans {
    
    @Autowired
    protected ProjectionFactory factory;
    
    @Autowired
    protected CachedEntities cachedEntities;
    
    @Autowired
    protected ImpostazioniApplicazioniRepository impostazioniApplicazioniRepository;
    
    @Autowired
    protected UtenteRepository utenteRepository;
    
    @Autowired
    ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    protected Utente user, realUser;
    protected Persona person, realPerson;
    protected int idSessionLog;
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ProjectionBeans.class);

    protected void setAuthenticatedUserProperties() {
        TokenBasedAuthentication authentication = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
        user = (Utente) authentication.getPrincipal();
        realUser = (Utente) authentication.getRealUser();
        idSessionLog = authentication.getIdSessionLog();
        person = cachedEntities.getPersona(user);
        realPerson = cachedEntities.getPersona(realUser);
    }
    
    public UtenteWithIdPersona getUtenteConPersona(Utente utente){
        if (utente != null) {
            return factory.createProjection(UtenteWithIdPersona.class, utente);
        } else {
            return null;
        }
    }
    
    public UtenteStrutturaWithIdAfferenzaStrutturaCustom 
        getUtenteStrutturaWithIdAfferenzaStrutturaCustom(UtenteStruttura utenteStruttura){
        return factory.createProjection(UtenteStrutturaWithIdAfferenzaStrutturaCustom.class, utenteStruttura);
    }
    
    public StrutturaWithIdAzienda getStrutturaConAzienda(Struttura struttura){
        return factory.createProjection(StrutturaWithIdAzienda.class, struttura);
    }
    
    public List<AttivitaWithIdPersona> getAttivitaWithIdPersona(Azienda azienda) {
        return azienda.getAttivitaList().stream().map(
                a -> {
                    return factory.createProjection(AttivitaWithIdPersona.class, a);
                }
        ).collect(Collectors.toList());
    }
    
    public CustomUtenteLogin getUtenteRealeWithIdPersonaImpostazioniApplicazioniList(Utente utente) {
        //Utente refreshedUtente = utenteRepository.getOne(utente.getId());
//        Persona persona = utente.getIdPersona();
        if (utente.getUtenteReale() != null)
            return factory.createProjection(CustomUtenteLogin.class, utente.getUtenteReale());
        else
            return null;
//        
//            if (impostazioniApplicazioniList != null && !impostazioniApplicazioniList.isEmpty()) {
//            return impostazioniApplicazioniList.stream().map(
//                        imp -> factory.createProjection(ImpostazioniApplicazioniWithPlainFields.class, imp)
//                    ).collect(Collectors.toList());
//        } else
//            return null;
    }
    public CustomPersonaWithImpostazioniApplicazioniList getIdPersonaWithImpostazioniApplicazioniList(Utente utente) {
        return factory.createProjection(CustomPersonaWithImpostazioniApplicazioniList.class, utente.getIdPersona());
    }
    
    public AziendaWithPlainFields getAziendaWithPlainFields(Utente utente) {
        return factory.createProjection(AziendaWithPlainFields.class, utente.getIdAzienda());
    }
    
    public List<ImpostazioniApplicazioniWithPlainFields> getImpostazioniApplicazioniListWithPlainFields(Persona persona) {
        List<ImpostazioniApplicazioni> impostazioniApplicazioniList = persona.getImpostazioniApplicazioniList();
        if (impostazioniApplicazioniList != null && !impostazioniApplicazioniList.isEmpty()) {
            return impostazioniApplicazioniList.stream().filter(imp -> imp.getIdApplicazione().getId().equals(persona.getApplicazione())).
                    map(
                        imp -> factory.createProjection(ImpostazioniApplicazioniWithPlainFields.class, imp)
                    ).collect(Collectors.toList());
        } else
            return null;
    }

    public List<PecAziendaWithIdAzienda> getPecAziendaListWithIdAzienda(List<PecAzienda> pecAziendaList){
        if (pecAziendaList != null && !pecAziendaList.isEmpty()) {
            return pecAziendaList.stream().map(pecAzienda -> factory.createProjection(PecAziendaWithIdAzienda.class, pecAzienda))
                    .collect(Collectors.toList());
        } else{
            return null;
        }
    }

    public List<MessageAddressWithIdAddress> getMessageAddressListWithIdAddress(Message message){
        if (message != null) {
            List<MessageAddress> messageAddresssList = message.getMessageAddressList();
            if (messageAddresssList != null && !messageAddresssList.isEmpty()) {
                return messageAddresssList.stream().map(messageAddress -> factory.createProjection(MessageAddressWithIdAddress.class, messageAddress))
                        .collect(Collectors.toList());
            } else{
                return null;
            }
        } else {
            return null;
        }
    }

    public List<MessageTagWithIdTag> getMessageTagListWithIdTag(Message message){
        if (message != null) {
            List<MessageTag> messageTagList = message.getMessageTagList();
            if (messageTagList != null && !messageTagList.isEmpty()) {
                return messageTagList.stream().map(messageTag -> factory.createProjection(MessageTagWithIdTag.class, messageTag))
                        .collect(Collectors.toList());
            } else{
                return null;
            }
        } else {
            return null;
        }
    }
    
     
    public List<MessageFolderWithIdFolder> getMessageFolderListWithIdFolder(Message message){
        try {
            return (List<MessageFolderWithIdFolder>) projectionsInterceptorLauncher.lanciaInterceptorCollection(message, "getMessageFolderList", MessageFolderWithIdFolder.class.getSimpleName());
        } catch (EntityReflectionException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | NoSuchFieldException | InterceptorException | AbortLoadInterceptorException ex) {
            LOGGER.error("errore nell'estrazione di getRibaltoneDaLanciareList", ex);
            return null;
        }
    }
    
    public List<RibaltoneDaLanciareCustom> getRibaltoneDaLanciareListWithIdUtente(Azienda a) {
        try {
            return (List<RibaltoneDaLanciareCustom>) projectionsInterceptorLauncher.lanciaInterceptorCollection(a, "getRibaltoneDaLanciareList", RibaltoneDaLanciareCustom.class.getSimpleName());
        } catch (EntityReflectionException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | NoSuchFieldException | InterceptorException | AbortLoadInterceptorException ex) {
            LOGGER.error("errore nell'estrazione di getRibaltoneDaLanciareList", ex);
            return null;
        }
    }
    
    public String getUrlCommand(Azienda azienda) {
        String result = "";
        Azienda aziendaUtenteLoggato = user.getIdAzienda();
        
        result = "aziendaCorrente: " + azienda.getNome() +
                " - aziendaUtenteLoggato: " + aziendaUtenteLoggato.getNome();
                        
        return result;
    }
    
    public CustomAziendaLogin getAziendaLogin(Utente utente) {
        return factory.createProjection(CustomAziendaLogin.class, utente.getIdAzienda());
    }
    
}
