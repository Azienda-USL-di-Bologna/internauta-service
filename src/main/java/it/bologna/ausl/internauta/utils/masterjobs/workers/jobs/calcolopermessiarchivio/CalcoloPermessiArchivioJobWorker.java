package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PersonaVedenteRepository;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QArchivioInfo;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    
    @Autowired
    private PersonaVedenteRepository personaVedenteRepository;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");

        CalcoloPermessiArchivioJobWorkerData data = getWorkerData();
        log.info("Calcolo permessi archivio: " + data.getIdArchivio().toString());
        
        Boolean queueJobCalcolaPersoneVedentiDoc = data.getQueueJobCalcolaPersoneVedentiDoc();
        
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        
        List<Integer> idPersoneConPermessoPrima = null;
        List<Integer> idPersoneConPermessoDopo;
        
        try {           
            // Se devo calcolare le persone vedenti faccio quest'operazione il cui scopo è spiegato sotto
            if (queueJobCalcolaPersoneVedentiDoc) {
                idPersoneConPermessoPrima = estraiPermessiEspliciti(data.getIdArchivio());
            }
            
            archivioRepository.calcolaPermessiEspliciti(data.getIdArchivio());
        } catch (Exception ex){
           String errore = "Errore nel calcolo dei permessi espliciti dello archivio";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }
        
        /*
            Voglio andare a ricalcolare le persone vedenti solo se è necessario, quindi, 
            dato che ho sia chi aveva i permessi prima che dopo, posso avere tre possibilità:
            1- ora ho più persone quindi devo andare ad aggiungere le persone che hanno ottenuto un permesso, direttamente nelle persone vedenti
            2- ora ho meno persone quindi qualcuno ha perso il permesso allora vado a ricalcolare tutto perché se una persona non ha il permesso non è detto che non lo veda
            3- se i set sono uguali non faccio nulla
        */
        if (queueJobCalcolaPersoneVedentiDoc) {
            log.info("Calcolo delle persone vedenti");

            QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
            QDocDetail qDocDetail = QDocDetail.docDetail;
            // Prendo tutti i doc fascicolati nell'archivio
            // Il permesso sul fascicolo non è sufficiente a vedere i doc non registrati
            List<Integer> idDocsDaArchivio = jpaQueryFactory
                    .select(qArchivioDoc.idDoc.id)
                    .from(qArchivioDoc)
                    .join(qDocDetail).on(qDocDetail.id.eq(qArchivioDoc.idDoc.id))
                    .where(qArchivioDoc.idArchivio.id.eq(data.getIdArchivio())
                            .and(qDocDetail.numeroRegistrazione.isNotNull()))
                    .fetch();
            log.info("idDocsDaArchivi calcolati");
            
            if (idDocsDaArchivio != null && !idDocsDaArchivio.isEmpty()) {
                // Riprendo le persone che hanno permesso, che nel frattempo possono essere cambiate.
                idPersoneConPermessoDopo = estraiPermessiEspliciti(data.getIdArchivio());

                // Calcolo delle liste che mi dicono se ho perso/acquisito nuovi permessi o se tutto rimane com'è.
                List<Integer> soloInSetPrima = new ArrayList<>(idPersoneConPermessoPrima);
                soloInSetPrima.removeAll(idPersoneConPermessoDopo);

                List<Integer> soloInSetDopo = new ArrayList<>(idPersoneConPermessoDopo);
                soloInSetDopo.removeAll(idPersoneConPermessoPrima);

                if (!soloInSetDopo.isEmpty() && soloInSetPrima.isEmpty()) {    
                    // CASO 1: considero solo il caso si aggiungono persone che devono vedere il doc, se soloinsetprima è pieno devo comunque ricalcolare tutto
                    log.info("Devo aggiungere delle persone vedenti");

                    // Trasformo in stringa di integer sennò non va la query
                    String idsPersoneString = "";
                    for (Integer num : soloInSetDopo) {
                        idsPersoneString += num + ",";
                    }
                    // Rimuovi l'ultima virgola
                    if (!idsPersoneString.isEmpty()) {
                        idsPersoneString = idsPersoneString.substring(0, idsPersoneString.length() - 1);
                    }
                    personaVedenteRepository.insertPersoneVedentiMassiva(idDocsDaArchivio, idsPersoneString);

                } else if (!soloInSetPrima.isEmpty() ) {
                    // CASO 2: in questo caso vanno ricalcolati tutti 
                    log.info("Devo ricalcolare le persone vedenti dei documenti contenuti nell'archivio. Inserisco i job");

                    AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
                    for (Integer idDoc : idDocsDaArchivio) {
                        accodatoreVeloce.accodaCalcolaPersoneVedentiDoc(idDoc, idDoc.toString(), "scripta_doc", null);
                    }
                } else {
                    // CASO 3:  NON FACCIO NULLA
                    log.info("Non sono cambiati i permessi sugli archivi, non ricalcolo le persone vedenti");
                }
            } else {
                log.info("Non ci sono documenti nell'archivio, non devo ricalcolare le persone vedenti");
            }
        }
        
        log.info("Aggiorno la data di ultirmo ricalcolo permessi sullo archivio info");
        jpaQueryFactory
                .update(QArchivioInfo.archivioInfo)
                .set(QArchivioInfo.archivioInfo.dataUltimoRicalcoloPermessi, ZonedDateTime.now())
                .where(QArchivioInfo.archivioInfo.id.eq(data.getIdArchivio()))
                .execute();
        
        return null;
    }
    
    /**
     * Ritorna tutti gli idPersona che hanno un permesso di almeno VISUALIZZA
     * (bit > 1) sull'archivio passato come parametro
     * @param idArchivio
     * @return 
     */
    private List<Integer> estraiPermessiEspliciti(Integer idArchivio){
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        QPermessoArchivio qPermessoArchivio = QPermessoArchivio.permessoArchivio;
        QArchivio qArchivio = QArchivio.archivio;
        List<Integer> permessiAttuali = jpaQueryFactory.
                    select(qPermessoArchivio.idPersona.id)
                    .from(qPermessoArchivio)
                    .join(qArchivio)
                        .on(
                            qArchivio.id.eq(qPermessoArchivio.idArchivioDetail.id)
                            .and(qArchivio.dataCreazione.eq(qPermessoArchivio.dataCreazione)
                            .and(qArchivio.idAzienda.id.eq(qPermessoArchivio.idAzienda.id)))
                        )
                    .where(qPermessoArchivio.idArchivioDetail.id.eq(idArchivio)
                            .and(qPermessoArchivio.bit.gt(1)))
                    .fetch(); 
        return permessiAttuali;
    }
}
