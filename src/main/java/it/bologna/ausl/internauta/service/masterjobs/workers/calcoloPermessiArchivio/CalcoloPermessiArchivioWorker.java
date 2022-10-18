/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.service.masterjobs.workers.calcoloPermessiArchivio;

import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.service.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.service.masterjobs.workers.foo.FooWorker;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CalcoloPermessiArchivioWorker extends Worker{
    private static final Logger log = LoggerFactory.getLogger(FooWorker.class);
    private String name = "CalcoloPermessiArchivioWorker";
    
    @Autowired
    private ArchivioRepository archivioRepository;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected WorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono inizio a lavorare");

        CalcoloPermessiArchivioWorkerData data = (CalcoloPermessiArchivioWorkerData) getData();
        
        try {
            archivioRepository.calcolaPermessiEspliciti(data.getIdArchivio());
        }catch (Exception ex){
           String errore ="Errore nel calcolo dei permessi espliciti degli archivi";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }
        
        return null;
         
    }
}
