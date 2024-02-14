package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriapermessiveicolati;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.repositories.PermessoRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.ribaltoneutils.QRibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Michele D'Onza
 */@MasterjobsWorker
public class SanatoriaPermessiVeicolatiJobWorker extends JobWorker<SanatoriaPermessiVeicolatiJobWorkerData, JobWorkerResult>{
    private static final Logger log = LoggerFactory.getLogger(SanatoriaPermessiVeicolatiJobWorker.class);
    private final String name = SanatoriaPermessiVeicolatiJobWorker.class.getSimpleName();
    
    @Autowired
    private PermessoRepository permessoRepository;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public boolean isExecutable() {
        SanatoriaPermessiVeicolatiJobWorkerData workerData = getWorkerData();
        if (!workerData.isAspettaRibaltone()){
            return true;
        } else {
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            QRibaltoneDaLanciare qrdl = QRibaltoneDaLanciare.ribaltoneDaLanciare;
            List<RibaltoneDaLanciare> ribaltoniList = queryFactory.select(qrdl).from(qrdl).where(qrdl.stato.eq("LANCIATO")).fetch();
            return ribaltoniList == null || ribaltoniList.isEmpty();
        }
    }
    
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono il " + getName() + " e sto funzionando...");
        permessoRepository.spegniPermessiVeicolatiInvalidi();
        return null;
    }
}
