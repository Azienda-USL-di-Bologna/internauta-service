package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.eliminaarchiviazioni;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocDetailRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PersonaVedenteRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author conte
 */
@MasterjobsWorker
public class EliminaArchiviazioniJobWorker extends JobWorker<EliminaArchiviazioniJobWorkerData, JobWorkerResult> {

    private static final Logger log = LoggerFactory.getLogger(EliminaArchiviazioniJobWorker.class);
    private final String name = EliminaArchiviazioniJobWorker.class.getSimpleName();

    @Autowired
    private ReporitoryConnectionManager aziendeConnectionManager;
    
    @Autowired
    private PersonaVedenteRepository personaVedenteRepository;
    
    @Autowired
    private  ArchivioDocRepository archivioDocRepository;
    
    @Autowired
    private  AllegatoRepository allegatoRepository;
    
    @Autowired
    private  DocRepository docRepository;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        Integer idAzienda = getWorkerData().getIdAzienda();
        Integer tempoEliminaArchiviazioni = getWorkerData().getTempoEliminaArchiviazioni();
        log.info("sono in do doWork() di {} per l'azienda {}", getName(), idAzienda);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        
        // pesco tutte le righe di archivio_docs eliminate logicamente prima di tot giorni quanto deciso dal paramentro
        Predicate predicatoArchiviDocsDaEliminare = QArchivioDoc.archivioDoc.dataEliminazione.isNotNull()
                        .and(QArchivioDoc.archivioDoc.dataEliminazione.before(ZonedDateTime.now().minusDays(tempoEliminaArchiviazioni)))
                        .and(QArchivioDoc.archivioDoc.idDoc.idAzienda.id.eq(idAzienda));
        Iterable<ArchivioDoc> archiviDocsDaEliminare = transactionTemplate.execute(a -> {
            return archivioDocRepository.findAll(predicatoArchiviDocsDaEliminare);
        });
        Iterator<ArchivioDoc> iteratorArchiviDocsDaEliminare = archiviDocsDaEliminare.iterator();
        
        List<Integer> idDocGiaGestiti = new ArrayList<>();
        if (!iteratorArchiviDocsDaEliminare.hasNext()){
            log.info("non ci sono archivio_docs da eliminare");
        } else {
            log.info("procedo ad eliminare i archivio_docs con doc e allegati se posso");
        }
        // mi ciclo tutti gli archivi_docs che sono stati eliminati prima di tot giorni quanto deciso dal paramentro
        for (ArchivioDoc archivioDoc: archiviDocsDaEliminare){
            // ottengo l'entità del doc sulla quale farò tutto
            Doc docDaTrattare = docRepository.getById(archivioDoc.getIdDoc().getId());
            // inizio cancellando l'archivio_docs in qualsiasi caso in cui abbia già gestito o no il doc
            log.info("elimino l'archivio_docs {} ", archivioDoc.getId());
            transactionTemplate.executeWithoutResult(a -> {
                archivioDocRepository.delete(archivioDoc);
            });
            // controllo di non aver già trattato il doc del archivio_docs appena eliminato
            if (!idDocGiaGestiti.isEmpty() && idDocGiaGestiti.contains(docDaTrattare.getId())){
                log.info("il doc {} è già stato gestito", docDaTrattare.getId());
            } else {
                // nel caso non abbia già trattato il doc procedo a verificare se dovrò eliminarlo oppure no
                try {
                    // ottengo la lista di tutte le rige di archivio_docs ancora valide per il doc selezionato
                    Predicate predicatoAchiviDocsNonEliminati = QArchivioDoc.archivioDoc.idDoc.id.eq(docDaTrattare.getId())
                            .and(QArchivioDoc.archivioDoc.dataEliminazione.isNull()
                                        .or(QArchivioDoc.archivioDoc.dataEliminazione.after(ZonedDateTime.now().minusDays(tempoEliminaArchiviazioni))));
        
                    Iterable<ArchivioDoc> achiviDocsNonEliminati = archivioDocRepository.findAll(predicatoAchiviDocsNonEliminati);
                    Iterator<ArchivioDoc> iteratorAchiviDocsNonEliminati = achiviDocsNonEliminati.iterator();
                    // se non ha altre archiviazioni valide e se è un DOCUMENT_UTENTE elimino il doc
                    if(!iteratorAchiviDocsNonEliminati.hasNext() && docDaTrattare.getTipologia().equals(DocDetailInterface.TipologiaDoc.DOCUMENT_UTENTE)) {
                        // per eliminare il doc devo collegarmi al minio per eliminarne gli allegati
                        log.info("il doc {} non ha archiviazioni ancora valide ed è DOCUMENT_UTENTE, quindi procedo ad eliminarlo", docDaTrattare.getId());
                        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
                        // ottengo tutti gli allegati del doc preso in causa
                        List<Allegato> allegati = docDaTrattare.getAllegati();
                        if (allegati.isEmpty()) {
                            log.info("non ho trovato allegati per il doc {}", docDaTrattare.getId());
                        }
                            
                        // ciclo tutti gli allegati del doc preso in causa
                        for (Allegato allegato : allegati) {
                            List<Allegato.DettaglioAllegato> allTipiDettagliAllegati = new ArrayList<>();
                            if (allegato.getDettagli() != null) {
                                allTipiDettagliAllegati = allegato.getDettagli().getAllTipiDettagliAllegati();
                            }
                            // ciclo tutti i dettagli dell'allegato del doc preso in causa
                            for (Allegato.DettaglioAllegato dettaglioAllegato : allTipiDettagliAllegati) {
                                try {
                                    // elimino il file da minio del dettaglio dell'allegato del doc preso in causa
                                    minIOWrapper.deleteByFileId(dettaglioAllegato.getIdRepository());
                                    log.info("ho eliminato l'allegato {} del doc {} dal minio",allegato.getId(), docDaTrattare.getId());
                                } catch (MinIOWrapperException ex) {
                                    log.error("errore nella delete del'allegato {} del doc {} dal minio",allegato.getId(), docDaTrattare.getId());
                                    // ritorno l'eccezione così da non eliminare la riga sul db dell'allegato del doc preso in causa 
                                    throw new Exception(ex);
                                }
                            }
                            // se seono arrivato qui vuol dire che ho eliminato tutti i file dell'allegato dal db,
                            // ergo posso eliminare la riga dell'allegato
                            log.info("elimino l'allegato {} del doc {} dal db", allegato.getId(), docDaTrattare.getId());
                            transactionTemplate.executeWithoutResult(a -> {
                                allegatoRepository.delete(allegato);
                            });
                        }
                        // arrivato a questo punto sono certo di aver eliminato tutti gli allegati del doc preso in causa ergo posso eliminare pure lui
                        log.info("elimino il doc {} dal db", docDaTrattare.getId());
                        transactionTemplate.executeWithoutResult(a -> {
                            docRepository.deleteById(docDaTrattare.getId());
                        });
                    } else if (!docDaTrattare.getTipologia().equals(DocDetailInterface.TipologiaDoc.DOCUMENT_UTENTE)){
                        log.info("non faccio nulla per il doc {} poiché non è un DOCUMENT_UTENTE", docDaTrattare.getId());
                        personaVedenteRepository.calcolaPersoneVedenti(docDaTrattare.getId());
                    } else {
                        log.info("non faccio nulla per il doc {} poiché ancora usato", docDaTrattare.getId());
                        personaVedenteRepository.calcolaPersoneVedenti(docDaTrattare.getId());
                    }
                    
                } catch (Exception ex) {
                    log.error("errore nella delete dell'archivio_docs ".concat(archivioDoc.getId().toString()), ex );
                }
                // arrivato qui sono certo di aver trattato correttamente il doc,
                // ergo lo inserisco tra quelli già trattati
                idDocGiaGestiti.add(archivioDoc.getIdDoc().getId());
            }
        }
        log.info("job finito");
        return null;
    }
    
    
}
