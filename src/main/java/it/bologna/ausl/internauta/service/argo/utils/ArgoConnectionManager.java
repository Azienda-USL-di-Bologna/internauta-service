/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.utils;

import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;
import org.sql2o.data.Table;

/**
 *
 * @author Salo
 */
@Component
public class ArgoConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ArgoConnectionManager.class);

    @Autowired
    PostgresConnectionManager postgresConnectionManager;

    public Connection getConnection(Integer idAzienda) throws Exception {
        log.info("Retrieving connection for azienda id " + idAzienda);
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(idAzienda);
        log.info("Returning open connection...");
        return (Connection) dbConnection.open();
    }

    public List queryAndFetcth(String queryString, Connection conn) throws Exception {
        List<Map<String, Object>> asList = null;
        try {
            List<Row> rows = null;
            log.info("Creating query object by:\n" + queryString);
            Query query = conn.createQuery(queryString);
            log.info("Execute and fetch....");
            Table table = query.executeAndFetchTable();
            asList = table.asList();
            if (asList != null) {
                log.info("Found " + asList.toString());
            } else {
                log.info("No res found!");
            }
            conn.close();
        } catch (Throwable t) {
            t.printStackTrace();
            conn.rollback();
        }
        return asList != null && asList.size() > 0 ? asList : null;
    }

    public List queryAndFetcth(String queryString, Integer idAzienda) throws Exception {
        List<Map<String, Object>> asList = null;
        Connection conn = getConnection(idAzienda);
        try {
            List<Row> rows = null;
            log.info("Creating query object by:\n" + queryString);
            Query query = conn.createQuery(queryString);
            log.info("Execute and fetch....");
            Table table = query.executeAndFetchTable();
            asList = table.asList();
            if (asList != null) {
                log.info("Found " + asList.toString());
            } else {
                log.info("No res found!");
            }
            conn.close();
        } catch (Throwable t) {
            t.printStackTrace();
            conn.rollback();
        }
        return asList != null && asList.size() > 0 ? asList : null;
    }

    public Connection getTransactionalConnection(Integer idAzienda) throws Exception {
        log.info("Retrieving connection for azienda id " + idAzienda);
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(idAzienda);
        log.info("Return BEGIN connection...");
        return (Connection) dbConnection.beginTransaction();
    }

}
