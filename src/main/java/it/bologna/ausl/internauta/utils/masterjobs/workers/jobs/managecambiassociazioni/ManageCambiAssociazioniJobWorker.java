package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sun.org.apache.xerces.internal.impl.xs.XSMessageFormatter;
import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.service.repositories.baborg.CambiamentiAssociazioneRepository;
import it.bologna.ausl.internauta.utils.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDataInterface;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorkerData;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.CambiamentiAssociazione;
import it.bologna.ausl.model.entities.baborg.QCambiamentiAssociazione;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.persistence.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class ManageCambiAssociazioniJobWorker extends JobWorker {
    private static final Logger log = LoggerFactory.getLogger(ManageCambiAssociazioniJobWorker.class);

    @Autowired
    private CambiamentiAssociazioneRepository cambiamentiAssociazioneRepository;
    
    @Autowired
    PermissionRepositoryAccess permissionRepositoryAccess;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("job %s started", getName()));
        ManageCambiAssociazioniJobWorkerData data = getWorkerData(ManageCambiAssociazioniJobWorkerData.class);
        Iterable<CambiamentiAssociazione> cambiamentiAssociazioni = cambiamentiAssociazioneRepository.findAll(
                QCambiamentiAssociazione.cambiamentiAssociazione.fatto.eq(false).and(
                        QCambiamentiAssociazione.cambiamentiAssociazione.dataInserimentoRiga.loe(data.getDataRiferimento())), 
                Sort.by(Sort.Direction.ASC, QCambiamentiAssociazione.cambiamentiAssociazione.id.getAnnotatedElement().getDeclaredAnnotation(Column.class).name()));
        if(cambiamentiAssociazioni!= null) {
            Set<Integer> archiviDaPermessizzare = new HashSet();
            List<Integer> idCambiamentiAssociazioni = new ArrayList<>();
            for (CambiamentiAssociazione cambiamentiAssociazione : cambiamentiAssociazioni) {
                log.info("Sto chiedendo alla blackbox i permessi che ha ogni struttura sui fascicoli");
                EntitaStoredProcedure soggetto = new EntitaStoredProcedure();
                soggetto.setIdProvenienza(cambiamentiAssociazione.getIdStruttura().getId());
                soggetto.setSchema("baborg");
                soggetto.setTable("strutture");
                List<PermessoEntitaStoredProcedure> permessiStrutturaSuArchivi;
                
                try {
                    log.info(String.format("sto chiedendo alla blackbox i permessi per la riga %s", cambiamentiAssociazione.getId()));
                    permessiStrutturaSuArchivi = permissionRepositoryAccess.getPermissionsOfSubjectAdvanced(soggetto, null, null, Arrays.asList("SCRIPTA"), Arrays.asList("ARCHIVIO"), true, LocalDate.now(), null, BlackBoxConstants.Direzione.PRESENTE);                
                } catch (BlackBoxPermissionException ex) {
                    String errorMessage = String.format("Errore nella lettura dei permessi della struttura, nella riga %s", cambiamentiAssociazione.getId());
                    log.error(errorMessage, ex);
                    throw new MasterjobsWorkerException(errorMessage, ex);
                }
                if(permessiStrutturaSuArchivi != null && !permessiStrutturaSuArchivi.isEmpty()) {
                    permessiStrutturaSuArchivi.stream().forEach(p -> archiviDaPermessizzare.add(p.getOggetto().getIdProvenienza()));
                }
                idCambiamentiAssociazioni.add(cambiamentiAssociazione.getId());
            }
            if (!archiviDaPermessizzare.isEmpty()) {
                log.info("Vado a vedere su quali archivi bisogna ricalcolare i permessi");
                for (Integer archivio : archiviDaPermessizzare) {
                    CalcoloPermessiArchivioJobWorkerData calcoloPermessiArchivioJobWorkerData = new CalcoloPermessiArchivioJobWorkerData(archivio);
                    CalcoloPermessiArchivioJobWorker jobWorker = super.masterjobsObjectsFactory.getJobWorker(CalcoloPermessiArchivioJobWorker.class, calcoloPermessiArchivioJobWorkerData, false);
                  
                    try {
                        super.masterjobsJobsQueuer.queue(jobWorker, null, null, null, false, it.bologna.ausl.model.entities.masterjobs.Set.SetPriority.HIGHEST);
                    } catch (MasterjobsQueuingException ex) {
                        String errorMessage = String.format("Errore nell'accodamento di %s", CalcoloPermessiArchivioJobWorker.class.getSimpleName());
                        log.error(errorMessage, ex);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                    JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
                    jPAQueryFactory
                            .update(QCambiamentiAssociazione.cambiamentiAssociazione)
                            .set(QCambiamentiAssociazione.cambiamentiAssociazione.fatto, true)
                            .where(QCambiamentiAssociazione.cambiamentiAssociazione.id.in(idCambiamentiAssociazioni))
                            .execute();
                    log.info("ho settato il cambiamento associazione come fatto");
                }
            } else {
                log.warn("Non ho trovato nessun archivio su cui devo calcolare i permessi");
            }
            
        }
        log.info(String.format("job %s ended", getName()));

        return null;
    }
    
}
