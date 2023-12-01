package it.bologna.ausl.internauta.service.controllers.baborg;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fooexternal.FooExternalWorkerData;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.utils.jpa.natiquery.NativeQueryTools;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerInitializationException;
import it.bologna.ausl.internauta.utils.masterjobs.repository.JobReporitory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MultiJobQueueDescriptor;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.foo.FooWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.foo.FooWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fooexternal.FooExternalWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriacontatti.SanatoriaContattiJobWorker;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.utentestruttura.UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.masterjobs.Job;
import it.bologna.ausl.model.entities.masterjobs.QJob;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.scripta.QArchivioInfo;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.csv.QuoteMode;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

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
    private ParametriAziendeReader parametriAziendeReader;

    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    MasterjobsJobsQueuer masterjobsJobsQueuer;

    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    ProjectionFactory factory;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @PersistenceContext
    EntityManager entityManager;   

    @Autowired
    ProjectionsInterceptorLauncher projectionsInterceptorLauncher;
    
    @Autowired
    BeanFactory beanFactory;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    @Autowired
    private JobReporitory jobRepository;

    @Autowired
    private ArchivioRepository archivioRepository;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Autowired
    @Qualifier(value = "redisMaterjobs")
    protected RedisTemplate redisTemplate;
    
    @Value("${masterjobs.manager.jobs-executor.redis-active-threads-set-name}")
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
    public Object test2(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException, MasterjobsWorkerInitializationException, MasterjobsQueuingException {
        
        FooWorker fooWorker1 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(1, "1", false), false, 5000);
        FooWorker fooWorker2 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(2, "2", false), false, 5000);
        FooWorker fooWorker3 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(3, "3", false), false, 5000);
        MasterjobsJobsQueuer mjQueuer = beanFactory.getBean(MasterjobsJobsQueuer.class);
        
        List<MultiJobQueueDescriptor> descriptors = Arrays.asList(
                MultiJobQueueDescriptor.newBuilder().addWorker(fooWorker1).objectId("1").app("aaa").waitForObject(false).build(),
                MultiJobQueueDescriptor.newBuilder().addWorker(fooWorker2).addWorker(fooWorker3).objectId("1").waitForObject(true).app("aaa").build()
        );
        
//        mjQueuer.queue(fooWorker, null, null, null, false, Set.SetPriority.NORMAL, false);
        mjQueuer.queueMultiJobs(descriptors, null);
        return null;
    }
    
    @RequestMapping(value = "test3", method = RequestMethod.GET)
//    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRES_NEW)
    public void test3(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException {
        String query = ""
                + "select distinct u.id_persona, array_agg(distinct id_struttura) as strutture "
                + " from baborg.cambiamenti_associazioni ca " +
                "join baborg.utenti u " +
                "on u.id = ca.id_utente " +
                "group by id_persona order by id_persona";
        List resultList = entityManager.createNativeQuery(query).getResultList();
        NativeQueryTools nq = new NativeQueryTools(entityManager);
        List listarisultati = nq.asListOfMaps(resultList, nq.getColumnNameToIndexMap(query));
        System.out.println("pippo");
        

    }
    
    @RequestMapping(value = "test5", method = RequestMethod.GET)
    @Transactional(rollbackFor = Throwable.class)
    public void test5(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException, ClassNotFoundException, MasterjobsQueuingException, MasterjobsWorkerException {
        MasterjobsJobsQueuer mjQueuer = beanFactory.getBean(MasterjobsJobsQueuer.class);
        mjQueuer.stopThreads();
    }
    
    @RequestMapping(value = "test6", method = RequestMethod.GET)
//    @Transactional(rollbackFor = Throwable.class)
    public void test6(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException, ClassNotFoundException, MasterjobsQueuingException, MasterjobsWorkerException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.executeWithoutResult(a2 -> {
        
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.executeWithoutResult(a1 -> {
                JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
                Job job = entityManager.find(Job.class, 997140l);
                System.out.println(job.getState());
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.executeWithoutResult(a -> {
                    queryFactory
                            .update(QJob.job)
                            .set(QJob.job.state, Job.JobState.RUNNING)
                            .where(QJob.job.name.eq("testgdm"))
                            .execute();
                });
                System.out.println(job.getState());
                //job = entityManager.find(Job.class, 997140l);
//                entityManager.refresh(job);
                job = queryFactory.select(QJob.job).from(QJob.job).where(QJob.job.id.eq(997140l)).fetchOne();
                System.out.println(job.getState());
            });
        });
    }
    
    @RequestMapping(value = "testgus", method = RequestMethod.GET)
    @Transactional(rollbackFor = Throwable.class)
    public void testgus(HttpServletRequest request) {
        List<ParametroAziende> parameters = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.ricalcoloPermessiArchivi.toString());
        if (parameters == null || parameters.isEmpty() || parameters.size() > 1) {
            System.out.println("naaa");
        }
        RicalcoloPermessiArchiviParams parametri = parametriAziendeReader.getValue(parameters.get(0), RicalcoloPermessiArchiviParams.class);
        
        System.out.println("GiorniPerDataMassimaUltimoRicalcolo: " + parametri.getGiorniPerDataMassimaUltimoRicalcolo());
        System.out.println("GiorniPerDataMinimaUltimoUtilizzo" + parametri.getGiorniPerDataMinimaUltimoUtilizzo());
        System.out.println("NumeroArchiviAggiuntiviDaRecuperare" + parametri.getNumeroArchiviAggiuntiviDaRecuperare());
        
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime dataMassimaUltimoRicalcolo = now.minusDays(parametri.getGiorniPerDataMassimaUltimoRicalcolo());
        ZonedDateTime dataMinimaUltimoUtilizzo = now.minusDays(parametri.getGiorniPerDataMinimaUltimoUtilizzo());
        
        QArchivioInfo qArchivioinfo = QArchivioInfo.archivioInfo;
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        
        JPAQuery<Integer> archiviDaRicalcolarePerMaggioreUtilizzo = jPAQueryFactory
                .select(qArchivioinfo.id)
                .from(qArchivioinfo)
                .where(qArchivioinfo.dataUltimoUtilizzo.goe(dataMinimaUltimoUtilizzo)
                        .and(qArchivioinfo.dataUltimoRicalcoloPermessi.loe(dataMassimaUltimoRicalcolo))
                )
                .fetchAll();
        
        JPAQuery<Integer> archiviDaRicalcolarePerRecupero = jPAQueryFactory
                .select(qArchivioinfo.id)
                .from(qArchivioinfo)
                .orderBy(qArchivioinfo.dataUltimoRicalcoloPermessi.asc())
                .limit(parametri.getNumeroArchiviAggiuntiviDaRecuperare())
                .fetchAll();
        
        System.out.println("Ora accodo il job per il calcolo di ogni singolo archivio");
        //AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
        
        Integer i = 0;
        
        for (Iterator<Integer> a = archiviDaRicalcolarePerMaggioreUtilizzo.iterate(); a.hasNext();) {
            Integer idArchivio = a.next();
            i++;
            //accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivio, idArchivio.toString(), "scripta_archivio", null);
        }

        System.out.println("Size di archiviDaRicalcolarePerMaggioreUtilizzo:" + i);
        
        i = 0;
        
        for (Iterator<Integer> a = archiviDaRicalcolarePerRecupero.iterate(); a.hasNext();) {
            Integer idArchivio = a.next();
            i++;
            //accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivio, idArchivio.toString(), "scripta_archivio", null);
        }
        
        System.out.println("Size di archiviDaRicalcolarePerRecupero:" + i);
    }
    
    public static class RicalcoloPermessiArchiviParams {
        Integer numeroArchiviAggiuntiviDaRecuperare;
        Integer giorniPerDataMinimaUltimoUtilizzo;
        Integer giorniPerDataMassimaUltimoRicalcolo;
        
        public RicalcoloPermessiArchiviParams() {};
        
        public Integer getNumeroArchiviAggiuntiviDaRecuperare() {
            return numeroArchiviAggiuntiviDaRecuperare;
        }

        public void setNumeroArchiviAggiuntiviDaRecuperare(Integer numeroArchiviAggiuntiviDaRecuperare) {
            this.numeroArchiviAggiuntiviDaRecuperare = numeroArchiviAggiuntiviDaRecuperare;
        }

        public Integer getGiorniPerDataMinimaUltimoUtilizzo() {
            return giorniPerDataMinimaUltimoUtilizzo;
        }

        public void setGiorniPerDataMinimaUltimoUtilizzo(Integer giorniPerDataMinimaUltimoUtilizzo) {
            this.giorniPerDataMinimaUltimoUtilizzo = giorniPerDataMinimaUltimoUtilizzo;
        }

        public Integer getGiorniPerDataMassimaUltimoRicalcolo() {
            return giorniPerDataMassimaUltimoRicalcolo;
        }

        public void setGiorniPerDataMassimaUltimoRicalcolo(Integer giorniPerDataMassimaUltimoRicalcolo) {
            this.giorniPerDataMassimaUltimoRicalcolo = giorniPerDataMassimaUltimoRicalcolo;
        }
    }
    
    @RequestMapping(value = "test4", method = RequestMethod.GET)
    @Transactional(rollbackFor = Throwable.class)
    public void test4(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException, ClassNotFoundException, MasterjobsQueuingException, MasterjobsWorkerException {
        //MasterjobsJobsQueuer mjQueuer = beanFactory.getBean(MasterjobsJobsQueuer.class);
        FooExternalWorkerData fooExternalWorkerData = new FooExternalWorkerData(1, "p1", false);
        FooExternalWorker worker1 = masterjobsObjectsFactory.getJobWorker(FooExternalWorker.class, new FooExternalWorkerData(1, "p1", false), false);
        String calcolaMD5 = jobRepository.calcolaMD5(worker1.getName(), objectMapper.writeValueAsString(fooExternalWorkerData), Boolean.TRUE);
        System.out.println(calcolaMD5);
        
        Optional<Job> findOne = jobRepository.findOne(QJob.job.id.eq(175714L));
        System.out.println(findOne.get().getHash().toString());
        System.out.println(UUID.fromString(calcolaMD5.replaceFirst( 
        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5" 
        )).toString());
        
//        mjQueuer.relaunchJobsInError();
//        Service find = entityManager.find(Service.class, 1l);
//        System.out.println(find);
//        FooWorker worker2 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(2, "p2", false), false, 60000);
//        FooWorker worker3 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(3, "p3", false), false);
//        FooWorker worker4 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(4, "p3", false), false);
//        FooWorker worker5 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(5, "p3", false), false);
//        FooWorker worker6 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(6, "p3", false), false);
//        FooWorker worker7 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(7, "p3", false), false);
//        FooWorker worker8 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(8, "p3", false), false);
//        FooWorker worker9 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(9, "p3", false), false);
//        FooWorker worker0 = masterjobsObjectsFactory.getJobWorker(FooWorker.class, new FooWorkerData(0, "p3", false), false);
//        Applicazione applicazione = cachedEntities.getApplicazione("procton");
//        boolean wait = true;
//        mjQueuer.queue(Arrays.asList(worker2, worker2, worker1, worker2), "1", "t1", applicazione.getId(), true, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t1", applicazione.getId(), true, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t1", applicazione.getId(), false, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "2", "t2", applicazione.getId(), true, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "2", "t2", applicazione.getId(), true, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "2", "t2", applicazione.getId(), true, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker4, worker5, worker6), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGH);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker1, worker2, worker3), "1", "t2", applicazione.getId(), wait, Set.SetPriority.NORMAL);
//        mjQueuer.queue(Arrays.asList(worker4, worker5, worker6), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGH);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
//        mjQueuer.queue(Arrays.asList(worker7, worker8, worker9), "1", "t2", applicazione.getId(), wait, Set.SetPriority.HIGHEST);
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
    
    @RequestMapping(value = "testJob", method = RequestMethod.GET)
    @Transactional(rollbackFor = Throwable.class)
    public void testJob() throws MasterjobsWorkerException {
        try {
            masterjobsJobsQueuer.queue(
                    new SanatoriaContattiJobWorker(),
                    null,
                    null,
                    Applicazione.Applicazioni.rubrica.toString(),
                    false,
                    Set.SetPriority.NORMAL,
                    null
            );
        } catch (MasterjobsQueuingException ex) {
            throw new MasterjobsWorkerException("errore nell'accodamento del job", ex);
        }
    }
}
