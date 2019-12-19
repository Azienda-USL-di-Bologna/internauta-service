//package it.bologna.ausl.internauta.service.controllers.baborg;
//
//import com.querydsl.core.types.Predicate;
//
//import it.bologna.ausl.internauta.service.repositories.baborg.AttivitaRepository;
//import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
//import it.bologna.ausl.internauta.service.repositories.baborg.Gdm1Repository;
//import it.bologna.ausl.internauta.service.repositories.baborg.Gdm2Repository;
//import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
//import it.bologna.ausl.model.entities.baborg.Gdm1;
//import it.bologna.ausl.model.entities.baborg.Gdm2;
//import it.bologna.ausl.model.entities.baborg.QGdm1;
//import it.bologna.ausl.model.entities.baborg.QGdm2;
//import it.bologna.ausl.model.entities.configuration.Applicazione;
//import it.bologna.ausl.model.entities.configuration.QApplicazione;
//import it.bologna.ausl.model.entities.scrivania.Attivita;
//import it.bologna.ausl.model.entities.scrivania.QAttivita;
//import it.nextsw.common.controller.BaseCrudController;
//
//import it.nextsw.common.controller.exceptions.RestControllerEngineException;
//import it.nextsw.common.utils.exceptions.EntityReflectionException;
//import javax.persistence.EntityManager;
//import javax.persistence.PersistenceContext;
//import javax.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.querydsl.binding.QuerydslPredicate;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping(value = "${baborg.mapping.url.root}")
//public class BaseControllerTest extends BaseCrudController {
//
//    private final Logger log = LoggerFactory.getLogger(BaseControllerTest.class);
//
//    @Autowired
//    AttivitaRepository attivitaRepository;
//
//    @Autowired
//    ApplicazioneRepository applicazioneRepository;
//
//    @Autowired
//    AziendaRepository aziendaRepository;
//
//    @Autowired
//    Gdm1Repository gdm1Repository;
//
//    @Autowired
//    Gdm2Repository gdm2Repository;
//
//    @PersistenceContext
//    EntityManager entityManager;
//
//    @Value("${baborg.mapping.url.root}")
//    private String baseUrl;
//
//    @RequestMapping(value = {"attivita", "attivita/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = Error.class)
//    public ResponseEntity<?> attivita(
//            @QuerydslPredicate(root = Attivita.class) Predicate predicate,
//            Pageable pageable,
//            @RequestParam(required = false) String projection,
//            @PathVariable(required = false) Integer id,
//            HttpServletRequest request,
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException {
//
//        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QAttivita.attivita, Attivita.class);
//        return ResponseEntity.ok(resource);
//    }
//
//    @RequestMapping(value = {"applicazione", "applicazione/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> applicazione(
//            /**
//             * il predicate è la rappresentazione dei filtri passati. In questo
//             * modo si va a reperire il repository e in base al metodo customize
//             * viene elaborato il filtro utilizzando l'oggetto Q
//             */
//            @QuerydslPredicate(root = Applicazione.class) Predicate predicate,
//            // se non si passano in automatico avrà valore null
//            Pageable pageable,
//            // nome della projection da usare; non obbligatoria perchè c'è comunque quella di default
//            @RequestParam(required = false) String projection,
//            // campo racchiuso tra {}
//            @PathVariable(required = false) String id,
//            // richiesta http
//            HttpServletRequest request,
//            /**
//             * stringa ma deve essere rappresentata come
//             * key=value,key=value,ecc...
//             * esempio...&additionalData=key1=value1,key2=value2
//             */
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {
//
//        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QApplicazione.applicazione, Applicazione.class);
//        return ResponseEntity.ok(resource);
//    }
//
//    @RequestMapping(value = {"gdm1", "gdm1/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> gdm1(
//            /**
//             * il predicate è la rappresentazione dei filtri passati. In questo
//             * modo si va a reperire il repository e in base al metodo customize
//             * viene elaborato il filtro utilizzando l'oggetto Q
//             */
//            @QuerydslPredicate(root = Gdm1.class) Predicate predicate,
//            // se non si passano in automatico avrà valore null
//            Pageable pageable,
//            // nome della projection da usare; non obbligatoria perchè c'è comunque quella di default
//            @RequestParam(required = false) String projection,
//            // campo racchiuso tra {}
//            @PathVariable(required = false) String id,
//            // richiesta http
//            HttpServletRequest request,
//            /**
//             * stringa ma deve essere rappresentata come
//             * key=value,key=value,ecc...
//             * esempio...&additionalData=key1=value1,key2=value2
//             */
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {
//
//        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QGdm1.gdm1, Gdm1.class);
//        return ResponseEntity.ok(resource);
//    }
//
//    @RequestMapping(value = {"gdm2", "gdm2/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
//    public ResponseEntity<?> gdm2(
//            /**
//             * il predicate è la rappresentazione dei filtri passati. In questo
//             * modo si va a reperire il repository e in base al metodo customize
//             * viene elaborato il filtro utilizzando l'oggetto Q
//             */
//            @QuerydslPredicate(root = Gdm2.class) Predicate predicate,
//            // se non si passano in automatico avrà valore null
//            Pageable pageable,
//            // nome della projection da usare; non obbligatoria perchè c'è comunque quella di default
//            @RequestParam(required = false) String projection,
//            // campo racchiuso tra {}
//            @PathVariable(required = false) String id,
//            // richiesta http
//            HttpServletRequest request,
//            /**
//             * stringa ma deve essere rappresentata come
//             * key=value,key=value,ecc...
//             * esempio...&additionalData=key1=value1,key2=value2
//             */
//            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, EntityReflectionException, RestControllerEngineException {
//
//        Object resource = getResources(request, id, projection, predicate, pageable, additionalData, QGdm2.gdm2, Gdm2.class);
//        return ResponseEntity.ok(resource);
//    }
//
//    @RequestMapping(value = {"test"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
////    @Transactional(rollbackFor = {Error.class})
//    public void test() {
//        Gdm1 gdm1 = gdm1Repository.getOne("1");
//        Gdm2 gdm21 = new Gdm2("gdm1");
//        gdm21.setIdGdm1(gdm1);
//        gdm21.setOggetto("011");
//        gdm21.setObbligatorio("zzzz");
//
//        Gdm2 gdm22 = new Gdm2("gdm3");
//        gdm22.setIdGdm1(gdm1);
//        gdm22.setOggetto("03");
//        gdm22.setObbligatorio("cccc");
//
////        List<Gdm2> gdm2List = gdm1.getGdm2List();
////        gdm2List.stream().forEach(e -> {System.out.println(e.toString());});
//        gdm1.getGdm2List().clear();
//        gdm1.getGdm2List().add(gdm21);
//        gdm1.getGdm2List().add(gdm22);
////        Applicazione app = new Applicazione("gdm", "gdmgdmgdm");
////        Applicazione app = attivita.getIdApplicazione();
////        entityManager.detach(app);
////        app = entityManager.getReference(Applicazione.class, "aaa");
//////        app.setId("aaa");
////        app.setNome("gdmgdmgdmgmd");
////        attivita.setIdApplicazione(app);
//
//        gdm1Repository.save(gdm1);
//    }
//
//    @Override
//    public String getBaseUrl() {
//        return this.baseUrl;
//    }
//
//}
