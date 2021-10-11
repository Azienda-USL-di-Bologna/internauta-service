/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.utils;

import it.bologna.ausl.internauta.service.argo.raccolta.Fascicolo;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;
import org.sql2o.data.Table;

/**
 *
 * @author Salo
 */
@Component
public class FascicoloUtils {

    private static final Logger log = LoggerFactory.getLogger(FascicoloUtils.class);

    @Autowired
    PostgresConnectionManager postgresConnectionManager;

    String QUERY_FIND_FASCICOLO_BY_NUMERAZIONE_GERARCHICA = "select * "
            + "from gd.fascicoligd "
            + "where numerazione_gerarchica = '%s'; ";

    String QUERY_FIND_ID_FASCICOLO_BY_NUMERAZIONE_GERARCHICA = "select id_fascicolo "
            + "from gd.fascicoligd "
            + "where numerazione_gerarchica = '%s'; ";

    private Connection getConnection(Integer idAzienda) throws Exception {
        log.info("Retrieving connection for azienda id " + idAzienda);
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(idAzienda);
        log.info("Returning open connection...");
        return (Connection) dbConnection.open();
    }

    private List queryAndFetcth(String queryString, Connection conn) throws Exception {
        List<Row> rows = null;
        log.info("Creating query object by:\n" + queryString);
        Query query = conn.createQuery(queryString);
        log.info("Execute and fetch....");
        Table table = query.executeAndFetchTable();
        List<Map<String, Object>> asList = table.asList();
        if (asList != null) {
            log.info("Found " + asList.toString());
        } else {
            log.info("No res found!");
        }
        return asList;
    }

    public String getIdFascicoloByPatternInName(Integer idAzienda, String patternLike) throws Exception {
        Connection connection = getConnection(idAzienda);
        String query = "select id_fascicolo "
                + "from gd.fascicoligd "
                + "where nome_fascicolo like '%" + patternLike + "%';";
        List result = (List<Map<String, Object>>) queryAndFetcth(query, connection);
        Map map = (Map) result.get(0);
        String idFascicolo = (String) map.get("id_fascicolo");
        log.info(idFascicolo);
        return idFascicolo;
    }

    public Map<String, Object> getIdFascicoloByPatternInNameAndIdFascicoloPadre(Integer idAzienda, String patternLike, String idFascicoloPadre) throws Exception {
        Connection connection = getConnection(idAzienda);
        String query = "select id_fascicolo "
                + "from gd.fascicoligd "
                + "where id_fascicolo_padre = '" + idFascicoloPadre + "'"
                + "and nome_fascicolo like '%" + patternLike + "%';";
        List result = (List<Map<String, Object>>) queryAndFetcth(query, connection);
        Map fascicolo = (Map) result.get(0);
        return fascicolo;
    }

    public Map<String, Object> getFascicoloByPatternInNameAndIdFascicoloPadre(Integer idAzienda, String patternLike, String idFascicoloPadre) throws Exception {
        Connection connection = getConnection(idAzienda);
        String query = "select * "
                + "from gd.fascicoligd "
                + "where id_fascicolo_padre = '" + idFascicoloPadre + "'"
                + "and nome_fascicolo like '%" + patternLike + "%';";
        List result = (List<Map<String, Object>>) queryAndFetcth(query, connection);
        Map fascicolo = (Map) result.get(0);
        return fascicolo;
    }

    public String getIdFascicoloByNumerazioneGerarchica(Integer idAzienda, String numerazioneGerarchica) throws Exception {
        String queryString = String.format(QUERY_FIND_ID_FASCICOLO_BY_NUMERAZIONE_GERARCHICA, numerazioneGerarchica);
        log.info(queryString);
        Connection conn = getConnection(idAzienda);
        List result = (List<Map<String, Object>>) queryAndFetcth(queryString, conn);
        Map map = (Map) result.get(0);
        String idFascicolo = (String) map.get("id_fascicolo");
        log.info(idFascicolo);
        return idFascicolo;
    }

}
