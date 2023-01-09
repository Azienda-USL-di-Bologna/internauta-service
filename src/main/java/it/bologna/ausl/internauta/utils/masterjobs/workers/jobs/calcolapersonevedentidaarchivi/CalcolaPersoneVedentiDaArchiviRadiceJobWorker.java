package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidaarchivi;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.model.entities.scripta.QArchivio;
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
public class CalcolaPersoneVedentiDaArchiviRadiceJobWorker extends JobWorker<CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(CalcolaPersoneVedentiDaArchiviRadiceJobWorker.class);
    private final String name = CalcolaPersoneVedentiDaArchiviRadiceJobWorker.class.getSimpleName();
    
    @Autowired
    private ArchivioDocRepository archivioDocRepository;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");

        CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData data = getWorkerData();
        List<Integer> idDocsDaArchivi = null;
        
        try {
            if (data.getIdArchiviRadice() != null && !data.getIdArchiviRadice().isEmpty()) {
                QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
                QArchivio qArchivio = QArchivio.archivio;
                JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
                idDocsDaArchivi = jpaQueryFactory
                        .select(qArchivioDoc.idDoc.id).distinct()
                        .from(qArchivioDoc)
                        .join(qArchivio).on(qArchivio.id.eq(qArchivioDoc.idArchivio.id))
                        .where(qArchivio.idArchivioRadice.id.in(data.getIdArchiviRadice()))
                        .fetch();
                //idDocsDaArchivi = archivioDocRepository.getIdDocsDaArchiviRadice(data.getIdArchiviRadice());
                log.info("idDocsDaArchivi calcolati");
            }
        } catch (Exception ex) {
           String errore = "Errore nel calcolo dei documenti su cui calcolare le persone vedenti";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }
        
        
        
        if (idDocsDaArchivi != null) {
            log.info("idDocsDaArchivi non e' null");
            AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
            for (Integer idDoc : idDocsDaArchivi) {
                accodatoreVeloce.accodaCalcolaPersoneVedentiDoc(idDoc);
            }
        }
        
        return null;
    }
}
