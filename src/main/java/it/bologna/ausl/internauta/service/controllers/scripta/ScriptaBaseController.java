package it.bologna.ausl.internauta.service.controllers.scripta;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
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
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.QMezzo;
import it.bologna.ausl.model.entities.scripta.QRelated;
import it.bologna.ausl.model.entities.scripta.QSmistamento;
import it.bologna.ausl.model.entities.scripta.QSpedizione;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Smistamento;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Session;

@RestController
@RequestMapping(value = "${scripta.mapping.url.root}")
public class ScriptaBaseController extends BaseCrudController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptaBaseController.class);

    @Autowired
    private RestControllerEngineImpl restControllerEngine;
    
    @PersistenceContext
    private EntityManager entityManager;
    

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }

    @RequestMapping(value = {"doc", "doc/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> doc(
            @QuerydslPredicate(root = Doc.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

//        Session session = entityManager.unwrap(Session.class);
//        session.enableFilter("mittenti");
//        session.enableFilter("destinatari");
//        List<Related> a = new ArrayList();
//        a.stream().filter(r -> r.getTipo() == Related.TipoRelated.MITTENTE);
        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QDoc.doc, Doc.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"mezzo", "mezzo/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mezzo(
            @QuerydslPredicate(root = Mezzo.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMezzo.mezzo, Mezzo.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"related", "related/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> related(
            @QuerydslPredicate(root = Related.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QRelated.related, Related.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"smistamento", "smistamento/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> smistamento(
            @QuerydslPredicate(root = Smistamento.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QSmistamento.smistamento, Smistamento.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"spedizione", "spedizione/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> spedizione(
            @QuerydslPredicate(root = Spedizione.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QSpedizione.spedizione, Spedizione.class);
        return ResponseEntity.ok(resource);
    }
}
