/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.repository;

import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.junit.Assert;

/**
 *
 * @author Salo
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestPersonaExtendedRetrieverRepository {

    @Autowired
    PersonaRepository persRepository;

    @Test
    public void caricaPersoneConUtenteAttivoInAUSLBOLOGNA() {
        System.out.println("caricaPersoneConUtenteAttivoInAUSLBOLOGNA()");
        List<Integer> lista = persRepository.getPersoneAttiveConUtentiAttiviSuAzienda(2);
        Assert.assertTrue("Caricamento fallito", lista != null);
        Assert.assertTrue("Non sono stati trovati utenti", !lista.isEmpty());
    }
}
