package it.bologna.ausl.internauta.service.controllers.rubrica;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.Indirizzo;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.bologna.ausl.model.entities.rubrica.QEmail;
import it.bologna.ausl.model.entities.rubrica.QIndirizzo;
import it.bologna.ausl.model.entities.rubrica.QTelefono;
import it.bologna.ausl.model.entities.rubrica.Telefono;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${rubrica.mapping.url.root}")
public class RubricaBaseController extends BaseCrudController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RubricaBaseController.class);
    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }

    @RequestMapping(value = {"contatto", "contatto/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> contatto(
            @QuerydslPredicate(root = Contatto.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QContatto.contatto, Contatto.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"email", "email/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> email(
            @QuerydslPredicate(root = Email.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QEmail.email1, Email.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"indirizzo", "indirizzo/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> indirizzo(
            @QuerydslPredicate(root = Indirizzo.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QIndirizzo.indirizzo, Indirizzo.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"telefono", "telefono/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> telefono(
            @QuerydslPredicate(root = Telefono.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QTelefono.telefono, Telefono.class);
        return ResponseEntity.ok(resource);
    }
}
