package it.bologna.ausl.baborg.service.controllers;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.baborg.model.entities.QAfferenzaStruttura;
import it.bologna.ausl.baborg.model.entities.QAzienda;
import it.bologna.ausl.baborg.model.entities.QIdpEntityId;
import it.bologna.ausl.baborg.model.entities.QPec;
import it.bologna.ausl.baborg.model.entities.QPecProvider;
import it.bologna.ausl.baborg.model.entities.QPecStruttura;
import it.bologna.ausl.baborg.model.entities.QPecUtente;
import it.bologna.ausl.baborg.model.entities.QPermesso;
import it.bologna.ausl.baborg.model.entities.QPersona;
import it.bologna.ausl.baborg.model.entities.QRuolo;
import it.bologna.ausl.baborg.model.entities.QStruttura;
import it.bologna.ausl.baborg.model.entities.QStrutturaUnificata;
import it.bologna.ausl.baborg.model.entities.QTipoPermesso;
import it.bologna.ausl.baborg.model.entities.QUtente;
import it.bologna.ausl.baborg.model.entities.QUtenteStruttura;
import it.bologna.ausl.baborg.model.entities.AfferenzaStruttura;
import it.bologna.ausl.baborg.model.entities.Azienda;
import it.bologna.ausl.baborg.model.entities.IdpEntityId;
import it.bologna.ausl.baborg.model.entities.Pec;
import it.bologna.ausl.baborg.model.entities.PecProvider;
import it.bologna.ausl.baborg.model.entities.PecStruttura;
import it.bologna.ausl.baborg.model.entities.PecUtente;
import it.bologna.ausl.baborg.model.entities.Permesso;
import it.bologna.ausl.baborg.model.entities.Persona;
import it.bologna.ausl.baborg.model.entities.Ruolo;
import it.bologna.ausl.baborg.model.entities.Struttura;
import it.bologna.ausl.baborg.model.entities.StrutturaUnificata;
import it.bologna.ausl.baborg.model.entities.TipoPermesso;
import it.bologna.ausl.baborg.model.entities.Utente;
import it.bologna.ausl.baborg.model.entities.UtenteStruttura;

import it.nextsw.common.controller.RestControllerEngine;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${custom.mapping.url.root}")
public class CustomRestController extends RestControllerEngine {

    private static final Logger log = LoggerFactory.getLogger(CustomRestController.class);

    @RequestMapping(value = {"afferenzastruttura", "afferenzastruttura/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Error.class)
    public ResponseEntity<?> afferenzastruttura(
            @QuerydslPredicate(root = AfferenzaStruttura.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QAfferenzaStruttura.afferenzaStruttura, AfferenzaStruttura.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"afferenzastruttura"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> afferenzastruttura(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, AfferenzaStruttura.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("insert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"afferenzastruttura/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> afferenzastruttura(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        // carichiamo l'entità e poi dobbiamo fare il merge
        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"afferenzastruttura/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> afferenzastruttura(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    // gestione Azienda
    @RequestMapping(value = {"azienda", "azienda/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> azienda(
            @QuerydslPredicate(root = Azienda.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QAzienda.azienda, Azienda.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"azienda"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> azienda(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, Azienda.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"azienda/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> azienda(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"azienda/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> azienda(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"idpentityid", "idpentityid/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> idpentityid(
            @QuerydslPredicate(root = IdpEntityId.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QIdpEntityId.idpEntityId, IdpEntityId.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"idpentityid"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> idpentityid(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, IdpEntityId.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"idpentityid/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> idpentityid(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"idpentityid/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> idpentityid(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"pec", "pec/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> Pec(
            @QuerydslPredicate(root = Pec.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QPec.pec, Pec.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"pec"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pec(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, Pec.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"pec/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pec(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"pec/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pec(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"pecprovider", "pecprovider/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pecprovider(
            @QuerydslPredicate(root = PecProvider.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QPecProvider.pecProvider, PecProvider.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"pecprovider"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecprovider(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, PecProvider.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"pecprovider/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecprovider(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"pecprovider/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecprovider(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"pecstruttura", "pecstruttura/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pecstruttura(
            @QuerydslPredicate(root = PecStruttura.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QPecStruttura.pecStruttura, PecStruttura.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"pecstruttura"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecstruttura(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, PecStruttura.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"pecstruttura/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecstruttura(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"pecstruttura/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecstruttura(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"pecutente", "pecutente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pecutente(
            @QuerydslPredicate(root = PecUtente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QPecUtente.pecUtente, PecUtente.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"pecutente"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecutente(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, PecUtente.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"pecutente/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecutente(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"pecutente/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> pecutente(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"permesso", "permesso/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> permesso(
            @QuerydslPredicate(root = Permesso.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QPermesso.permesso, Permesso.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"permesso"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> permesso(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, Permesso.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"permesso/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> permesso(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"permesso/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> permesso(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"persona", "persona/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> persona(
            @QuerydslPredicate(root = Persona.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QPersona.persona, Persona.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"persona"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> persona(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, Persona.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"persona/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> persona(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"persona/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> persona(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"ruolo", "ruolo/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ruolo(
            @QuerydslPredicate(root = Ruolo.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QRuolo.ruolo, Ruolo.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"ruolo"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> ruolo(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, Ruolo.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"ruolo/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> ruolo(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"ruolo/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> ruolo(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"struttura", "struttura/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> struttura(
            @QuerydslPredicate(root = Struttura.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QStruttura.struttura, Struttura.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"struttura"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> struttura(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, Struttura.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"struttura/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> struttura(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"struttura/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> struttura(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"strutturaunificata", "strutturaunificata/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> strutturaunificata(
            @QuerydslPredicate(root = StrutturaUnificata.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QStrutturaUnificata.strutturaUnificata, StrutturaUnificata.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"strutturaunificata"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> strutturaunificata(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, StrutturaUnificata.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"strutturaunificata/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> strutturaunificata(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"strutturaunificata/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> strutturaunificata(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"tipopermesso", "tipopermesso/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> tipopermesso(
            @QuerydslPredicate(root = TipoPermesso.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QTipoPermesso.tipoPermesso, TipoPermesso.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"tipopermesso"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> tipopermesso(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, TipoPermesso.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"tipopermesso/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> tipopermesso(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"tipopermesso/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> tipopermesso(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"utente", "utente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> utente(
            @QuerydslPredicate(root = Utente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QUtente.utente, Utente.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"utente"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> utente(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, Utente.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("isert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"utente/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> utente(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"utente/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> utente(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"utentestruttura", "utentestruttura/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> utentestruttura(
            @QuerydslPredicate(root = UtenteStruttura.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QUtenteStruttura.utenteStruttura, UtenteStruttura.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"utentestruttura"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> registroAccessi(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        Object entity = null;
        try {
            entity = insert(data, UtenteStruttura.class, request, additionalData);
        } catch (RollBackInterceptorException ex) {
            log.error("insert error", ex);
        }
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"utentestruttura/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> utentestruttura(
            @PathVariable(required = true) Integer id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {

        Object entity = get(id, request);
        if (entity != null) {
            Object update = update(id, entity, data, request, additionalData);
            return new ResponseEntity(update, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = {"utentestruttura/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> utentestruttura(
            @PathVariable(required = true) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, RollBackInterceptorException {

        Object entity = get(id, request);
        if (entity != null) {
            delete(entity, request, additionalData);
            return new ResponseEntity(entity, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }
}
