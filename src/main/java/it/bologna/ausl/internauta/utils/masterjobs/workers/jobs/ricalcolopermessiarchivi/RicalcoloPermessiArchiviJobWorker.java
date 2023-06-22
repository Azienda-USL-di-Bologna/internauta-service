package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.ricalcolopermessiarchivi;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessigerarchiaarchivio.*;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class RicalcoloPermessiArchiviJobWorker extends JobWorker<CalcoloPermessiGerarchiaArchivioJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(RicalcoloPermessiArchiviJobWorker.class);
    private final String name = RicalcoloPermessiArchiviJobWorker.class.getSimpleName();
    
    @Autowired
    private ParametriAziendeReader parametriAziendeReader;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");
        
        List<ParametroAziende> parameters = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.ricalcoloPermessiArchivi.toString());
        if (parameters == null || parameters.isEmpty() || parameters.size() > 1) {
            throw new MasterjobsWorkerException("il parametro ricalcoloPermessiArchivi non è presente una e una sola volta");
        }
        RicalcoloPermessiArchiviParams parametri = parametriAziendeReader.getValue(parameters.get(0), RicalcoloPermessiArchiviParams.class);
        
        log.info("GiorniPerDataMassimaUltimoRicalcolo: " + parametri.getGiorniPerDataMassimaUltimoRicalcolo());
        log.info("GiorniPerDataMinimaUltimoUtilizzo" + parametri.getGiorniPerDataMinimaUltimoUtilizzo());
        log.info("NumeroArchiviAggiuntiviDaRecuperare" + parametri.getNumeroArchiviAggiuntiviDaRecuperare());
        
        
//        CalcoloPermessiGerarchiaArchivioJobWorkerData data = getWorkerData();
//        log.info("Calcolo permessi archivioRadice: " + data.getIdArchivioRadice().toString());
//        
//        List<Integer> idArchivi = null;
//        
//        try {
//            QArchivio qArchivio = QArchivio.archivio;
//            
//            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
//            idArchivi = jPAQueryFactory
//                    .select(qArchivio.id)
//                    .from(qArchivio)
//                    .where(qArchivio.idArchivioRadice.id.eq(data.getIdArchivioRadice()))
//                    .fetch();
//        } catch (Exception ex){
//           String errore = "Errore nel calcolo dei permessi espliciti degli archivi";
//           log.error(errore, ex);
//           throw new MasterjobsWorkerException(errore, ex);
//        }
//        
//        log.info("Ora accodo il job per il calcolo di ogni singolo archivio");
//        AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
//        for (Integer idArchivio : idArchivi) {
//            // Come object id uso idArchivioRadice perché voglio che CalcolaPersoneVedentiDaArchiviRadice abbia il wait for object rispetto a tutti questi job
//            accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivio, data.getIdArchivioRadice().toString(), "scripta_archivio");
//        }
//        
//        log.info("Ora accodo il ricalcolo persone vedenti");
//        accodatoreVeloce.accodaCalcolaPersoneVedentiDaArchiviRadice(new HashSet(Arrays.asList(data.getIdArchivioRadice())), data.getIdArchivioRadice().toString(), "scripta_archivio", null);
        return null;
    }
    
    private class RicalcoloPermessiArchiviParams {
        Integer numeroArchiviAggiuntiviDaRecuperare;
        Integer giorniPerDataMinimaUltimoUtilizzo;
        Integer giorniPerDataMassimaUltimoRicalcolo;

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
}
