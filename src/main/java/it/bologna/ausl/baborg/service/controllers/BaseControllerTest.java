//package it.bologna.ausl.baborg.service.controllers;
//
//import com.querydsl.core.types.Predicate;
//import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
//import it.bologna.ausl.model.entities.baborg.QAzienda;
//import it.bologna.ausl.model.entities.baborg.QIdpEntityId;
//import it.bologna.ausl.model.entities.baborg.QPec;
//import it.bologna.ausl.model.entities.baborg.QPecProvider;
//import it.bologna.ausl.model.entities.baborg.QPecStruttura;
//import it.bologna.ausl.model.entities.baborg.QPecUtente;
//import it.bologna.ausl.model.entities.baborg.QPermesso;
//import it.bologna.ausl.model.entities.baborg.QPersona;
//import it.bologna.ausl.model.entities.baborg.QRuolo;
//import it.bologna.ausl.model.entities.baborg.QStruttura;
//import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
//import it.bologna.ausl.model.entities.baborg.QTipoPermesso;
//import it.bologna.ausl.model.entities.baborg.QUtente;
//import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
//import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
//import it.bologna.ausl.model.entities.baborg.Azienda;
//import it.bologna.ausl.model.entities.baborg.IdpEntityId;
//import it.bologna.ausl.model.entities.baborg.Pec;
//import it.bologna.ausl.model.entities.baborg.PecProvider;
//import it.bologna.ausl.model.entities.baborg.PecStruttura;
//import it.bologna.ausl.model.entities.baborg.PecUtente;
//import it.bologna.ausl.model.entities.baborg.Permesso;
//import it.bologna.ausl.model.entities.baborg.Persona;
//import it.bologna.ausl.model.entities.baborg.Ruolo;
//import it.bologna.ausl.model.entities.baborg.Struttura;
//import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
//import it.bologna.ausl.model.entities.baborg.TipoPermesso;
//import it.bologna.ausl.model.entities.baborg.Utente;
//import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
//import it.bologna.ausl.model.entities.configuration.Applicazione;
//import it.bologna.ausl.model.entities.configuration.QApplicazione;
//import it.bologna.ausl.model.entities.scrivania.Attivita;
//import it.bologna.ausl.model.entities.scrivania.QAttivita;
//
//import it.nextsw.common.controller.RestControllerEngine;
//import it.nextsw.common.controller.exceptions.RestControllerEngineException;
//import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
//import it.nextsw.common.utils.exceptions.EntityReflectionException;
//import java.util.Map;
//import javax.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.querydsl.binding.QuerydslPredicate;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping(value = "${custom.mapping.url.root}")
//public class BaseControllerTest extends RestControllerEngine {
//
//    private static final Logger log = LoggerFactory.getLogger(RestController.class);
//
//    @RequestMapping(value = {"attivita", "attivita/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = Error.class)
//    public ResponseEntity<?> attivita(
//            @QuerydslPredicate(root = Attivita.class) Predicate predicate,
//            Pageable pageable,
//            @RequestParam(required = false) String projection,
//            @PathVariable(required = false) Integer id,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException {
//
//        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QAttivita.attivita, Attivita.class);
//        return ResponseEntity.ok(resource);
//    }
//
//    @RequestMapping(value = {"attivita"}, method = {RequestMethod.POST, RequestMethod.PUT})
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> attivita(
//            @RequestBody Map<String, Object> data,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
//        Object entity = null;
//        try {
//            entity = insert(data, Attivita.class, request, additionalData);
//        } catch (RollBackInterceptorException ex) {
//            log.error("insert error", ex);
//        }
//        return new ResponseEntity(entity, HttpStatus.CREATED);
//    }
//
//    @RequestMapping(value = {"attivita/{id}"}, method = RequestMethod.PATCH)
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> attivita(
//            @PathVariable(required = true) Integer id,
//            @RequestBody Map<String, Object> data,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
//
//        // carichiamo l'entità e poi dobbiamo fare il merge
//        Object entity = get(id, request);
//        if (entity != null) {
//            Object update = update(id, entity, data, request, additionalData);
//            return new ResponseEntity(update, HttpStatus.OK);
//        }
//        return new ResponseEntity(HttpStatus.NOT_FOUND);
//    }
//
//    @RequestMapping(value = {"attivita/{id}"}, method = RequestMethod.DELETE)
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> attivita(
//            @PathVariable(required = true) Integer id,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {
//
//        Object entity = get(id, request);
//        if (entity != null) {
//            delete(entity, request, additionalData);
//            return new ResponseEntity(HttpStatus.OK);
//        }
//        return new ResponseEntity(HttpStatus.NOT_FOUND);
//    }
//
//    @RequestMapping(value = {"applicazione", "applicazione/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> applicazione(
//            /**
//             * il predicate è la rappresentazione dei filtri passati. In questo
//             * modo si va a reperire il repository e in base al metodo customize
//             * viene elaborato il filtro utilizzando l'oggetto Q
//             */
//            @QuerydslPredicate(root = Applicazione.class) Predicate predicate,
//            // se non si passano in automatico avrà valore null
//            Pageable pageable,
//            // nome della projection da usare; non obbligatoria perchè c'è comunque quella di default
//            @RequestParam(required = false) String projection,
//            // campo racchiuso tra {}
//            @PathVariable(required = false) String id,
//            // richiesta http
//            HttpServletRequest request,
//            /**
//             * stringa ma deve essere rappresentata come
//             * key=value,key=value,ecc...
//             * esempio...&additionalData=key1=value1,key2=value2
//             */
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {
//
//        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QApplicazione.applicazione, Applicazione.class);
//        return ResponseEntity.ok(resource);
//    }
//
//    @RequestMapping(value = {"applicazione"}, method = {RequestMethod.POST, RequestMethod.PUT})
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> applicazione(
//            @RequestBody Map<String, Object> data,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
//        Object entity = null;
//        try {
//            entity = insert(data, Applicazione.class, request, additionalData);
//        } catch (RollBackInterceptorException ex) {
//            log.error("isert error", ex);
//        }
//        return new ResponseEntity(entity, HttpStatus.CREATED);
//    }
//
//    @RequestMapping(value = {"applicazione/{id}"}, method = RequestMethod.PATCH)
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> applicazione(
//            @PathVariable(required = true) String id,
//            @RequestBody Map<String, Object> data,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
//
//        Object entity = get(id, request);
//        if (entity != null) {
//            Object update = update(id, entity, data, request, additionalData);
//            return new ResponseEntity(update, HttpStatus.OK);
//        }
//        return new ResponseEntity(HttpStatus.NOT_FOUND);
//    }
//
//    @RequestMapping(value = {"applicazione/{id}"}, method = RequestMethod.DELETE)
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> applicazione(
//            @PathVariable(required = true) String id,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {
//
//        Object entity = get(id, request);
//        if (entity != null) {
//            delete(entity, request, additionalData);
//            return new ResponseEntity(HttpStatus.OK);
//        }
//        return new ResponseEntity(HttpStatus.NOT_FOUND);
//    }
//}
