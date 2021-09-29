package it.bologna.ausl.internauta.service.controllers.configurazione;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ConfigurazioneAmbiente;
import it.bologna.ausl.model.entities.configurazione.DominioFirma;
import it.bologna.ausl.model.entities.configurazione.FirmePersona;
import it.bologna.ausl.model.entities.configurazione.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QApplicazione;
import it.bologna.ausl.model.entities.configurazione.QConfigurazioneAmbiente;
import it.bologna.ausl.model.entities.configurazione.QDominioFirma;
import it.bologna.ausl.model.entities.configurazione.QFirmePersona;
import it.bologna.ausl.model.entities.configurazione.QImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import it.nextsw.common.controller.BaseCrudController;
import it.nextsw.common.controller.RestControllerEngine;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${configurazione.mapping.url.root}")
public class ConfigurazioneBaseController extends BaseCrudController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurazioneBaseController.class);
    
    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }
    @RequestMapping(value = {"parametroaziende", "parametroaziende/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> parametroaziende(
            @QuerydslPredicate(root = ParametroAziende.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QParametroAziende.parametroAziende, ParametroAziende.class);
        return ResponseEntity.ok(resource);
    }

    /*
     *
     * APPLICAZIONE
     *
     */
    @RequestMapping(value = {"applicazione", "applicazione/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> applicazione(
            @QuerydslPredicate(root = Applicazione.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) String id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QApplicazione.applicazione, Applicazione.class);
        return ResponseEntity.ok(resource);
    }

    /*
     *
     * Configurazione Ambiente
     *
     */
    @RequestMapping(value = {"configurazioneambiente", "configurazioneambiente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> configurazioneambiente(
            @QuerydslPredicate(root = ConfigurazioneAmbiente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QConfigurazioneAmbiente.configurazioneAmbiente, ConfigurazioneAmbiente.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"impostazioniapplicazioni", "impostazioniapplicazioni/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> impostazioniapplicazioni(
            @QuerydslPredicate(root = ImpostazioniApplicazioni.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QImpostazioniApplicazioni.impostazioniApplicazioni, ImpostazioniApplicazioni.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"firmepersona", "firmepersona/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> firmepersona(
            @QuerydslPredicate(root = FirmePersona.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QFirmePersona.firmePersona, FirmePersona.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"dominiofirma", "dominiofirma/{codice}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> dominiofirma(
            @QuerydslPredicate(root = DominioFirma.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) String codice,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, codice, projection, predicate, pageable, additionalData, QDominioFirma.dominioFirma, DominioFirma.class);
        return ResponseEntity.ok(resource);
    }
}
