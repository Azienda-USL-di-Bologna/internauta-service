/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.aggiustatori.managers;

import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.schedulers.managers.InviaNotificaAttivitaSospeseWorkerManager;
import it.bologna.ausl.internauta.service.schedulers.workers.InviaNotificaAttivitaSospeseWorker;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Salo
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class InviaNotificaAttivitaSospeseWorkerManagerTest {

    @Autowired
    InviaNotificaAttivitaSospeseWorkerManager manager;
    @Autowired
    InviaNotificaAttivitaSospeseWorker worker;
    @Autowired
    PersonaRepository personaRepository;

    //@Test
    public void testDiAvvio() {
        System.out.println("TEST 1: AVVIO");
//        List<Integer> personeAttiveConUtentiAttiviSuAzienda = personaRepository.getPersoneAttiveConUtentiAttiviSuAzienda(2);
//        System.out.println(personeAttiveConUtentiAttiviSuAzienda);
        //worker.setParameter(new ArrayList<Integer>(), 2);
        manager.run();
        manager.run();

    }
}
