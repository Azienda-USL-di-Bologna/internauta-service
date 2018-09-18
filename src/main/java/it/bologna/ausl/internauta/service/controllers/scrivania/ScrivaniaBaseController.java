package it.bologna.ausl.internauta.service.controllers.scrivania;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.configuration.QApplicazione;
import it.bologna.ausl.model.entities.configuration.QParametroAziende;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.AttivitaFatta;
import it.bologna.ausl.model.entities.scrivania.QAttivita;
import it.bologna.ausl.model.entities.scrivania.QAttivitaFatta;
import it.nextsw.common.controller.BaseCrudController;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping(value = "${scrivania.mapping.url.root}")
public class ScrivaniaBaseController extends BaseCrudController {

    private static final Logger log = LoggerFactory.getLogger(ScrivaniaBaseController.class);

    @Value("${scrivania.mapping.url.root}")
    private String baseUrl;

    @Override
    public String getBaseUrl() {
        return this.baseUrl;
    }

    /*
     * ATTIVITA'
     *
     */
    @RequestMapping(value = {"attivita", "attivita/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Error.class)
    public ResponseEntity<?> attivita(
            @QuerydslPredicate(root = Attivita.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QAttivita.attivita, Attivita.class);
        return ResponseEntity.ok(resource);
    }

    /*
     *
     * ATTIVITA' FATTA
     *
     */
    @RequestMapping(value = {"attivitafatta", "attivitafatta/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Error.class)
    public ResponseEntity<?> attivitafatta(
            @QuerydslPredicate(root = AttivitaFatta.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QAttivitaFatta.attivitaFatta, AttivitaFatta.class);
        return ResponseEntity.ok(resource);
    }

    /*
     *
     * PARAMETRO AZIENDE
     *
     */
    @RequestMapping(value = {"parametroaziende", "parametroaziende/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Error.class)
    public ResponseEntity<?> parametroaziende(
            @QuerydslPredicate(root = ParametroAziende.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QParametroAziende.parametroAziende, ParametroAziende.class);
        return ResponseEntity.ok(resource);
    }

    /*
     *
     * APPLICAZIONE
     *
     */
    @RequestMapping(value = {"applicazione", "applicazione/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Error.class)
    public ResponseEntity<?> applicazione(
            @QuerydslPredicate(root = Applicazione.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException {

        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QApplicazione.applicazione, Applicazione.class);
        return ResponseEntity.ok(resource);
    }

}
