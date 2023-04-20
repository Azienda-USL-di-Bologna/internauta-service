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
        log.info("sono in do doWork() di " + getName());
        
        Integer idAzienda = getWorkerData().getIdAzienda();
        Integer tempoEliminaArchiviazioni = getWorkerData().getTempoEliminaArchiviazioni();
        
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

        List<Integer> idDocsConCrossEliminateLogicamenteEAncheNonEliminate = jpaQueryFactory
                .select(QArchivioDoc.archivioDoc.idDoc.id).distinct()
                .from(QArchivioDoc.archivioDoc)
                .where(
                        QArchivioDoc.archivioDoc.idDoc.id.in(idDocsConCrossEliminateLogicamente),
                        QArchivioDoc.archivioDoc.dataEliminazione.isNull()
                )
                .fetch();
        
        idDocsConCrossEliminateLogicamente.removeAll(idDocsConCrossEliminateLogicamenteEAncheNonEliminate);
        
        jpaQueryFactory
                .delete(QArchivioDoc.archivioDoc)
                .where(QArchivioDoc.archivioDoc.idDoc.id.in(idDocsConCrossEliminateLogicamente));
        
        List<Integer> idDocsConCrossEliminateLogicamenteNonPec = jpaQueryFactory
                .select(QDoc.doc.id)
                .from(QDoc.doc)
                .where(
                        QDoc.doc.id.in(idDocsConCrossEliminateLogicamente),
                        QDoc.doc.tipologia.ne(DocDetailInterface.TipologiaDoc.DOCUMENT_PEC.toString())
                ).fetch();
        
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        for(Integer idDoc: idDocsConCrossEliminateLogicamenteNonPec){
            Doc doc = docRepository.getById(idDoc);
            List<Allegato> allegati = allegatoRepository.findByIdDoc(doc);
            for(Allegato allegato: allegati){
                List<Allegato.DettaglioAllegato> allTipiDettagliAllegati = allegato.getDettagli().getAllTipiDettagliAllegati();
                for(Allegato.DettaglioAllegato dettaglioAllegato: allTipiDettagliAllegati){
                    try {
                        minIOWrapper.deleteByFileUuid(dettaglioAllegato.getIdRepository());
                    } catch (MinIOWrapperException ex) {
                        java.util.logging.Logger.getLogger(EliminaArchiviazioniJobWorker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        
        jpaQueryFactory
                .delete(QAllegato.allegato)
                .where(
                        QAllegato.allegato.idDoc.id.in(idDocsConCrossEliminateLogicamenteNonPec)
                );
        
        jpaQueryFactory
                .delete(QDoc.doc)
                .where(
                        QDoc.doc.id.in(idDocsConCrossEliminateLogicamenteNonPec),
                        QDoc.doc.idAzienda.id.eq(idAzienda)
                );
   
        return null;
    }
}
