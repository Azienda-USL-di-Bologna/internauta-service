package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.ricalcolopermessiarchivi;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessigerarchiaarchivio.*;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.QArchivioInfo;
import java.time.ZonedDateTime;
import java.util.Iterator;
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
        log.info("LimitArchiviUltimoPeriodo" + parametri.getLimitArchiviUltimoPeriodo());
        
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime dataMassimaUltimoRicalcolo = now.minusDays(parametri.getGiorniPerDataMassimaUltimoRicalcolo());
        ZonedDateTime dataMinimaUltimoUtilizzo = now.minusDays(parametri.getGiorniPerDataMinimaUltimoUtilizzo());
        
        QArchivioInfo qArchivioinfo = QArchivioInfo.archivioInfo;
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        
        JPAQuery<Integer> archiviDaRicalcolarePerMaggioreUtilizzo = jPAQueryFactory
                .select(qArchivioinfo.id)
                .from(qArchivioinfo)
                .where(qArchivioinfo.dataUltimoUtilizzo.goe(dataMinimaUltimoUtilizzo)
                        .and(qArchivioinfo.dataUltimoRicalcoloPermessi.loe(dataMassimaUltimoRicalcolo))
                )
                .limit(parametri.getLimitArchiviUltimoPeriodo())
                .fetchAll();
        
        JPAQuery<Integer> archiviDaRicalcolarePerRecupero = jPAQueryFactory
                .select(qArchivioinfo.id)
                .from(qArchivioinfo)
                .orderBy(qArchivioinfo.dataUltimoRicalcoloPermessi.asc())
                .limit(parametri.getNumeroArchiviAggiuntiviDaRecuperare())
                .fetchAll();
        
        log.info("Ora accodo il job per il calcolo di ogni singolo archivio");
        AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
        
        Integer i = 0;
        
        for (Iterator<Integer> a = archiviDaRicalcolarePerMaggioreUtilizzo.iterate(); a.hasNext();) {
            Integer idArchivio = a.next();
            i++;
            accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivio, idArchivio.toString(), "scripta_archivio", null, true);
        }

        log.info("Size di archiviDaRicalcolarePerMaggioreUtilizzo:" + i);
        
        i = 0;
        
        for (Iterator<Integer> a = archiviDaRicalcolarePerRecupero.iterate(); a.hasNext();) {
            Integer idArchivio = a.next();
            i++;
            accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivio, idArchivio.toString(), "scripta_archivio", null, true);
        }
        
        log.info("Size di archiviDaRicalcolarePerRecupero:" + i);

        return null;
    }
    
    public static class RicalcoloPermessiArchiviParams {
        Integer numeroArchiviAggiuntiviDaRecuperare;
        Integer giorniPerDataMinimaUltimoUtilizzo;
        Integer giorniPerDataMassimaUltimoRicalcolo;
        Integer limitArchiviUltimoPeriodo;
        
        public RicalcoloPermessiArchiviParams() {};

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

        public Integer getLimitArchiviUltimoPeriodo() {
            return limitArchiviUltimoPeriodo;
        }

        public void setLimitArchiviUltimoPeriodo(Integer limitArchiviUltimoPeriodo) {
            this.limitArchiviUltimoPeriodo = limitArchiviUltimoPeriodo;
        }
    }
}
