package it.bologna.ausl.internauta.service.controllers.scrivania;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.AttivitaFatta;
import it.bologna.ausl.model.entities.scrivania.Menu;
import it.bologna.ausl.model.entities.scrivania.QAttivita;
import it.bologna.ausl.model.entities.scrivania.QAttivitaFatta;
import it.bologna.ausl.model.entities.scrivania.QMenu;
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
@RequestMapping(value = "${scrivania.mapping.url.root}")
public class ScrivaniaBaseController extends BaseCrudController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrivaniaBaseController.class);
    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }

    @RequestMapping(value = {"attivita", "attivita/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> attivita(
            @QuerydslPredicate(root = Attivita.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAttivita.attivita, Attivita.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"attivitafatta", "attivitafatta/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> attivitafatta(
            @QuerydslPredicate(root = AttivitaFatta.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAttivitaFatta.attivitaFatta, AttivitaFatta.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"menu", "menu/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> menu(
            @QuerydslPredicate(root = Menu.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMenu.menu, Menu.class);
        return ResponseEntity.ok(resource);
    }
}
