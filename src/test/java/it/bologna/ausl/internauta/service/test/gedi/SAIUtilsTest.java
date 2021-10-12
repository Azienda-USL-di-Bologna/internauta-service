/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.gedi;

import it.bologna.ausl.internauta.service.gedi.utils.SAIUtils;
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
public class SAIUtilsTest {

    private static final Logger log = LoggerFactory.getLogger(SAIUtilsTest.class);

    @Autowired
    SAIUtils saiUtils;

    Integer idOutbox, idAzienda;
    String codiceFiscale, mittente, numerazioneGerarchicaDelPadre = null;

    @Test
    public void testProtocollaPec() throws Exception {
        idOutbox = 1;
        idAzienda = 2;
        mittente = "babel.test1@pec.ausl.bologna.it";
        //codiceFiscale = "SLMLNZ85C13A944M";
        codiceFiscale = "SLMLNZ00C13A944M";
        numerazioneGerarchicaDelPadre = "56/2021";
        saiUtils.fascicolaPec(idOutbox, idAzienda, codiceFiscale, mittente, numerazioneGerarchicaDelPadre);
    }

}
