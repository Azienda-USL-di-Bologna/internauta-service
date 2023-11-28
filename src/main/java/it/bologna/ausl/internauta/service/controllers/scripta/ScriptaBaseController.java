package it.bologna.ausl.internauta.service.controllers.scripta;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.ArchivioDiInteresse;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.ArchivioRecente;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
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
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDoc;
import it.bologna.ausl.model.entities.scripta.FrequenzaUtilizzoArchivio;
import it.bologna.ausl.model.entities.scripta.Massimario;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.NoteVersamento;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.PersonaVedente;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QArchivioDiInteresse;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QArchivioRecente;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QAttoreDoc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.scripta.QDocDoc;
import it.bologna.ausl.model.entities.scripta.QFrequenzaUtilizzoArchivio;
import it.bologna.ausl.model.entities.scripta.QMassimario;
import it.bologna.ausl.model.entities.scripta.views.QDocDetailView;
import it.bologna.ausl.model.entities.scripta.QMezzo;
import it.bologna.ausl.model.entities.scripta.QNoteVersamento;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPersonaVedente;
import it.bologna.ausl.model.entities.scripta.QRelated;
import it.bologna.ausl.model.entities.scripta.QSmistamento;
import it.bologna.ausl.model.entities.scripta.QSpedizione;
import it.bologna.ausl.model.entities.scripta.QTitolo;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Smistamento;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView;
import it.bologna.ausl.model.entities.scripta.views.DocDetailView;
import it.bologna.ausl.model.entities.scripta.views.QArchivioDetailView;

@RestController
@RequestMapping(value = "${scripta.mapping.url.root}")
public class ScriptaBaseController extends BaseCrudController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptaBaseController.class);

    @Autowired
    private RestControllerEngineImpl restControllerEngine;

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
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

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
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMezzo.mezzo, Mezzo.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"allegato", "allegato/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> allegato(
            @QuerydslPredicate(root = Allegato.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAllegato.allegato, Allegato.class);
        return ResponseEntity.ok(resource);
    }

//    @RequestMapping(value = {"dettaglioAllegato", "dettaglioAllegato/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<?> dettaglioAllegato(
//            @QuerydslPredicate(root = DettaglioAllegato.class) Predicate predicate,
//            Pageable pageable,
//            @RequestParam(required = false) String projection,
//            @PathVariable(required = false) Integer id,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {
//
//        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QDettaglioAllegato.dettaglioAllegato, DettaglioAllegato.class);
//        return ResponseEntity.ok(resource);
//    }
//
    @RequestMapping(value = {"related", "related/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> related(
            @QuerydslPredicate(root = Related.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

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
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

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
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QSpedizione.spedizione, Spedizione.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"docdetail", "docdetail/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> docdetail(
            @QuerydslPredicate(root = DocDetail.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QDocDetail.docDetail, DocDetail.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"personavedente", "personavedente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> personavedente(
            @QuerydslPredicate(root = PersonaVedente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPersonaVedente.personaVedente, PersonaVedente.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"docdetailview", "docdetailview/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> docdetailview(
            @QuerydslPredicate(root = DocDetailView.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QDocDetailView.docDetailView, DocDetailView.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"archivio", "archivio/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> archivio(
            @QuerydslPredicate(root = Archivio.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QArchivio.archivio, Archivio.class);

        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"archiviodoc", "archiviodoc/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> archiviodoc(
            @QuerydslPredicate(root = ArchivioDoc.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QArchivioDoc.archivioDoc, ArchivioDoc.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"attorearchivio", "attorearchivio/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> attorearchivio(
            @QuerydslPredicate(root = AttoreArchivio.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAttoreArchivio.attoreArchivio, AttoreArchivio.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"attoredoc", "attoredoc/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> attoredoc(
            @QuerydslPredicate(root = AttoreDoc.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAttoreDoc.attoreDoc, AttoreDoc.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"archiviodiinteresse", "archiviodiinteresse/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> archiviodiinteresse(
            @QuerydslPredicate(root = ArchivioDiInteresse.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QArchivioDiInteresse.archivioDiInteresse, ArchivioDiInteresse.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"frequenzautilizzoarchivio", "frequenzautilizzoarchivio/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> frequenzautilizzoarchivio(
            @QuerydslPredicate(root = FrequenzaUtilizzoArchivio.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QFrequenzaUtilizzoArchivio.frequenzaUtilizzoArchivio, FrequenzaUtilizzoArchivio.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"archiviodetail", "archiviodetail/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> archiviodetail(
            @QuerydslPredicate(root = ArchivioDetail.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QArchivioDetail.archivioDetail, ArchivioDetail.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"permessoarchivio", "permessoarchivio/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> permessoarchivio(
            @QuerydslPredicate(root = PermessoArchivio.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QPermessoArchivio.permessoArchivio, PermessoArchivio.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"archiviodetailview", "archiviodetailview/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> archiviodetailview(
            @QuerydslPredicate(root = ArchivioDetailView.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QArchivioDetailView.archivioDetailView, ArchivioDetailView.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"massimario", "massimario/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> massimario(
            @QuerydslPredicate(root = Massimario.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMassimario.massimario, Massimario.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"titolo", "titolo/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> titolo(
            @QuerydslPredicate(root = Titolo.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QTitolo.titolo, Titolo.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"noteversamento", "noteversamento/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> noteVersamento(
            @QuerydslPredicate(root = NoteVersamento.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QNoteVersamento.noteVersamento, NoteVersamento.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"archiviorecente", "archiviorecente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<?> archiviorecente(
            @QuerydslPredicate(root = ArchivioRecente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QArchivioRecente.archivioRecente, ArchivioRecente.class);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = {"docdoc", "docdoc/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> docdoc(
            @QuerydslPredicate(root = DocDoc.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QDocDoc.docDoc, DocDoc.class);
        return ResponseEntity.ok(resource);
    }
}
