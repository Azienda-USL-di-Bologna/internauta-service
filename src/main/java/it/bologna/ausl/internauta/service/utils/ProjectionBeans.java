package it.bologna.ausl.internauta.service.utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
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
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageAddress;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageAddressWithIdAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageFolderWithIdFolder;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageTagWithIdTag;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;
import it.bologna.ausl.model.entities.baborg.projections.CustomPersonaLogin;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;

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
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    UserInfoService userInfoService;

    @Autowired
    HttpSessionData httpSessionData;
    
    @Autowired
    ObjectMapper objectMapper;

    protected Utente user, realUser;
    protected Persona person, realPerson;
    protected int idSessionLog;
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ProjectionBeans.class);

    protected void setAuthenticatedUserProperties() throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        user = authenticatedSessionData.getUser();
        realUser = authenticatedSessionData.getRealUser();
        idSessionLog = authenticatedSessionData.getIdSessionLog();
        person = authenticatedSessionData.getPerson();
        realPerson = authenticatedSessionData.getRealPerson();
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
    public CustomPersonaLogin getIdPersonaWithImpostazioniApplicazioniList(Utente utente) {
        return factory.createProjection(CustomPersonaLogin.class, utente.getIdPersona());
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
    
    public String getUrlCommand(Azienda azienda) throws IOException {
        
        final String FROM = "&from=INTERNAUTA";
        final String APP_URL = "/Procton/Procton.htm";
                
        Utente utente = (Utente)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);
        AziendaParametriJson parametriAziendaLogin = AziendaParametriJson.parse(objectMapper, utente.getIdAzienda().getParametri());                
        AziendaParametriJson parametriAziendaDestinazione = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
                                      
        String crossLoginUrlTemplate = parametriAziendaDestinazione.getCrossLoginUrlTemplate();
        
        String stringToEncode = "?CMD=ricevi_from_pec_int;[id_pec]";
        
        
        stringToEncode += "&utenteImpersonato=" + utente.getIdPersona().getCodiceFiscale();
        
        if(utente.getUtenteReale() != null ){            
            stringToEncode += "&utenteLogin=" + utente.getUtenteReale().getIdPersona().getCodiceFiscale();
        } else {
            stringToEncode += "&utenteLogin=" + utente.getIdPersona().getCodiceFiscale();           
        }       
        stringToEncode += "&idSessionLog=" + httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.IdSessionLog);
        stringToEncode += FROM;
        stringToEncode += "&modalitaAmministrativa=0";
        
        String encodedParams = URLEncoder.encode(stringToEncode, "UTF-8");                
        
        String assembledUrl = crossLoginUrlTemplate
//            .replace("?entityID=", "")  // TODO da togliere
//            .replace("&target=", "")   // TODO da togliere
            .replace("[target-login-path]", parametriAziendaDestinazione.getLoginPath()) //parametriAziendaDestinazione.getLoginPath())
            .replace("[entity-id]", parametriAziendaLogin.getEntityId()) //parametriAziendaLogin.getEntityId())
            .replace("[app]", APP_URL)
            .replace("[encoded-params]", encodedParams)
                    ;
        
        return assembledUrl;
        
    }
    
//    public CustomAziendaLogin getAziendaLogin(Utente utente) {
//        return factory.createProjection(CustomAziendaLogin.class, utente.getIdAzienda());
//    }
    
}
