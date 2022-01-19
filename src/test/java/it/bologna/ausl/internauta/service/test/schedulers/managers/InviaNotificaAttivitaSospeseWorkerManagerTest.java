/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.schedulers.managers;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ParametroAziendeRepository;
import it.bologna.ausl.internauta.service.schedulers.managers.InviaNotificaAttivitaSospeseWorkerManager;
import it.bologna.ausl.internauta.service.schedulers.workers.InviaNotificaAttivitaSospeseWorker;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.internauta.service.utils.SimpleMailSenderUtility;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Salo
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest
public class InviaNotificaAttivitaSospeseWorkerManagerTest {

    @Autowired
    InviaNotificaAttivitaSospeseWorkerManager manager;
    @Autowired
    InviaNotificaAttivitaSospeseWorker worker;
    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    ParametroAziendeRepository parametroAziendeRepository;
    @Autowired
    SimpleMailSenderUtility simpleMailSenderUtility;

    @Value("${internauta.mode}")
    String internautaMode;

    @Value("${internauta.scheduled.invio-mail-notifica-attivita-sospese.enabled-emails-test}")
    String[] enabledEmailsForTest;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    InternautaUtils internautaUtils;

    //@Test
    public void testDiAvvio() {
        // TEST SE VANNO FATTI

    }
}
