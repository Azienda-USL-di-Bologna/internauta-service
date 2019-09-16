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
import it.bologna.ausl.model.entities.configuration.Applicazione;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckPec;

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
    protected Applicazione.Applicazioni applicazione;
    protected int idSessionLog;
    final String APP_URL_PICO = "/Procton/Procton.htm";
    final String APP_URL_BABEL = "/Babel/Babel.htm";
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ProjectionBeans.class);

    protected void setAuthenticatedUserProperties() throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        user = authenticatedSessionData.getUser();
        realUser = authenticatedSessionData.getRealUser();
        idSessionLog = authenticatedSessionData.getIdSessionLog();
        person = authenticatedSessionData.getPerson();
        realPerson = authenticatedSessionData.getRealPerson();
        applicazione = authenticatedSessionData.getApplicazione();
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
    
    public List<ImpostazioniApplicazioniWithPlainFields> getImpostazioniApplicazioniListWithPlainFields(Persona persona) throws BlackBoxPermissionException {
        setAuthenticatedUserProperties();
        List<ImpostazioniApplicazioni> impostazioniApplicazioniList = persona.getImpostazioniApplicazioniList();
        if (impostazioniApplicazioniList != null && !impostazioniApplicazioniList.isEmpty()) {
            return impostazioniApplicazioniList.stream().filter(imp -> imp.getIdApplicazione().getId().equals(applicazione.toString())).
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
    
    /**
     * Restituisce gli url da mettere nelle aziende dell'utente, 
     * per chiamare le funzioni dell'onCommand sulle applicazioni Inde
     * @param azienda
     * @return
     * @throws IOException 
     */
    public Map<String, String> getUrlCommands(Azienda azienda) throws IOException {                
        final String FROM = "&from=INTERNAUTA";
        
        Map<String, String> result = new HashMap<>();
                
        Utente utente = (Utente)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);
        AziendaParametriJson parametriAziendaLogin = AziendaParametriJson.parse(objectMapper, utente.getIdAzienda().getParametri());                
        AziendaParametriJson parametriAziendaDestinazione = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        String crossLoginUrlTemplate = parametriAziendaDestinazione.getCrossLoginUrlTemplate();
        String commonStringToEncode = commonStringToEncode(utente, FROM);
        
        addRegistrationUrlCommands(result, commonStringToEncode, parametriAziendaLogin, parametriAziendaDestinazione, crossLoginUrlTemplate);
        addArchiveUrlCommands(result, commonStringToEncode, parametriAziendaLogin, parametriAziendaDestinazione, crossLoginUrlTemplate);
        
        return result;        
    }
    
    private String commonStringToEncode(Utente utente, String from) {
        String stringToEncode = "";
        stringToEncode += "&richiesta=[richiesta]";        
        stringToEncode += "&utenteImpersonato=" + utente.getIdPersona().getCodiceFiscale();
        if(utente.getUtenteReale() != null ){
            stringToEncode += "&utenteLogin=" + utente.getUtenteReale().getIdPersona().getCodiceFiscale();
        } else {
            stringToEncode += "&utenteLogin=" + utente.getIdPersona().getCodiceFiscale();
        }
        stringToEncode += "&idSessionLog=" + httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.IdSessionLog);
        stringToEncode += from;
        stringToEncode += "&modalitaAmministrativa=0";
        
        return stringToEncode;
    }
    
    private void addRegistrationUrlCommands(
            Map<String, String> result, 
            String commonStringToEncode, 
            AziendaParametriJson parametriAziendaLogin, 
            AziendaParametriJson parametriAziendaDestinazione,
            String crossLoginUrlTemplate) throws UnsupportedEncodingException {
        // ho due casi praticamente uguali sulla protocollazione di una pec. Il caso in cui creo un nuovo Protocollo 
        // e il caso in cui aggiungo la pec a un protocollo già esistente
        // cambia solo il valore del parametro CMD, quindi fascio un ciclo per gestire questi due casi
        String stringToEncode = "";
        
        for(int i = 0; i < 2; i++){
            stringToEncode = "";
            if(i == 0){
                stringToEncode = "?CMD=ricevi_from_pec;[id_message]";
            } else {
                stringToEncode = "?CMD=add_from_pec;[id_message]";
            }
            stringToEncode += "&id_sorgente=[id_sorgente]";
            stringToEncode += "&pec_ricezione=[pec_ricezione]";
            stringToEncode += commonStringToEncode;
            
            String encodedParams = URLEncoder.encode(stringToEncode, "UTF-8");                

            String assembledUrl = crossLoginUrlTemplate
                .replace("[target-login-path]", parametriAziendaDestinazione.getLoginPath()) //parametriAziendaDestinazione.getLoginPath())
                .replace("[entity-id]", parametriAziendaLogin.getEntityId()) //parametriAziendaLogin.getEntityId())
                .replace("[app]", APP_URL_PICO)
                .replace("[encoded-params]", encodedParams);
            
            if(i == 0){
                result.put(InternautaConstants.UrlCommand.Keys.PROTOCOLLA_PEC_NEW.toString(), assembledUrl);
            } else {
                result.put(InternautaConstants.UrlCommand.Keys.PROTOCOLLA_PEC_ADD.toString(), assembledUrl);
            }
        }
    } 
    
    private void addArchiveUrlCommands(
            Map<String, String> result, 
            String commonStringToEncode, 
            AziendaParametriJson parametriAziendaLogin, 
            AziendaParametriJson parametriAziendaDestinazione,
            String crossLoginUrlTemplate) throws UnsupportedEncodingException {
        String stringToEncode = "";
        stringToEncode = "?CMD=fascicola_shpeck;[id_message]";
        //stringToEncode = "CMD=ricevi_from_pec_int;[id_message]"; //local
        stringToEncode += commonStringToEncode;
        String encodedParams = URLEncoder.encode(stringToEncode, "UTF-8");                
        String assembledUrl = crossLoginUrlTemplate
            .replace("[target-login-path]", parametriAziendaDestinazione.getLoginPath()) //parametriAziendaDestinazione.getLoginPath())
            .replace("[entity-id]", parametriAziendaLogin.getEntityId()) //parametriAziendaLogin.getEntityId())
            .replace("[app]", APP_URL_BABEL)
            .replace("[encoded-params]", encodedParams);
        result.put(InternautaConstants.UrlCommand.Keys.ARCHIVE_MESSAGE.toString(), assembledUrl);
    } 
    
    /**
     * restituisce i parametri dell'azienda che servono 
     * al front end e non contengono informazioni sensibili 
     * @return
     */
    public Map<String, String> getParametriAziendaFrontEnd() throws IOException{
        
        final String LOGOUT_URL_KEY = "logoutUrl";
                
        Map<String, String> result = new HashMap<>();
        
        Utente utente = (Utente)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);
        
        AziendaParametriJson parametri = AziendaParametriJson.parse(objectMapper, utente.getIdAzienda().getParametri());

        result.put(LOGOUT_URL_KEY, parametri.getLogoutUrl());
        
        return result;
    }
    

    public KrintShpeckPec getPecKrint(Message message){
        return factory.createProjection(KrintShpeckPec.class, message.getIdPec());
    }          
    
    
}
