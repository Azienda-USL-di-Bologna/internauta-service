package it.bologna.ausl.internauta.service.schedulers.workers.logoutmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.intimus.IntimusSendCommandException;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import it.bologna.ausl.internauta.service.utils.MasterChefUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author gdm
 */
@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LogoutManagerWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LogoutManagerWorker.class);
    
    @Autowired
    @Qualifier(value = "redisSharedData")
    private RedisTemplate redisTemplate;
//    
//    @Value("${intimus.redis.connected-clients-hash-name}")
//    private String connectedClientsHashName;
    
    @Value("${security.refresh-session.timeout-seconds:1800}")
    private Integer refreshSessionTimeoutSeconds;
    
    @Value("${security.refresh-session.connected-client-redis-hash-name:ConnectedClients}")
    private String connectedClientRedisHashName;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    CachedEntities cachedEntities;
    
    @Autowired
    MasterChefUtils masterChefUtils;
    
    @Autowired
    IntimusUtils intimusUtils;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void run() {
        ZonedDateTime now = ZonedDateTime.now();
        log.info(String.format("now %s", now.toString()));
//        log.info(String.format("refreshSessionTimeoutSeconds:  %d", refreshSessionTimeoutSeconds));
        Set keys = redisTemplate.opsForHash().keys(connectedClientRedisHashName);
        keys.forEach(
            cf -> {
                try {
                    log.info(String.format("checking:  %s", cf));
                    // ConnectedPersona connectedPersonaEntry = objectMapper.readValue((String)redisTemplate.opsForHash().get(connectedClientRedisHashName, cf), ConnectedPersona.class);
                    ConnectedPersona connectedPersonaEntry = (ConnectedPersona) redisTemplate.opsForHash().get(connectedClientRedisHashName, cf);
                    log.info(String.format("lastSeen:  %s", connectedPersonaEntry.getLastSeen().toString()));
//                    Duration.between(connectedPersonaEntry.getLastSeen(), now).toSeconds();
                    boolean timedOut = connectedPersonaEntry.getLastSeen().plusSeconds(refreshSessionTimeoutSeconds).isBefore(now);
                    if (timedOut) {
                        log.info(String.format("timed out", cf));
                        redisTemplate.opsForHash().delete(connectedClientRedisHashName, cf);
                        Persona persona = cachedEntities.getPersona(connectedPersonaEntry.getId());
                        sendLogoutCommand(persona, connectedPersonaEntry.getFromUrl());
                    }
                } catch (Exception ex) {
                    log.error(String.format("errore nell'invio del comando di logout alla persona: %s", cf), ex);
                }
        });
    }
    
    /**
     * invia il comando di logout a tutte le applicazioni di tutte le anziende della persona passata
     * @param persona
     * @param redirectUrl l'url sul quale le applicazioni saranno redirette dopo il logout
     * @throws IOException
     * @throws IntimusSendCommandException 
     */
    public void sendLogoutCommand(Persona persona, String redirectUrl) throws IOException, IntimusSendCommandException {
        
        log.info(String.format("sending logout command to: %s", persona.getCodiceFiscale()));
        List<Azienda> aziendeLogout = userInfoService.getAziendePersona(persona);
        List<String> dests = Arrays.asList(persona.getCodiceFiscale());
        
        // per le vecchie applicazioni ciclo su tutte le aziende e invio il comando primus di logout ai masterchef delle varie aziende
        Map<String, Object> primusCommandParams = new HashMap();
        primusCommandParams.put("redirectUrl", redirectUrl);
        for (Azienda azienda: aziendeLogout) {
            AziendaParametriJson aziendaParametriJson = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
            AziendaParametriJson.MasterChefParmas masterchefParams = aziendaParametriJson.getMasterchefParams();
            MasterChefUtils.MasterchefJobDescriptor masterchefJobDescriptor = masterChefUtils.buildPrimusMasterchefJob(
                    MasterChefUtils.PrimusCommands.logout, primusCommandParams, "1", "1", dests, "*");
            masterChefUtils.sendMasterChefJob(masterchefJobDescriptor, masterchefParams);
        }
        
        // invio il comando anche a intimus per le applicazioni internauta (passando null come secondo parametro, lo mando a tutte le applicazioni)
        intimusUtils.sendCommand(intimusUtils.buildLogoutCommand(persona, null, redirectUrl));
    }
    
    public void addOrRefreshPersona(Persona persona, String fromUrl) {
        redisTemplate.opsForHash().put(connectedClientRedisHashName, persona.getCodiceFiscale(), new ConnectedPersona(persona.getId(), ZonedDateTime.now(), fromUrl));
    }

}
    

