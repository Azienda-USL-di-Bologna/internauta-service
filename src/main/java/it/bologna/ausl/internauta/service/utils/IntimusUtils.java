package it.bologna.ausl.internauta.service.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.intimus.IntimusSendCommandException;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class IntimusUtils {
    private static final Logger log = LoggerFactory.getLogger(IntimusUtils.class);
    /*
        {
            "dest": [{
                        "id_persona": row["id_persona"],
                        "id_aziende": [],
                        "apps": ["scrivania"]
                    }
            ],
            "command": {
                "params": {
                    "id_attivita": row["id"],
                    "operation": TD["event"]
                },
                "command": "RefreshAttivita"
            }
        }
    */
    
    @Value("${intimus.redis.command-queue-name}")
    private String intimusCommandQueueName;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private StrutturaRepository strutturaRepository;
    
    @Autowired
    private EntityManager em;
    
    @Autowired
    @Qualifier(value = "redisIntimus")
    private RedisTemplate redisIntimusTemplate; 
    
    public enum IntimusCommandNames {
        RefreshAttivita, ShowMessage, Logout, RefreshMails
    }
    
    public class IntimusCommand {
        @JsonProperty("dest")
        private List<DestObject> dest;
        
        @JsonProperty("command")
        private CommandObject command;

        public IntimusCommand(List<DestObject> dest, CommandObject command) {
            this.dest = dest;
            this.command = command;
        }

        public List<DestObject> getDest() {
            return dest;
        }

        public void setDest(List<DestObject> dest) {
            this.dest = dest;
        }

        public CommandObject getCommand() {
            return command;
        }

        public void setCommand(CommandObject command) {
            this.command = command;
        }
        
        @JsonIgnore
        public String buildIntimusStringCommand() throws JsonProcessingException {
            return objectMapper.writeValueAsString(this);
        }
    }
    public class DestObject {
        @JsonProperty("id_persona")
        private Integer idPersona;
        
        @JsonProperty("id_aziende")
        private Integer[] idAziende;
        
        @JsonProperty("apps")
        private String apps[];
        
        @JsonProperty("all_aziende")
        private Boolean allAziende;

        public DestObject(Integer idPersona, Integer[] idAziende, String[] apps, Boolean allAziende) {
            this.idPersona = idPersona;
            this.idAziende = idAziende;
            this.apps = apps;
            this.allAziende = allAziende;
        }

        public Integer getIdPersona() {
            return idPersona;
        }

        public void setIdPersona(Integer idPersona) {
            this.idPersona = idPersona;
        }

        public Integer[] getIdAziende() {
            return idAziende;
        }

        public void setIdAziende(Integer[] idAziende) {
            this.idAziende = idAziende;
        }

        public String[] getApps() {
            return apps;
        }

        public void setApps(String[] apps) {
            this.apps = apps;
        }

        public Boolean getAllAziende() {
            return allAziende;
        }

        public void setAllAziende(Boolean allAziende) {
            this.allAziende = allAziende;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (DestObject.class.isAssignableFrom(obj.getClass())) {
                DestObject target = (DestObject) obj;
                if (target.getIdPersona().equals(getIdPersona())) {
                    HashSet<Integer> setAziende1 = new HashSet<>(Arrays.asList(getIdAziende()));
                    HashSet<Integer> setAziende2 = new HashSet<>(Arrays.asList(target.getIdAziende()));
                    if (setAziende1.equals(setAziende2)) {
                        HashSet<String> setApps1 = new HashSet<>(Arrays.asList(getApps()));
                        HashSet<String> setApps2 = new HashSet<>(Arrays.asList(target.getApps()));
                        if (setApps1.equals(setApps2)) {
                            if (getAllAziende().equals(target.getAllAziende())) {
                                return true;                            
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.idPersona);
            hash = 29 * hash + Arrays.deepHashCode(this.idAziende);
            hash = 29 * hash + Arrays.deepHashCode(this.apps);
            return hash;
        }
    }
    
    public class CommandObject {
        @JsonProperty("params")
        @JsonInclude(value=Include.NON_EMPTY, content=Include.NON_NULL)
        private CommandParams params;

        @JsonProperty("command")
        private IntimusCommandNames command;

        public CommandObject(CommandParams params, IntimusCommandNames command) {
            this.params = params;
            this.command = command;
        }

        public CommandParams getParams() {
            return params;
        }

        public void setParams(CommandParams params) {
            this.params = params;
        }

        public IntimusCommandNames getCommand() {
            return command;
        }

        public void setCommand(IntimusCommandNames command) {
            this.command = command;
        }
    }

    public interface CommandParams{};

    public class ShowMessageParams implements CommandParams {

        private Integer messageId;
        private String title;
        private String body;
        private AmministrazioneMessaggio.SeveritaEnum severity;
        private Integer rescheduleInterval;
        private AmministrazioneMessaggio.TipologiaEnum type;
        private AmministrazioneMessaggio.InvasivitaEnum invasivity;
        private Boolean disabled;

        public ShowMessageParams(Integer messageId, String title, String body, AmministrazioneMessaggio.SeveritaEnum severity, Integer rescheduleInterval, AmministrazioneMessaggio.TipologiaEnum type, AmministrazioneMessaggio.InvasivitaEnum invasivity) {
            this(messageId, title, body, severity, rescheduleInterval, type, invasivity, false);
        }
        
        public ShowMessageParams(Integer messageId, String title, String body, AmministrazioneMessaggio.SeveritaEnum severity, Integer rescheduleInterval, AmministrazioneMessaggio.TipologiaEnum type, AmministrazioneMessaggio.InvasivitaEnum invasivity, Boolean expired) {
            this.messageId = messageId;
            this.title = title;
            this.body = body;
            this.severity = severity;
            this.rescheduleInterval = rescheduleInterval;
            this.type = type;
            this.invasivity = invasivity;
            this.disabled = expired;
        }

        public Integer getMessageId() {
            return messageId;
        }

        public void setMessageId(Integer messageId) {
            this.messageId = messageId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public AmministrazioneMessaggio.SeveritaEnum getSeverity() {
            return severity;
        }

        public void setSeverity(AmministrazioneMessaggio.SeveritaEnum severity) {
            this.severity = severity;
        }

        public Integer getRescheduleInterval() {
            return rescheduleInterval;
        }

        public void setRescheduleInterval(Integer rescheduleInterval) {
            this.rescheduleInterval = rescheduleInterval;
        }

        public AmministrazioneMessaggio.TipologiaEnum getType() {
            return type;
        }

        public void setType(AmministrazioneMessaggio.TipologiaEnum type) {
            this.type = type;
        }

        public AmministrazioneMessaggio.InvasivitaEnum getInvasivity() {
            return invasivity;
        }

        public void setInvasivity(AmministrazioneMessaggio.InvasivitaEnum invasivity) {
            this.invasivity = invasivity;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(Boolean disabled) {
            this.disabled = disabled;
        }
    }

    public class LogoutParams implements CommandParams {
        private String redirectUrl;

        public LogoutParams(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public void setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }
    }

    public class RefreshAttivitaParams implements CommandParams {
        @JsonProperty("id_attivita")
        private Integer idAttivita;
        @JsonProperty("operation")
        private String operation;

        public RefreshAttivitaParams(Integer idAttivita, String operation) {
            this.idAttivita = idAttivita;
            this.operation = operation;
        }

        public Integer getIdAttivita() {
            return idAttivita;
        }

        public void setIdAttivita(Integer idAttivita) {
            this.idAttivita = idAttivita;
        }
        
        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }
    }

    public class RefreshMailsParams implements CommandParams {
        @JsonProperty("entity")
        private String entity;
        @JsonProperty("id")
        private Integer id;
        @JsonProperty("id_utente")
        private Integer id_utente;
        @JsonProperty("persona")
        private String persona;
        @JsonProperty("folder_description")
        private String folder_description;
        @JsonProperty("folder_name")
        private String folder_name;
        @JsonProperty("tag_description")
        private String tag_description;
        @JsonProperty("tag_name")
        private String tag_name;
        @JsonProperty("id_message")
        private Integer id_message;
        @JsonProperty("operation")
        private String operation;

        public RefreshMailsParams(String entity, Integer id, Integer id_utente, String persona, String folder_description, String folder_name, String tag_description, String tag_name, Integer id_message, String operation) {
            this.entity = entity;
            this.id = id;
            this.id_utente = id_utente;
            this.persona = persona;
            this.folder_description = folder_description;
            this.folder_name = folder_name;
            this.tag_description = tag_description;
            this.tag_name = tag_name;
            this.id_message = id_message;
            this.operation = operation;
        }

        public String getEntity() {
            return entity;
        }

        public void setEntity(String entity) {
            this.entity = entity;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getId_utente() {
            return id_utente;
        }

        public void setId_utente(Integer id_utente) {
            this.id_utente = id_utente;
        }

        public String getPersona() {
            return persona;
        }

        public void setPersona(String persona) {
            this.persona = persona;
        }

        public String getFolder_description() {
            return folder_description;
        }

        public void setFolder_description(String folder_description) {
            this.folder_description = folder_description;
        }

        public String getFolder_name() {
            return folder_name;
        }

        public void setFolder_name(String folder_name) {
            this.folder_name = folder_name;
        }

        public String getTag_description() {
            return tag_description;
        }

        public void setTag_description(String tag_description) {
            this.tag_description = tag_description;
        }

        public String getTag_name() {
            return tag_name;
        }

        public void setTag_name(String tag_name) {
            this.tag_name = tag_name;
        }

        public Integer getId_message() {
            return id_message;
        }

        public void setId_message(Integer id_message) {
            this.id_message = id_message;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }
    }
        
    public IntimusCommand buildIntimusCommand(List<DestObject> dests, CommandParams params, IntimusCommandNames intimusCommandName) {
        CommandObject commandObject = new CommandObject(params, intimusCommandName);
        IntimusCommand intimusCommand = new IntimusCommand(dests, commandObject);

        return intimusCommand;
    }
    
    public IntimusCommand buildIntimusShowMessageCommand(AmministrazioneMessaggio amministrazioneMessaggio) throws BlackBoxPermissionException {
        List<DestObject> dests = null;
        if (!amministrazioneMessaggio.getPerTutti()) {
            dests = new ArrayList();
            String[] apps = null;
            if (amministrazioneMessaggio.getIdApplicazioni() != null && amministrazioneMessaggio.getIdApplicazioni().length > 0) {
                apps = amministrazioneMessaggio.getIdApplicazioni();
            }
            if (amministrazioneMessaggio.getIdStrutture() != null && amministrazioneMessaggio.getIdStrutture().length > 0) {
                for (Integer idStruttura: amministrazioneMessaggio.getIdStrutture()) {
                    Struttura struttura = strutturaRepository.findById(idStruttura).get();
//                    Struttura struttura = em.find(Struttura.class, idStruttura);
                    for (UtenteStruttura utenteStruttura: struttura.getUtenteStrutturaList()) {
                        Integer idPersona = userInfoService.getPersonaFromIdUtente(utenteStruttura.getIdUtente().getId()).getId();
                        // Integer[] idAziende = new Integer[] {cachedEntities.getAziendaFromIdUtente(utenteStruttura.getIdUtente().getId()).getId()};
                        DestObject destObject = new DestObject(idPersona, null, apps, true);
                        addDestIfNotExist(dests, destObject);
                    }
                }
            }
            if (amministrazioneMessaggio.getIdAziende()!= null && amministrazioneMessaggio.getIdAziende().length > 0) {
                Integer[] idAziende = amministrazioneMessaggio.getIdAziende(); 
                DestObject destObject = new DestObject(null, idAziende, apps, true);
                addDestIfNotExist(dests, destObject);
            }
            if (amministrazioneMessaggio.getIdPersone() != null && amministrazioneMessaggio.getIdPersone().length > 0) {
                for (Integer idPersona : amministrazioneMessaggio.getIdPersone()) {
                    // Integer idPersona = cachedEntities.getPersona(idPersona).getId();
//                    Integer[] idAziende = new Integer[] {cachedEntities.getAziendaFromIdUtente(idUtente).getId()};                    
                    DestObject destObject = new DestObject(idPersona, null, apps, true);
                    addDestIfNotExist(dests, destObject);
                }
            }
            
        }
        ShowMessageParams params = buildShowMessageParams(amministrazioneMessaggio);
        return buildIntimusCommand(dests, params, IntimusCommandNames.ShowMessage);
    }
    
    public ShowMessageParams buildShowMessageParams(AmministrazioneMessaggio amministrazioneMessaggio) {
        return new ShowMessageParams(
                amministrazioneMessaggio.getId(), 
                amministrazioneMessaggio.getTitolo(),
                amministrazioneMessaggio.getTesto(),
                amministrazioneMessaggio.getSeverita(),
                amministrazioneMessaggio.getIntervallo() * 60,
                amministrazioneMessaggio.getTipologia(),
                amministrazioneMessaggio.getInvasivita());
    }
    
    public IntimusCommand buildLogoutCommand(Persona persona, String[] apps, String redirectUrl) {
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(persona);
        DestObject dest = new DestObject(persona.getId(), aziendePersona.stream().map(p -> p.getId()).toArray(Integer[]::new), apps, false);
        
        IntimusCommand logoutCommand = buildIntimusCommand(Arrays.asList(dest), new LogoutParams(redirectUrl), IntimusCommandNames.Logout);
        
        return logoutCommand;
    }
    
    public IntimusCommand buildRefreshAttivitaCommand(Integer idPersona, Integer idAttivita, String operation) {
        List<Integer> idAziende = new ArrayList<Integer>();
        List<String> apps = new ArrayList<String>();
        
        apps.add("scrivania");
        
        DestObject dest = new DestObject(
                idPersona, 
                idAziende.stream().map(a -> a).toArray(Integer[]::new), 
                apps.stream().map(a -> a).toArray(String[]::new), 
                false);
        
        IntimusCommand refreshAttivitaCommand = buildIntimusCommand(
                Arrays.asList(dest), 
                new RefreshAttivitaParams(idAttivita, operation), 
                IntimusCommandNames.RefreshAttivita);
        
        return refreshAttivitaCommand;
    }
    
    public IntimusCommand buildRefreshMailsCommand(Map<String, Object> params) {
        List<Integer> idAziende = new ArrayList<Integer>();
        List<String> apps = new ArrayList<String>();
        
        apps.add("shpeck");
        
        DestObject dest = new DestObject(
                null, 
                idAziende.stream().map(a -> a).toArray(Integer[]::new), 
                apps.stream().map(a -> a).toArray(String[]::new), 
                false);
        
        RefreshMailsParams paramsObj = new RefreshMailsParams(
                params.get("entity").toString(), 
                (Integer) params.get("id"), 
                (Integer) params.get("id_utente"), 
                params.get("persona").toString(), 
                params.get("folder_description") != null ? params.get("folder_description").toString() : null, 
                params.get("folder_name") != null ? params.get("folder_name").toString() : null, 
                params.get("tag_description") != null ? params.get("tag_description").toString() : null, 
                params.get("tag_name") != null ? params.get("tag_name").toString() : null, 
                (Integer) params.get("id_message"), 
                params.get("operation").toString());
        
        IntimusCommand refreshMailsCommand = buildIntimusCommand(
                Arrays.asList(dest), 
                paramsObj, 
                IntimusCommandNames.RefreshMails);
        
        return refreshMailsCommand;
    }
    
    public void sendCommand(IntimusCommand intimusCommand) throws IntimusSendCommandException {
        try {
            redisIntimusTemplate.opsForList().rightPush(intimusCommandQueueName, intimusCommand.buildIntimusStringCommand());
        } catch (Exception ex) {
           throw new IntimusSendCommandException("errore nell'invio del comando a Intimus", ex);
        }
    }
    
    private boolean addDestIfNotExist(List<DestObject> dests, DestObject dest) {
        if (!dests.stream().anyMatch(d -> d.equals(dest))) {
            return dests.add(dest);
        } else {
            return false;
        }
    }
}
