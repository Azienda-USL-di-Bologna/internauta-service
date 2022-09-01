/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.configuration.utils;

import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
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
public class PostgresConnectionManagerTest {

    Integer idAzienda = 2;

    @Autowired
    PostgresConnectionManager postgresConnectionManager;

    @Autowired
    AziendaRepository aziendaReposistory;

    @Test
    public void testPostgresConnectionMangerByCodiceAzienda() {
        Azienda a = aziendaReposistory.findById(idAzienda).get();
        postgresConnectionManager.getDbConnection(a.getCodice());
    }

    @Test
    public void testPostgresConnectionMangerByIdAzienda() {
        postgresConnectionManager.getDbConnection(idAzienda);
    }

}
