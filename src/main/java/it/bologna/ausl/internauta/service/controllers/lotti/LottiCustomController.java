package it.bologna.ausl.internauta.service.controllers.lotti;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.lotti.LottoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.service.utils.MasterChefUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.lotti.Lotto;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author conte
 */
@RestController
@RequestMapping(value = "${lotti.mapping.url.root}")
public class LottiCustomController {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LottiCustomController.class);
    
    @Autowired
    private MasterChefUtils masterChefUtils;
        
    @Autowired
    private DocRepository docRepository;
    
    @Autowired
    private PersonaRepository personaRepository;
    
    @Autowired
    private LottoRepository lottoRepository;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    /**
     * Controller usato per avvertire PDD utilizzando primus che il pop-up di lotti-list è stato chiuso
     * @param guid il guid del Doc relativo ai lotti
     * @return
     * @throws ClassNotFoundException
     * @throws EntityReflectionException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws RestControllerEngineException
     * @throws AbortLoadInterceptorException
     * @throws BlackBoxPermissionException
     */
    @RequestMapping(value = {"refreshLotti"}, method = RequestMethod.GET)
    public ResponseEntity<?> refreshLotti(@RequestParam(name = "guid", required = true) String guid) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException, BlackBoxPermissionException {
        // recupero l'utente e la persona della sessione
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        Persona persona = personaRepository.findById(utente.getIdPersona().getId()).get();
        
        // ottengo il doc e in caso non esista ritorno errore 500
        Doc docFindByIdEsterno = docRepository.findByIdEsterno(guid);
        if (docFindByIdEsterno == null)
            return new ResponseEntity<>("Documento non trovato", HttpStatus.INTERNAL_SERVER_ERROR);
        
        // preparo e lancio doRefreshLotti
        Azienda idAzienda = docFindByIdEsterno.getIdAzienda();
        List<String> cfPersoneDiCuiAggiornareLaVideataList = new ArrayList<>();
        cfPersoneDiCuiAggiornareLaVideataList.add(persona.getCodiceFiscale());
        cfPersoneDiCuiAggiornareLaVideataList.add(persona.getCodiceFiscale());
        try {
            log.info("refreshLotti");
            doRefreshLotti(cfPersoneDiCuiAggiornareLaVideataList, guid, idAzienda);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(LottiCustomController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>("Refresh Lotti non avvenuto", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(null);
    }
    
    /**
     * Controller usato per sapere se un certo doc (identificato da un guid) ha o no dei lotti, e se necessita di una nuova stampa dato il ts dell'ultima stampa
     * @param guid il guid del Doc da prendere in esame
     * @param ts il timestamp dell'ultima stampa se presente
     * @return ritorna "false" se non sono stati trovati lotti, "true" se ci sono, "stampa" se ci sono ed è necessario produrre una nuova stampa
     * @throws ClassNotFoundException
     * @throws EntityReflectionException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws RestControllerEngineException
     * @throws AbortLoadInterceptorException
     * @throws BlackBoxPermissionException
     */
    @RequestMapping(value = {"hasLottiByGuid"}, method = RequestMethod.GET)
    public ResponseEntity<?> hasLottiByGuid(
            @RequestParam(name = "guid", required = true) String guid,
            @RequestParam(name = "ts", required = false) String ts) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException, BlackBoxPermissionException {
        Doc doc = docRepository.findByIdEsterno(guid);
        if (doc == null)
            return new ResponseEntity<>("Documento non trovato", HttpStatus.INTERNAL_SERVER_ERROR);        

        List<Lotto> lottiList = doc.getLottiList();
        if (lottiList.isEmpty()) {
            // se non ho lotti rispondo false a prescindere dal resto
            return ResponseEntity.ok(false);
        }
        
        // se ho dei lotti ma non ho un ts utlima stampa dico di produrne una
        if (ts == null) {
            return ResponseEntity.ok("stampa");
        }
        
        // altrimenti la produco solo se ho dei lotti successivi alla data di creazione dell'ultima stampa
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        ZonedDateTime tsUltimaStampa = LocalDateTime.parse(ts, format).atZone(ZoneId.systemDefault());
        for(Lotto lotto : lottiList) {
            if (lotto.getVersion().isAfter(tsUltimaStampa)) {
                return ResponseEntity.ok("stampa");
            }
        }
        
        // se arrivo qui non devo produrre una stampa ma con i lotti sto ok
        return ResponseEntity.ok(true);
    }
    
    
    private void doRefreshLotti(List<String> cfPersoneDiCuiAggiornareLaVideataList, String guid, Azienda azienda) throws JsonProcessingException {     
        // preparo il comando di primus lo mando a masterChef
        Map<String, Object> primusCommandParams = new HashMap();
        primusCommandParams.put("refreshLotti", guid);
        AziendaParametriJson aziendaParametriJson = azienda.getParametri();
        AziendaParametriJson.MasterChefParmas masterchefParams = aziendaParametriJson.getMasterchefParams();
        MasterChefUtils.MasterchefJobDescriptor masterchefJobDescriptor = masterChefUtils.buildPrimusMasterchefJob(
                        MasterChefUtils.PrimusCommands.refreshLotti,
                        primusCommandParams, 
                        "1", 
                        "1", 
                        cfPersoneDiCuiAggiornareLaVideataList, 
                        "*"
                );
        masterChefUtils.sendMasterChefJob(masterchefJobDescriptor, masterchefParams);
    }
}
