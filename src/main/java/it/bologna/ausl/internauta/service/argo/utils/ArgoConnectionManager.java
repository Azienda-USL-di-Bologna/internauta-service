/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.utils;

import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.argo.utils.ArgoConnectionException;
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

    private Sql2o getPosgresConnection(Integer idAzienda) throws ArgoConnectionException {
        Sql2o dbConnection = null;
        try {
            dbConnection = postgresConnectionManager.getDbConnection(idAzienda);
        } catch (Exception e) {
            throw new ArgoConnectionException("Impossibile stabilire connessione con azienda " + idAzienda, e);
        }
        return dbConnection;
    }

    public Connection getConnection(Integer idAzienda) throws Exception {
        log.info("Retrieving connection for azienda id " + idAzienda);
        Sql2o dbConnection = getPosgresConnection(idAzienda);
        log.info("Returning open connection...");
        Connection openedConnection = null;
        try {
            openedConnection = dbConnection.open();
        } catch (Exception ex) {
            throw new ArgoConnectionException("Impossibile aprire connessione "
                    + "con argo in azienda " + idAzienda, ex);
        }

        return openedConnection;
    }

    public Connection getTransactionalConnection(Integer idAzienda) throws Exception {
        log.info("Retrieving connection for azienda id " + idAzienda);
        Sql2o dbConnection = getPosgresConnection(idAzienda);
        Connection beginTransaction = null;
        try {
            beginTransaction = dbConnection.beginTransaction();
        } catch (Exception ex) {
            throw new ArgoConnectionException("Errore nel begin della connessione", ex);
        }

        log.info("Return BEGIN connection...");
        return beginTransaction;
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
            throw new ArgoConnectionException("Errore nel retrieving dei dati", t);
        }
        return asList != null && asList.size() > 0 ? asList : null;
    }

    public List queryAndFetcth(String queryString, Integer idAzienda) throws Exception {
        List<Map<String, Object>> asList = null;
        Connection conn = getConnection(idAzienda);
        Query query = createQuery(conn, queryString);
        try {
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
            throw new ArgoConnectionException("Errore nel retrieving dei dati", t);
        }
        return asList != null && asList.size() > 0 ? asList : null;
    }

    private Query createQuery(Connection conn, String queryString) throws ArgoConnectionException {
        log.info("Creating query object by:\n" + queryString);
        Query query = null;
        try {
            query = conn.createQuery(queryString);
        } catch (Exception ex) {
            throw new ArgoConnectionException("Errore nella creazione dell'oggetto Query", ex);
        }
        return query;
    }

}
