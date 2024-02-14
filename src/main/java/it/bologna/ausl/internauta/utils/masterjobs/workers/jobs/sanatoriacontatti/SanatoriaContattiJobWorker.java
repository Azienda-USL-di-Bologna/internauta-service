package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriacontatti;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.ribaltoneutils.QRibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import java.util.List;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class SanatoriaContattiJobWorker extends JobWorker<SanatoiaContattiJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(SanatoriaContattiJobWorker.class);
    private final String name = SanatoriaContattiJobWorker.class.getSimpleName();
    
    @Autowired
    private ContattoRepository contattoRespository;
    
    @Autowired
    private EntityManager entityManager;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public boolean isExecutable() {
        SanatoiaContattiJobWorkerData workerData = getWorkerData();
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
        log.info("Inizia il job");
        
        log.info("Aggiorno i contatti struttura");
        contattoRespository.aggiornaContattiStruttura();
        
        log.info("Aggiorno i contatti persona");
        contattoRespository.aggiornaContattiPersona();
        
        log.info("Elimino i protocontatti");
        contattoRespository.eliminaProtocontatti();
        
        return null;
    }
}
