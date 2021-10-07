/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.utils;

import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;
import org.sql2o.Sql2o;

/**
 *
 * @author Salo
 */
@Component
public class FascicoloDataRetriever {

    @Autowired
    PostgresConnectionManager postgresConnectionManager;

    String QUERY_FIND_FASCICOLO_BY_NUMERAZIONE_GERARCHICA = "select * "
            + "from gd.fascicologd "
            + "where numerazione_gerarchica = '%s'; ";

    public void getFascicoloByNumerazioneGerarchica(Integer idAzienda, String numerazioneGerarchica) {
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(idAzienda);
        String queryString = String.format(QUERY_FIND_FASCICOLO_BY_NUMERAZIONE_GERARCHICA, numerazioneGerarchica);
        Connection conn = (Connection) dbConnection.open();
        Query query = conn.createQuery(queryString);
        ResultSetHandler resultSetHandler = null;
        List executeAndFetch = query.executeAndFetch(resultSetHandler);
    }
}
