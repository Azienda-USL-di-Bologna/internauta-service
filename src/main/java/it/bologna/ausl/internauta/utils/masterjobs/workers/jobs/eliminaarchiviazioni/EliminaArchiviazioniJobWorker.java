package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.eliminaarchiviazioni;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author conte
 */
@MasterjobsWorker
public class EliminaArchiviazioniJobWorker extends JobWorker<EliminaArchiviazioniJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(EliminaArchiviazioniJobWorker.class);
    private final String name = EliminaArchiviazioniJobWorker.class.getSimpleName();
    
    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    AllegatoRepository allegatoRepository;
    
    @Autowired
    DocRepository docRepository;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        
        Integer idAzienda = getWorkerData().getIdAzienda();
        Integer tempoEliminaArchiviazioni = getWorkerData().getTempoEliminaArchiviazioni();
        log.info("sono in do doWork() di {} per l'azienda {}", getName(), idAzienda);
        //pesco tutti i doc con almeno un cross eliminato logicamnete
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        List<Integer> idDocsConCrossEliminateLogicamente = jpaQueryFactory
                .select(QArchivioDoc.archivioDoc.idDoc.id).distinct()
                .from(QArchivioDoc.archivioDoc)
                .join(QDoc.doc).on(QDoc.doc.id.eq(QArchivioDoc.archivioDoc.idDoc.id))
                .where(
                        QArchivioDoc.archivioDoc.dataEliminazione.isNotNull().and(QArchivioDoc.archivioDoc.dataEliminazione.before(ZonedDateTime.now().minusDays(tempoEliminaArchiviazioni))),
                        QDoc.doc.tipologia.eq(DocDetailInterface.TipologiaDoc.DOCUMENT.toString()).or(QDoc.doc.tipologia.eq(DocDetailInterface.TipologiaDoc.DOCUMENT_PEC.toString())).or(QDoc.doc.tipologia.eq(DocDetailInterface.TipologiaDoc.DOCUMENT_UTENTE.toString())),
                        QDoc.doc.idAzienda.id.eq(idAzienda)
                )
                .fetch();
        //pesco tutti i doc //pesco tutti i doc con almeno un cross eliminato logicamnete e anche almeo uno non eliminato
        List<Integer> idDocsConCrossEliminateLogicamenteEAncheNonEliminate = jpaQueryFactory
                .select(QArchivioDoc.archivioDoc.idDoc.id).distinct()
                .from(QArchivioDoc.archivioDoc)
                .where(
                        QArchivioDoc.archivioDoc.idDoc.id.in(idDocsConCrossEliminateLogicamente),
                        QArchivioDoc.archivioDoc.dataEliminazione.isNull()
                )
                .fetch();
        //faccio la sottrazione tra  due risultati ottentendo così solo quelli che hanno solo cross eliminate logicamnete
        idDocsConCrossEliminateLogicamente.removeAll(idDocsConCrossEliminateLogicamenteEAncheNonEliminate);
        //elimino tutte le cross di doc aventi solo cross logicamente elimininate 
        jpaQueryFactory
                .delete(QArchivioDoc.archivioDoc)
                .where(QArchivioDoc.archivioDoc.idDoc.id.in(idDocsConCrossEliminateLogicamente))
                .execute();
        //pesco tra i doc precedentemente pescati quelli che non derivno dalle pec (quindi per esclusione solo quelli caricati dall'utente) 
        List<Integer> idDocsConCrossEliminateLogicamenteNonPec = jpaQueryFactory
                .select(QDoc.doc.id)
                .from(QDoc.doc)
                .where(
                        QDoc.doc.id.in(idDocsConCrossEliminateLogicamente),
                        QDoc.doc.tipologia.ne(DocDetailInterface.TipologiaDoc.DOCUMENT_PEC.toString())
                ).fetch();
        //mi connetto al minio e elimino i file dei dec non pec pescati in precedenza ciclando per ogni doc i suoi allegti e per ogni allegato i suoi dettagli 
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        for(Integer idDoc: idDocsConCrossEliminateLogicamenteNonPec){
            Doc doc = docRepository.getById(idDoc);
            List<Allegato> allegati = jpaQueryFactory
                .select(QAllegato.allegato)
                .from(QAllegato.allegato)
                .where(
                        QAllegato.allegato.idDoc.id.eq(doc.getId())
                ).fetch();
            for(Allegato allegato: allegati){
                List<Allegato.DettaglioAllegato> allTipiDettagliAllegati = new ArrayList<Allegato.DettaglioAllegato>();
                if (allegato.getDettagli() != null){
                    allTipiDettagliAllegati = allegato.getDettagli().getAllTipiDettagliAllegati();
                }
                for(Allegato.DettaglioAllegato dettaglioAllegato: allTipiDettagliAllegati){
                    try {
                        minIOWrapper.deleteByFileId(dettaglioAllegato.getIdRepository());
                    } catch (MinIOWrapperException ex) {
                        log.error("errore nella delete del file dal minio", ex);
                    }
                }
            }
        }
        //rimuovo gli allegati in base ai doc
        jpaQueryFactory
                .delete(QAllegato.allegato)
                .where(QAllegato.allegato.idDoc.id.in(idDocsConCrossEliminateLogicamenteNonPec))
                .execute();
        //rimuovo i doc
        jpaQueryFactory
                .delete(QDoc.doc)
                .where(
                        QDoc.doc.id.in(idDocsConCrossEliminateLogicamenteNonPec),
                        QDoc.doc.idAzienda.id.eq(idAzienda)
                ).execute();
   
        return null;
    }
}
