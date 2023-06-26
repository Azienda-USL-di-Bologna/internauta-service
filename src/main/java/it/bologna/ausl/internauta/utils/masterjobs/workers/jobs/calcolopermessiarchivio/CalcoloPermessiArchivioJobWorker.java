package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class CalcoloPermessiArchivioJobWorker extends JobWorker<CalcoloPermessiArchivioJobWorkerData, JobWorkerResult>{
    private static final Logger log = LoggerFactory.getLogger(CalcoloPermessiArchivioJobWorker.class);
    private final String name = CalcoloPermessiArchivioJobWorker.class.getSimpleName();
    
    @Autowired
    private ArchivioRepository archivioRepository;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");

        CalcoloPermessiArchivioJobWorkerData data = getWorkerData();
        log.info("Calcolo permessi archivio: " + data.getIdArchivio().toString());
        
        try {
            archivioRepository.calcolaPermessiEspliciti(data.getIdArchivio());
        } catch (Exception ex){
           String errore = "Errore nel calcolo dei permessi espliciti dello archivio";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }
        
        if (data.getQueueJobCalcolaPersoneVedentiDoc()) {
            log.info("Ora inserisco i job per calcolare le persone vedenti dei documenti contenuti sull'archivio");
            QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
            JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
            List<Integer> idDocsDaArchivio = jpaQueryFactory
                    .select(qArchivioDoc.idDoc.id)
                    .from(qArchivioDoc)
                    .where(qArchivioDoc.idArchivio.id.eq(data.getIdArchivio()))
                    .fetch();
            log.info("idDocsDaArchivi calcolati");
            if (idDocsDaArchivio != null) {
                log.info("idDocsDaArchivi non e' null");
                AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
                for (Integer idDoc : idDocsDaArchivio) {
                    accodatoreVeloce.accodaCalcolaPersoneVedentiDoc(idDoc, idDoc.toString(), "scripta_doc", null);
                }
            }
        }
        
        return null;
    }
}
