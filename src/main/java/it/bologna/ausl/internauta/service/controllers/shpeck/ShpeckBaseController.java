package it.bologna.ausl.internauta.service.controllers.shpeck;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.model.entities.shpeck.Address;
import it.bologna.ausl.model.entities.shpeck.Draft;
import it.bologna.ausl.model.entities.shpeck.Note;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageAddress;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.QAddress;
import it.bologna.ausl.model.entities.shpeck.QDraft;
import it.bologna.ausl.model.entities.shpeck.QNote;
import it.bologna.ausl.model.entities.shpeck.QFolder;
import it.bologna.ausl.model.entities.shpeck.QMessageAddress;
import it.bologna.ausl.model.entities.shpeck.QMessageFolder;
import it.bologna.ausl.model.entities.shpeck.QMessageTag;
import it.bologna.ausl.model.entities.shpeck.QRawMessage;
import it.bologna.ausl.model.entities.shpeck.QTag;
import it.bologna.ausl.model.entities.shpeck.QRecepit;
import it.bologna.ausl.model.entities.shpeck.RawMessage;
import it.bologna.ausl.model.entities.shpeck.Recepit;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.bologna.ausl.model.entities.shpeck.views.DraftLite;
import it.bologna.ausl.model.entities.shpeck.views.MessageComplete;
import it.bologna.ausl.model.entities.shpeck.views.QDraftLite;
import it.bologna.ausl.model.entities.shpeck.views.QMessageComplete;
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
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.model.entities.shpeck.Outbox;
import it.bologna.ausl.model.entities.shpeck.QOutbox;
import it.bologna.ausl.model.entities.shpeck.views.OutboxLite;
import it.bologna.ausl.model.entities.shpeck.views.QOutboxLite;

@RestController
@RequestMapping(value = "${shpeck.mapping.url.root}")
public class ShpeckBaseController extends BaseCrudController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShpeckBaseController.class);

    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }

    @Autowired
    private MessageRepository messageRespository;

    @RequestMapping(value = {"tag", "tag/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> tag(
            @QuerydslPredicate(root = Tag.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QTag.tag, Tag.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"messagetag", "messagetag/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> messagetag(
            @QuerydslPredicate(root = MessageTag.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMessageTag.messageTag, MessageTag.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"folder", "folder/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> folder(
            @QuerydslPredicate(root = Folder.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QFolder.folder, Folder.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"messagefolder", "messagefolder/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> messagefolder(
            @QuerydslPredicate(root = MessageFolder.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMessageFolder.messageFolder, MessageFolder.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"address", "address/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> address(
            @QuerydslPredicate(root = Address.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAddress.address, Address.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"messageaddress", "messageaddress/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> messageaddress(
            @QuerydslPredicate(root = MessageAddress.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMessageAddress.messageAddress, MessageAddress.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"message", "message/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> message(
            @QuerydslPredicate(root = Message.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMessage.message, Message.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"recepit", "recepit/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> recepit(
            @QuerydslPredicate(root = Recepit.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QRecepit.recepit, Recepit.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"rawmessage", "rawmessage/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rawmessage(
            @QuerydslPredicate(root = RawMessage.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QRawMessage.rawMessage, RawMessage.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"draft", "draft/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> draft(
            @QuerydslPredicate(root = Draft.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QDraft.draft, Draft.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"draftlite", "draftlite/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> draftlite(
            @QuerydslPredicate(root = DraftLite.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QDraftLite.draftLite, DraftLite.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"outbox", "outbox/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> outbox(
            @QuerydslPredicate(root = Outbox.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QOutbox.outbox, Outbox.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"outboxLite", "outboxLite/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> outboxLite(
            @QuerydslPredicate(root = OutboxLite.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {
        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QOutboxLite.outboxLite, OutboxLite.class);
        return ResponseEntity.ok(resource);
    }

    @RequestMapping(value = {"note", "note/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> note(
            @QuerydslPredicate(root = Note.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QNote.note, Note.class);
        return ResponseEntity.ok(resource);
    }

    /*
        vista
     */
    @RequestMapping(value = {"messagecomplete", "messagecomplete/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> messagecomplete(
            @QuerydslPredicate(root = MessageComplete.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QMessageComplete.messageComplete, MessageComplete.class);
        return ResponseEntity.ok(resource);
    }
}
