package it.bologna.ausl.internauta.service.controllers.lotti;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.model.entities.lotti.GruppoLotto;
import it.bologna.ausl.model.entities.lotti.Lotto;
import it.bologna.ausl.model.entities.lotti.Componente;
import it.bologna.ausl.model.entities.lotti.Contraente;
import it.bologna.ausl.model.entities.lotti.QGruppoLotto;
import it.bologna.ausl.model.entities.lotti.QLotto;
import it.bologna.ausl.model.entities.lotti.QComponente;
import it.bologna.ausl.model.entities.lotti.QContraente;
import it.bologna.ausl.model.entities.lotti.QRuoloComponente;
import it.bologna.ausl.model.entities.lotti.QTipologia;
import it.bologna.ausl.model.entities.lotti.RuoloComponente;
import it.bologna.ausl.model.entities.lotti.Tipologia;
import it.nextsw.common.controller.BaseCrudController;
import it.nextsw.common.controller.RestControllerEngine;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import javax.servlet.http.HttpServletRequest;
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
 * @author QB
 */
@RestController
@RequestMapping(value = "${lotti.mapping.url.root}")
public class LottiBaseController extends BaseCrudController {
    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }
    
    @RequestMapping(value = {"partecipante", "partecipante/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> partecipante(
            @QuerydslPredicate(root = Componente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QComponente.componente, Componente.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"gruppolotto", "gruppolotto/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> gruppo(
            @QuerydslPredicate(root = GruppoLotto.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QGruppoLotto.gruppoLotto, GruppoLotto.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"lotto", "lotto/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> lotto(
            @QuerydslPredicate(root = Lotto.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QLotto.lotto1, Lotto.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"ruolocomponente", "ruolocomponente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ruolocomponente(
            @QuerydslPredicate(root = Lotto.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QRuoloComponente.ruoloComponente, RuoloComponente.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"contraente", "contraente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> contraente(
            @QuerydslPredicate(root = Lotto.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QContraente.contraente, Contraente.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"tipologia", "tipologia/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> tipologia(
            @QuerydslPredicate(root = Lotto.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QTipologia.tipologia, Tipologia.class);
        return ResponseEntity.ok(resource);
    }
}
