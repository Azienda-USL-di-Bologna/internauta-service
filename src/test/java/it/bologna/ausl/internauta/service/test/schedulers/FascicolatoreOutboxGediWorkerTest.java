/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.schedulers;

import it.bologna.ausl.internauta.service.schedulers.FascicolatoreOutboxGediLocaleManager;
import it.bologna.ausl.internauta.service.schedulers.workers.gedi.FascicolatoreAutomaticoGediLocaleWorker;
import it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers.FascicolatoreAutomaticoGediParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Salo
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FascicolatoreOutboxGediWorkerTest {

    private static final Logger log = LoggerFactory.getLogger(FascicolatoreOutboxGediWorkerTest.class);

    String cf, mittente, numerazioneGerarchica = null;
    Integer idAzienda = 2;

    @Autowired
    FascicolatoreAutomaticoGediLocaleWorker worker;

    @Test
    public void testaScheduleAutoFascicolazioneOutbox() throws Exception {
        try {
            log.info("Loggo");
            mittente = "babel.test1@pec.ausl.bologna.it";
            cf = "SLMLNZ85C13A944M";
            numerazioneGerarchica = "56-1/2021";
            FascicolatoreAutomaticoGediParams fascicolatoreAutomaticoGediParams = new FascicolatoreAutomaticoGediParams(1, idAzienda, cf, mittente, numerazioneGerarchica);
            worker.setParams(fascicolatoreAutomaticoGediParams);
            worker.run();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
