package it.bologna.ausl.internauta.service.controllers.messaggero;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.internauta.service.controllers.baborg.BaborgBaseController;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QAmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QTemplateMessaggio;
import it.bologna.ausl.model.entities.messaggero.TemplateMessaggio;
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

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@RestController
@RequestMapping(value = "${messaggero.mapping.url.root}")
public class MessaggeroBaseController extends BaseCrudController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BaborgBaseController.class);

    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }    
    
    @RequestMapping(value = {"amministrazionemessaggio", "amministrazionemessaggio/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> amministrazionemessaggio(
            @QuerydslPredicate(root = AmministrazioneMessaggio.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAmministrazioneMessaggio.amministrazioneMessaggio, AmministrazioneMessaggio.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"templatemessaggio", "templatemessaggio/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> templatemessaggio(
            @QuerydslPredicate(root = TemplateMessaggio.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QTemplateMessaggio.templateMessaggio, TemplateMessaggio.class);
        return ResponseEntity.ok(resource);
    }
}
