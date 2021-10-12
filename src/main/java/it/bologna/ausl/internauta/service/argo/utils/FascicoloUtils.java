/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.utils;

import it.bologna.ausl.internauta.service.argo.raccolta.Fascicolo;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
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
    ArgoConnectionManager connectionManager;

    String QUERY_FIND_FASCICOLO_BY_NUMERAZIONE_GERARCHICA = "select * "
            + "from gd.fascicoligd "
            + "where numerazione_gerarchica = '%s'; ";

    String GET_LAST_NUMBER_BY_YEAR_AND_FATHER = "select numero_fascicolo "
            + "from gd.fascicoligd "
            + "where anno_fascicolo = %s and id_fascicolo_padre = '%s' order by numero_fascicolo desc limit 1";

    String QUERY_FIND_ID_FASCICOLO_BY_NUMERAZIONE_GERARCHICA = "select id_fascicolo "
            + "from gd.fascicoligd "
            + "where numerazione_gerarchica = '%s'; ";

    private Connection getConnection(Integer idAzienda) throws Exception {
        return connectionManager.getConnection(idAzienda);
    }

    private Integer getLastNumberFascicolo(Integer idAzienda, Integer anno, String idFascicoloPadre) throws Exception {
        Integer lastNumber = 0;
        String queryString = String.format(GET_LAST_NUMBER_BY_YEAR_AND_FATHER, anno.toString(), idFascicoloPadre);
        Connection connection = getConnection(idAzienda);
        Query query = connection.createQuery(queryString);
        List<Integer> list = query.executeAndFetch(Integer.class);
        if (list.size() > 0) {
            lastNumber = list.get(0);
        }
        return lastNumber;
    }

    private List queryAndFetcth(String queryString, Connection conn) throws Exception {
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

    public String getIdFascicoloByPatternInName(Integer idAzienda, String patternLike) throws Exception {
        Connection connection = getConnection(idAzienda);
        String query = "select id_fascicolo "
                + "from gd.fascicoligd "
                + "where nome_fascicolo like '%" + patternLike + "%';";
        List result = (List<Map<String, Object>>) queryAndFetcth(query, connection);
        Map map = result != null && result.size() > 0 ? (Map) result.get(0) : null;
        String idFascicolo = (String) map.get("id_fascicolo");
        log.info(idFascicolo);
        return idFascicolo;
    }

    public Map<String, Object> getIdFascicoloByPatternInNameAndIdFascicoloPadre(Integer idAzienda, String patternLike, String idFascicoloPadre) throws Exception {
        Connection connection = getConnection(idAzienda);
        String query = "select id_fascicolo "
                + "from gd.fascicoligd "
                + "where id_fascicolo_padre = '" + idFascicoloPadre + "' "
                + "and nome_fascicolo like '%" + patternLike + "%';";
        List result = (List<Map<String, Object>>) queryAndFetcth(query, connection);
        Map fascicolo = result != null && result.size() > 0 ? (Map) result.get(0) : null;
        return fascicolo;
    }

    public Map<String, Object> getFascicoloByPatternInNameAndIdFascicoloPadre(Integer idAzienda, String patternLike, String idFascicoloPadre) throws Exception {
        Connection connection = getConnection(idAzienda);
        String query = "select * "
                + "from gd.fascicoligd "
                + "where id_fascicolo_padre = '" + idFascicoloPadre + "' "
                + "and nome_fascicolo like '%" + patternLike + "%';";
        List result = (List<Map<String, Object>>) queryAndFetcth(query, connection);
        Map fascicolo = result != null && result.size() > 0 ? (Map) result.get(0) : null;
        return fascicolo;
    }

    public String getIdFascicoloByNumerazioneGerarchica(Integer idAzienda, String numerazioneGerarchica) throws Exception {
        String queryString = String.format(QUERY_FIND_ID_FASCICOLO_BY_NUMERAZIONE_GERARCHICA, numerazioneGerarchica);
        log.info(queryString);
        Connection conn = getConnection(idAzienda);
        List result = (List<Map<String, Object>>) queryAndFetcth(queryString, conn);
        Map map = result != null && result.size() > 0 ? (Map) result.get(0) : null;
        String idFascicolo = (String) map.get("id_fascicolo");
        log.info(idFascicolo);
        return idFascicolo;
    }

    public Map<String, Object> getFascicoloByNumerazioneGerarchica(Integer idAzienda, String numerazioneGerarchica) throws Exception {
        String queryString = String.format(QUERY_FIND_FASCICOLO_BY_NUMERAZIONE_GERARCHICA, numerazioneGerarchica);
        log.info(queryString);
        Connection conn = getConnection(idAzienda);
        List result = (List<Map<String, Object>>) queryAndFetcth(queryString, conn);
        Map fascicolo = result != null && result.size() > 0 ? (Map) result.get(0) : null;
        return fascicolo;
    }

    private String getInsertQueryTemplateByFascicolo(Map<String, Object> fascicoloGenericRow) {
        String fields = "";
        String values = "";
        log.info("getInsertQueryTemplateByFascicolo(): ciclo le chiavi di " + fascicoloGenericRow.toString());
        for (Map.Entry<String, Object> entry : fascicoloGenericRow.entrySet()) {
            String key = entry.getKey();
            fields += (fields != "" ? ", " : "") + key; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
            values += (values != "" ? ", " : "") + ":" + key; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
        }
        String formatQuery = String.format("insert into gd.fascicoligd (%s) values (%s) returning *;", fields, values);
        log.info("Ritorno query:\n" + formatQuery);
        return formatQuery;
    }

    private Map<String, Object> getGenericRowFromFascicoligd(Integer idAzienda) throws Exception {
        Connection connection = getConnection(idAzienda);
        List result = (List<Map<String, Object>>) queryAndFetcth("select * from gd.fascicoligd limit 1;", connection);
        return result != null && result.size() > 0 ? (Map) result.get(0) : null;
    }

    private String getInsertQueryTemplateByIdAzienda(Integer idAzienda) throws Exception {
        Map<String, Object> fascicoloGenericRow = getGenericRowFromFascicoligd(idAzienda);
        String fields = "";
        String values = "";
        for (Map.Entry<String, Object> entry : fascicoloGenericRow.entrySet()) {
            String key = entry.getKey();
            fields += (fields != "" ? ", " : "") + key; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
            values += (values != "" ? ", " : "") + ":" + key; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
        }
        return String.format("insert into gd.fascicoligd (%s) values (%s);", fields, values);
    }

    private String generateIndeID() {
        char[] chars = "abcdefghijklmnopqrstuvwxyzQWERTYUIOPASDFGHJKLZXCVBNM1234567890.:,-_?!^|&".toCharArray();
        StringBuilder sb = new StringBuilder(20);
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    public Map<String, Object> createFascicolo(Integer idAzienda, String nomeFascicolo, Map<String, Object> fascicoloPadre) throws Exception {
        log.info("Creazione fascicolo del fascicolo  " + nomeFascicolo + " per azienda " + idAzienda);
        log.info("Fascicolo padre " + fascicoloPadre.toString());

        log.info("Recupero un fascicolo per ciclarmi le chiavi");
        Map<String, Object> templateFascicoloObject = getGenericRowFromFascicoligd(idAzienda);
        log.info("Preparo query per inserimento...");
        String insertQueryTemplateByFascicolo = getInsertQueryTemplateByFascicolo(templateFascicoloObject);
        log.info("Query template generato: " + insertQueryTemplateByFascicolo);
        Map<String, Object> newValues = new HashMap<>();
        log.info("Genero una connessione...");
        Connection connection = getConnection(idAzienda);
        log.info("Genero la query da oggetto connection");
        Query createQuery = connection.createQuery(insertQueryTemplateByFascicolo);
        log.info("Ciclo le chiavi (" + templateFascicoloObject.entrySet().size() + ")");
        UUID nuovoFascicoloGUID = java.util.UUID.randomUUID();
        String nuovoFascicoloId = nuovoFascicoloGUID.toString();
        String nuovoFascicoloNumerazioneGerarchica = null;
        for (Map.Entry<String, Object> entry : templateFascicoloObject.entrySet()) {

            String key = entry.getKey();
            Object val = null;
            if (key.equals("id_fascicolo")) {
                nuovoFascicoloId = generateIndeID();
                val = nuovoFascicoloId;
            } else if (key.equals("id_fascicolo_padre")) {
                val = fascicoloPadre != null ? fascicoloPadre.get("id_fascicolo") : null;
            } else if (key.equals("numero_fascicolo") || key.equals("numerazione_gerarchica")) {
                Integer year = Calendar.getInstance().get(Calendar.YEAR);
                if (fascicoloPadre != null) {
                    year = (Integer) fascicoloPadre.get("anno_fascicolo");
                }
                String idFascicoloPadre = fascicoloPadre != null ? (String) fascicoloPadre.get("id_fascicolo") : null;
                Integer actualMaxNumber = getLastNumberFascicolo(idAzienda, year, idFascicoloPadre);
                Integer newNumber = actualMaxNumber + 1;
                val = newNumber;

                if (key.equals("numerazione_gerarchica")) {
                    if (fascicoloPadre != null) {
                        String numerazioneGerarchica = (String) fascicoloPadre.get("numerazione_gerarchica");
                        int indexOf = numerazioneGerarchica.indexOf("/");
                        val = numerazioneGerarchica.substring(0, indexOf) + "-" + newNumber + numerazioneGerarchica.substring(indexOf);
                    }
                    nuovoFascicoloNumerazioneGerarchica = val.toString();
                }

            } else if (key.equals("id_livello_fascicolo")) {
                if (fascicoloPadre != null) {
                    // se ho un fascicolo padre, e questi ha padre, allora il mio livello è 3, 
                    // se invece non ha padre allora il mio livello è 2
                    val = fascicoloPadre.get("id_fascicolo_padre") != null ? 3 : 2;
                } else {
                    // non ho padre? allora sono di livello 1
                    val = 1;
                }
            } else if (key.equals("nome_fascicolo")) {
                val = nomeFascicolo;
            } else if (key.equals("anno_fascicolo")) {
                val = fascicoloPadre != null ? fascicoloPadre.get(key) : Calendar.getInstance().get(Calendar.YEAR);
            } else if (key.equals("stato_fascicolo")) {
                val = "a";
            } else if (key.equals("id_struttura")) {
                val = fascicoloPadre != null ? fascicoloPadre.get(key) : null;
            } else if (key.equals("data_creazione")) {
                val = new Date();
            } else if (key.equals("tscol") || key.equals("tscol_oggetto") || key.equals("id_iter")
                    || key.equals("id_fascicolo_app_origine") || key.equals("data_chiusura")) {
                val = null;
            } else if (key.equals("codice_fascicolo")) {
                val = "babel_" + nuovoFascicoloGUID.toString();
            } else if (key.equals("guid_fascicolo")) {
                val = nuovoFascicoloGUID.toString();
            } else {
                val = fascicoloPadre != null ? fascicoloPadre.get(key) : null;
            }

            createQuery = createQuery.addParameter(key, val);
        }

        log.info("QUERY INSERT: \n" + createQuery.toString());
        Connection executeUpdate = createQuery.executeUpdate();  // c'è l'autocommit
//        log.info("Committo la transazione...");
//        executeUpdate.commit();
        log.info("Recupero il fascicolo salvato e lo ritorno");
        Map<String, Object> result = getFascicoloByNumerazioneGerarchica(idAzienda, nuovoFascicoloNumerazioneGerarchica);
        return result;
    }

}
