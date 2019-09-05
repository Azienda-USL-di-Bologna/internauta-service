package it.bologna.ausl.internauta.service.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.exceptions.intimus.IntimusSendCommandException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.schedulers.MessageSenderManager;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
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
    CachedEntities cachedEntities;
    
    @Autowired
    @Qualifier(value = "redisIntimus")
    private RedisTemplate redisIntimusTemplate; 
    
    public enum IntimusCommandNames {
        RefreshAttivita, ShowMessage
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
        private String idPersona;
        
        @JsonProperty("id_aziende")
        private Integer[] idAziende;
        
        @JsonProperty("apps")
        private String apps[];

        public DestObject(String idPersona, Integer[] idAziende, String[] apps) {
            this.idPersona = idPersona;
            this.idAziende = idAziende;
            this.apps = apps;
        }

        public String getIdPersona() {
            return idPersona;
        }

        public void setIdPersona(String idPersona) {
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
                            return true;
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

        private String title;
        private String body;
        private AmministrazioneMessaggio.SeveritaEnum severita;

        public ShowMessageParams(String title, String body, AmministrazioneMessaggio.SeveritaEnum severita) {
            this.title = title;
            this.body = body;
            this.severita = severita;
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

        public AmministrazioneMessaggio.SeveritaEnum getSeverita() {
            return severita;
        }

        public void setSeverita(AmministrazioneMessaggio.SeveritaEnum severita) {
            this.severita = severita;
        }
    }

    public IntimusCommand buildIntimusCommand(List<DestObject> dests, ShowMessageParams params, IntimusCommandNames intimusCommandName) {
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
                    Struttura struttura = cachedEntities.getStruttura(idStruttura);
                    for (UtenteStruttura utenteStruttura: struttura.getUtenteStrutturaList()) {
                        String idPersona = String.valueOf(cachedEntities.getPersonaFromIdUtente(utenteStruttura.getIdUtente().getId()).getId());
                        Integer[] idAziende = new Integer[] {cachedEntities.getAziendaFromIdUtente(utenteStruttura.getIdUtente().getId()).getId()};
                        DestObject destObject = new DestObject(idPersona, idAziende, apps);
                        addDestIfNotExist(dests, destObject);
                    }
                }
            }
            if (amministrazioneMessaggio.getIdAziende()!= null && amministrazioneMessaggio.getIdAziende().length > 0) {
                Integer[] idAziende = amministrazioneMessaggio.getIdAziende(); 
                DestObject destObject = new DestObject(null, idAziende, apps);
                addDestIfNotExist(dests, destObject);
            }
            if (amministrazioneMessaggio.getIdUtenti() != null && amministrazioneMessaggio.getIdUtenti().length > 0) {
                for (Integer idUtente : amministrazioneMessaggio.getIdUtenti()) {
                    String idPersona = String.valueOf(cachedEntities.getPersonaFromIdUtente(idUtente).getId());
                    Integer[] idAziende = new Integer[] {cachedEntities.getAziendaFromIdUtente(idUtente).getId()};                    
                    DestObject destObject = new DestObject(idPersona, idAziende, apps);
                    addDestIfNotExist(dests, destObject);
                }
            }
            
        }
        
        ShowMessageParams params = new ShowMessageParams("Attenzione", amministrazioneMessaggio.getTesto(), amministrazioneMessaggio.getSeverita());
        return buildIntimusCommand(dests, params, IntimusCommandNames.ShowMessage);
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
