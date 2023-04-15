package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessigerarchiaarchivio;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mido
 */
@MasterjobsWorker
public class CalcoloPermessiGerarchiaArchivioJobWorker extends JobWorker<CalcoloPermessiGerarchiaArchivioJobWorkerData, JobWorkerResult>{
    private static final Logger log = LoggerFactory.getLogger(CalcoloPermessiGerarchiaArchivioJobWorker.class);
    private final String name = CalcoloPermessiGerarchiaArchivioJobWorker.class.getSimpleName();

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");

        CalcoloPermessiGerarchiaArchivioJobWorkerData data = getWorkerData();
        log.info("Calcolo permessi archivioRadice: " + data.getIdArchivioRadice().toString());
        
        List<Integer> idArchivi = null;
        
        try {
            QArchivio qArchivio = QArchivio.archivio;
            
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
            idArchivi = jPAQueryFactory
                    .select(qArchivio.id)
                    .from(qArchivio)
                    .where(qArchivio.idArchivioRadice.id.eq(data.getIdArchivioRadice()))
                    .fetch();
        } catch (Exception ex){
           String errore = "Errore nel calcolo dei permessi espliciti degli archivi";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }
        
        log.info("Ora accodo il job per il calcolo di ogni singolo archivio");
        AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
        for (Integer idArchivio : idArchivi) {
            // Come object id uso idArchivioRadice perché voglio che CalcolaPersoneVedentiDaArchiviRadice abbia il wait for object rispetto a tutti questi job
            accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivio, data.getIdArchivioRadice().toString(), "scripta_archivio", null);
        }
        
        log.info("Ora accodo il ricalcolo persone vedenti");
        accodatoreVeloce.accodaCalcolaPersoneVedentiDaArchiviRadice(new HashSet(Arrays.asList(data.getIdArchivioRadice())), data.getIdArchivioRadice().toString(), "scripta_archivio", null);
        return null;
    }
}
