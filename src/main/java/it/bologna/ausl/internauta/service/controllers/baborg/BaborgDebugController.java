package it.bologna.ausl.internauta.service.controllers.baborg;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.service.masterjobs.workers.MasterjobsQueuer;
import it.bologna.ausl.internauta.service.masterjobs.workers.foo.FooWorker;
import it.bologna.ausl.internauta.service.masterjobs.workers.foo.FooWorkerData;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StoricoRelazioneRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.utentestruttura.UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${internauta.mapping.url.debug}")
public class BaborgDebugController {

    @Autowired
    StrutturaRepository strutturaRepository;
    
    @Autowired
    StoricoRelazioneRepository storicoRelazioneRepository;

    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    ProjectionFactory factory;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    CachedEntities cachedEntities;
    
    @Autowired
    ParametriAziendeReader parametriAziende;
    
    @Autowired
    EntityManager entityManager;
    
    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    ProjectionsInterceptorLauncher projectionsInterceptorLauncher;
    
    @Autowired
    BeanFactory beanFactory;
    
    @Autowired
    MasterjobsObjectsFactory masterjobsObjectsFactory;

    @Autowired
    @Qualifier(value = "redisMaterjobs")
    protected RedisTemplate redisTemplate;
    
    @Value("${masterjobs.manager.redis-active-threads-set-name}")
    private String activeThreadsSetName;
    
    @RequestMapping(value = "ping", method = RequestMethod.GET)
    public String ping() {
        return "pong";
    }

    private UtenteStruttura buildUtenteStruttura(Map<String, Object> map) {
        UtenteStruttura res = new UtenteStruttura();
        Field[] fields = UtenteStruttura.class.getDeclaredFields();
        for (Field field : fields) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            try {
                Method setMethod = EntityReflectionUtils.getSetMethod(UtenteStruttura.class, field.getName());
                if (columnAnnotation != null) {
                    try {
                        Object value = map.get(columnAnnotation.name());
                        if (value != null) {
                            if (value.getClass().isAssignableFrom(java.sql.Timestamp.class)) {
                                value = ZonedDateTime.ofInstant(((java.sql.Timestamp) value).toInstant(), ZoneId.systemDefault());
                            }
                            setMethod.invoke(res, value);
                        }
                    } catch (Exception ex) {
                        //ex.printStackTrace();
                    }
                } else {
                    JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
                    if (joinColumnAnnotation != null) {
                        Class<?> fkClass = field.getType();
                        try {
                            Object value = map.get(joinColumnAnnotation.name());
                            Object fkObject = fkClass.getDeclaredConstructor().newInstance();
                            Method primaryKeySetMethod = EntityReflectionUtils.getPrimaryKeySetMethod(fkClass);
                            primaryKeySetMethod.invoke(fkObject, value);

                            setMethod.invoke(res, fkObject);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }

        return res;
    }

    private UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom getUtenteStruttura(Map<String, Object> map) {
        UtenteStruttura utenteStruttura = utenteStrutturaRepository.getOne((Integer) map.get("id"));
        UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom res = factory.createProjection(UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom.class, utenteStruttura);
        return res;
    }

    @RequestMapping(value = "test", method = RequestMethod.GET)
//    @Transactional(rollbackFor = Throwable.class)
    public Object test(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException { //26839
        projectionsInterceptorLauncher.setRequestParams(new HashMap<String, String>(), request);
//        List<UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom> res;
//        List<Map<String, Object>> utentiStrutturaSottoResponsabili = strutturaRepository.getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(34513, null);
        List<Map<String, Object>> utentiStrutturaSottoResponsabili = strutturaRepository.getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(133143, LocalDateTime.of(2017, Month.JUNE, 14, 0, 0, 0));

        List<UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom> res = utentiStrutturaSottoResponsabili.stream().map(utenteStrutturaMap -> {
            UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom utenteStruttura = this.getUtenteStruttura(utenteStrutturaMap);
            //return factory.createProjection(UtenteStrutturaWithIdUtente.class, utenteStruttura);
            return utenteStruttura;
        }).collect(Collectors.toList());
        res.stream().forEach(u -> System.out.println(u.getIdUtente().getId()));
//        UtenteStruttura one = utenteStrutturaRepository.findById(14454160).get();
//        if (true)
//        return Lists.newArrayList(factory.createProjection(UtenteStrutturaWithIdAfferenzaStruttura.class, one));
        /*
        "attivo_al": null,
        "data_inserimento_riga": "2020-05-06T14:00:32.927+0000",
        "id_utente": 367520,
        "version": "2020-05-06T14:00:32.927+0000",
        "id_afferenza_struttura": 3,
        "incarico": true,
        "bit_ruoli": 256,
        "id_azienda_derivazione_unificazione": null,
        "id_dettaglio_contatto": 3788686,
        "attivo": true,
        "id": 14453746,
        "attributi": [
            "R"
        ],
        "attivo_dal": "2020-05-07T09:29:19.377+0000",
        "id_struttura": 26850
         */
        return res;

//       return EmlHandler.handleEml("C:\\Users\\mdonza\\Desktop\\Eml\\test_mail.eml");
        //return EmlHandler.handleEml("C:\\Users\\mdonza\\Desktop\\Eml\\donazione sig.ra Giliola Grillini.eml");
    }

    @RequestMapping(value = "test2", method = RequestMethod.GET)
    public Object test2(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException {
        
//        Expression<Integer> value = Expressions.asNumber(16);
//        NumberTemplate<Integer> numberTemplate = Expressions.numberTemplate(Integer.class, "function('bitand', {0}, {1})", QUtenteStruttura.utenteStruttura.bitRuoli, value);
//        
//        BooleanExpression filter = QUtenteStruttura.utenteStruttura.id.eq(28075300).and(numberTemplate.gt(0));
//        Iterable<UtenteStruttura> res = utenteStrutturaRepository.findAll(filter);
//        
//        List<ParametroAziende> parameters1 = parametriAziende.getParameters("FiltraResponsabiliMatrint", new Integer[] {2});
//        List<ParametroAziende> parameters2 = parametriAziende.getParameters("FiltraResponsabiliMatrint", new Integer[] {3});
//        
//        if (parameters1 != null && !parameters1.isEmpty())
//            System.out.println(parametriAziende.getValue(parameters1.get(0), Boolean.class));
//        else 
//            System.out.println("parameters1 empty");
//        if (parameters2 != null && !parameters2.isEmpty())
//            System.out.println(parametriAziende.getValue(parameters2.get(0), Boolean.class));
//        else 
//            System.out.println("parameters2 empty");
        Object struttureRuolo = storicoRelazioneRepository.getStruttureRuolo(256, 351272);
        
        return struttureRuolo;
    }
    
    @RequestMapping(value = "test3", method = RequestMethod.GET)
    @Transactional(rollbackFor = Throwable.class)
    public void test3(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException {
        Map entries = redisTemplate.opsForHash().entries(activeThreadsSetName);
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (entries.containsKey(String.valueOf(t.getId()))) {
                String tName = (String) entries.get(t.getId());
                
            }
        }
    }
    
    @RequestMapping(value = "test4", method = RequestMethod.GET)
    @Transactional(rollbackFor = Throwable.class)
    public void test4(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException, ClassNotFoundException, MasterjobsQueuingException {
        MasterjobsQueuer mjQueuer = beanFactory.getBean(MasterjobsQueuer.class);

        FooWorker worker1 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(1, "p1", false), false);
        FooWorker worker2 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(2, "p2", false), false);
        FooWorker worker3 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(3, "p3", false), false);
        FooWorker worker4 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(4, "p3", false), false);
        FooWorker worker5 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(5, "p3", false), false);
        FooWorker worker6 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(6, "p3", false), false);
        FooWorker worker7 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(7, "p3", false), false);
        FooWorker worker8 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(8, "p3", false), false);
        FooWorker worker9 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(9, "p3", false), false);
        FooWorker worker0 = masterjobsObjectsFactory.getWorker(FooWorker.class, new FooWorkerData(0, "p3", false), false);
        Applicazione applicazione = cachedEntities.getApplicazione("procton");
        boolean wait = true;
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker4, worker5, worker6), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGH);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
        mjQueuer.queue(Arrays.asList(worker4, worker5, worker6), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGH);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
    }
    
    private void test() {
        Persona p = personaRepository.findOne(QPersona.persona.id.eq(188013)).get();
        p.setDescrizione("1234");
        personaRepository.save(p);
    }
    
//    public String add(String string, Foo foo) {
//        return foo.method(string);
//    }   
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void manageCloned(UnaryOperator<Object> fn) {
        Integer id = 25240;
        Object entity = entityManager.find(beanFactory.getClass(), id);
        //res.setNome("Centrale di Sterilizzazione_99");
//        Struttura idStrutturaPadre = entity.getIdStrutturaPadre();
//        System.out.println("idPadre:" + idStrutturaPadre.getId());
        fn.apply(entity);
    }
    
//    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public Struttura loadCloned(Integer id) {
        Struttura res = entityManager.find(Struttura.class, id);
        //res.setNome("Centrale di Sterilizzazione_99");
        Struttura idStrutturaPadre = res.getIdStrutturaPadre();
        System.out.println("idPadre:" + idStrutturaPadre.getId());
        return res;
    }
}
