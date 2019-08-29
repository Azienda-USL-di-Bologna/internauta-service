package it.bologna.ausl.internauta.service.controllers.baborg;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.internauta.service.repositories.baborg.Gdm1Repository;
import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.QIdpEntityId;
import it.bologna.ausl.model.entities.baborg.QPec;
import it.bologna.ausl.model.entities.baborg.QPecProvider;
import it.bologna.ausl.model.entities.baborg.QPecStruttura;
import it.bologna.ausl.model.entities.baborg.QPecUtente;
import it.bologna.ausl.model.entities.baborg.QPermesso;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QRuolo;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.QTipoPermesso;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Gdm1;
import it.bologna.ausl.model.entities.baborg.Gdm2;
import it.bologna.ausl.model.entities.baborg.IdpEntityId;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.PecProvider;
import it.bologna.ausl.model.entities.baborg.PecStruttura;
import it.bologna.ausl.model.entities.baborg.PecUtente;
import it.bologna.ausl.model.entities.baborg.Permesso;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QGdm1;
import it.bologna.ausl.model.entities.baborg.QGdm2;
import it.bologna.ausl.model.entities.baborg.QPecAzienda;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.TipoPermesso;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.Gdm1WithPlainFields;
import it.nextsw.common.controller.BaseCrudController;
import it.nextsw.common.controller.RestControllerEngine;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${baborg.mapping.url.root}")
public class BaborgBaseController extends BaseCrudController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaborgBaseController.class);

    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }
    
    /* 
    // GDM: non cancellare, mi serve a volte per fare delle prove
    @Autowired
    private Gdm1Repository gdm1Repository;
    @Autowired
    protected ProjectionFactory factory;
    @Autowired
    private EntityManager em;
    
    @RequestMapping(value = {"gdm1", "gdm1/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> gdm1(
            @QuerydslPredicate(root = Gdm1.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QGdm1.gdm1, Gdm1.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"gdm2", "gdm2/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> gdm2(
            @QuerydslPredicate(root = Gdm2.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QGdm2.gdm2, Gdm2.class);
        return ResponseEntity.ok(resource);
    }
    */

    @RequestMapping(value = {"afferenzastruttura", "afferenzastruttura/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = Error.class)
    public ResponseEntity<?> afferenzastruttura(
            @QuerydslPredicate(root = AfferenzaStruttura.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAfferenzaStruttura.afferenzaStruttura, AfferenzaStruttura.class);
        return ResponseEntity.ok(resource);
    }

    // gestione Azienda
    @RequestMapping(value = {"azienda", "azienda/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> azienda(
            /**
             * il predicate è la rappresentazione dei filtri passati. In questo
             * modo si va a reperire il repository e in base al metodo customize
             * viene elaborato il filtro utilizzando l'oggetto Q
             */
            @QuerydslPredicate(root = Azienda.class) Predicate predicate,
            // se non si passano in automatico avrà valore null
            Pageable pageable,
            // nome della projection da usare; non obbligatoria perchè c'è comunque quella di default
            @RequestParam(required = false) String projection,
            // campo racchiuso tra {}
            @PathVariable(required = false) Integer id,
            // richiesta http
            HttpServletRequest request,
            /**
             * stringa ma deve essere rappresentata come
             * key=value,key=value,ecc...
             * esempio...&additionalData=key1=value1,key2=value2
             */
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAzienda.azienda, Azienda.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"idpentityid", "idpentityid/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> idpentityid(
            @QuerydslPredicate(root = IdpEntityId.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QIdpEntityId.idpEntityId, IdpEntityId.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"pec", "pec/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> Pec(
            @QuerydslPredicate(root = Pec.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPec.pec, Pec.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"pecazienda", "pecazienda/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pecazienda(
            @QuerydslPredicate(root = PecAzienda.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPecAzienda.pecAzienda, PecAzienda.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"pecprovider", "pecprovider/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pecprovider(
            @QuerydslPredicate(root = PecProvider.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPecProvider.pecProvider, PecProvider.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"pecstruttura", "pecstruttura/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pecstruttura(
            @QuerydslPredicate(root = PecStruttura.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPecStruttura.pecStruttura, PecStruttura.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"pecutente", "pecutente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pecutente(
            @QuerydslPredicate(root = PecUtente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPecUtente.pecUtente, PecUtente.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"permessoold", "permessoold/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> permessoold(
            @QuerydslPredicate(root = Permesso.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPermesso.permesso, Permesso.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"persona", "persona/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> persona(
            @QuerydslPredicate(root = Persona.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPersona.persona, Persona.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"ruolo", "ruolo/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ruolo(
            @QuerydslPredicate(root = Ruolo.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QRuolo.ruolo, Ruolo.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"struttura", "struttura/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> struttura(
            @QuerydslPredicate(root = Struttura.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QStruttura.struttura, Struttura.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"strutturaunificata", "strutturaunificata/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> strutturaunificata(
            @QuerydslPredicate(root = StrutturaUnificata.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QStrutturaUnificata.strutturaUnificata, StrutturaUnificata.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"tipopermesso", "tipopermesso/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> tipopermesso(
            @QuerydslPredicate(root = TipoPermesso.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QTipoPermesso.tipoPermesso, TipoPermesso.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"utente", "utente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> utente(
            @QuerydslPredicate(root = Utente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QUtente.utente, Utente.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"utentestruttura", "utentestruttura/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> utentestruttura(
            @QuerydslPredicate(root = UtenteStruttura.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QUtenteStruttura.utenteStruttura, UtenteStruttura.class);
        return ResponseEntity.ok(resource);
    }
}
