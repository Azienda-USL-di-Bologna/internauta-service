package it.bologna.ausl.internauta.utils.masterjobs.workers.services.ritentaversamenti;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.versatore.VersatoreServiceUtils;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author utente
 */
@MasterjobsWorker
public class RitentaVersamentiServiceWorker extends ServiceWorker{
       private static Logger log = LoggerFactory.getLogger(RitentaVersamentiServiceWorker.class);
       
        @Override
        public String getName() {
            return getClass().getSimpleName();
        }
        
        @Autowired
        private CachedEntities cachedEntities;
        
        @Autowired
        private ParametriAziendeReader parametriAziendaReader;
        
        @PersistenceContext
        private EntityManager em;
       
        @Override
        public WorkerResult doWork() throws MasterjobsWorkerException {
            log.info(String.format("starting %s...", getName()));
            List<Integer> aziendeSuCuiDeveGirare = new ArrayList<>();
            List<ParametroAziende> parameters = parametriAziendaReader.getParameters(ParametriAziendeReader.ParametriAzienda.usaRitentaVersamentiAutomatico);
            parameters.stream().filter(p -> 
                    p.getValore().equals("true")
            ).forEach(p -> {
                Stream.of(p.getIdAziende()).forEach(
                        idAzienda -> aziendeSuCuiDeveGirare.add(
                                idAzienda)
                        );
            });
            
            
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
            QDocDetail qDocDetail = QDocDetail.docDetail;
            QDoc qDoc = QDoc.doc;
            List<Integer> idDocList = jPAQueryFactory.select(qDocDetail.id)
                    .from(qDocDetail)
                    .where(qDocDetail.versamentoForzabile.eq(Boolean.TRUE)
                    .and(qDocDetail.dataUltimoVersamento.between(ZonedDateTime.now().minusMonths(3), ZonedDateTime.now()))
                    .and(qDocDetail.statoUltimoVersamento.eq(Versamento.StatoVersamento.ERRORE)))
                    .fetch();
    
            
            
            AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
            aziendeSuCuiDeveGirare.forEach(a -> {
                Map<Integer, Map<String, Object>> aziendeAttiveConParametriVersatore = VersatoreServiceUtils.getAziendeAttiveConParametri(parametriAziendaReader, cachedEntities);
                Map<String, Object> versatoreConfigAziendaValue = aziendeAttiveConParametriVersatore.get(a);
                log.info("accodo il job versamento per i documnenti da ritentare");
                if (versatoreConfigAziendaValue != null) {
                    jPAQueryFactory
                        .update(qDoc)
                        .set(qDoc.statoVersamento, Versamento.StatoVersamento.VERSARE)
                        .where(qDoc.id.in(idDocList))
                        .execute();
                    String hostId = (String) versatoreConfigAziendaValue.get("hostId");
                    Integer threadPoolSize = (Integer) versatoreConfigAziendaValue.get("threadPoolSize");
                    Map<String,Object> params = (Map<String,Object>) versatoreConfigAziendaValue.get("params");
                    try {
                        accodatoreVeloce.accodaVersatore(
                                idDocList,
                                a,
                                hostId,
                                SessioneVersamento.TipologiaVersamento.RITENTA,
                                1,
                                threadPoolSize,
                                params
                        );
                    } catch (MasterjobsWorkerException ex) {
                        java.util.logging.Logger.getLogger(RitentaVersamentiServiceWorker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });


                

            return null;
        }

}
