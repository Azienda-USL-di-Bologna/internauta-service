/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Query;

/**
 *
 * @author Salo
 */
@Component
public class GddocUtils {

    private static final Logger log = LoggerFactory.getLogger(GddocUtils.class);

    IndeUtils indeUtils;

    @Autowired
    ArgoConnectionManager connectionManager;

    public Map<String, Object> getGddocByIdGddoc(Integer idAzienda, String idGddoc) throws Exception {
        String query = String.format("select * from gd.gddocs where id_gddoc = '%s'", idGddoc);
        List result = connectionManager.queryAndFetcth(query, idAzienda);
        Map<String, Object> gddoc = result != null && result.size() > 0 ? (Map) result.get(0) : null;
        return gddoc;
    }

    private Connection getConnection(Integer idAzienda) throws Exception {
        return connectionManager.getConnection(idAzienda);
    }

    private Map<String, Object> getGenericRowFromGddoc(Integer idAzienda) throws Exception {
        Connection connection = getConnection(idAzienda);
        List result = (List<Map<String, Object>>) connectionManager.queryAndFetcth("select * from gd.gddocs limit 1;", connection);
        return result != null && result.size() > 0 ? (Map) result.get(0) : null;
    }

    private String getInsertQueryTemplateByGddoc(Map<String, Object> gddocGenericRow) {
        String fields = "";
        String values = "";
        log.info("getInsertQueryTemplateByFascicolo(): ciclo le chiavi di " + gddocGenericRow.toString());
        for (Map.Entry<String, Object> entry : gddocGenericRow.entrySet()) {
            String key = entry.getKey();
            fields += (fields != "" ? ", " : "") + key; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
            values += (values != "" ? ", " : "") + ":" + key; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
        }
        String formatQuery = String.format("insert into gd.gddocs (%s) values (%s);", fields, values);
        log.info("Ritorno query:\n" + formatQuery);
        return formatQuery;
    }

    private String getInsertQueryTemplateByIdAzienda(Integer idAzienda) throws Exception {
        String fields = "";
        String values = "";
        Map<String, Object> gddocGenericRow = getGenericRowFromGddoc(idAzienda);
        log.info("getInsertQueryTemplateByFascicolo(): ciclo le chiavi di " + gddocGenericRow.toString());
        for (Map.Entry<String, Object> entry : gddocGenericRow.entrySet()) {
            String key = entry.getKey();
            fields += (fields != "" ? ", " : "") + key; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
            values += (values != "" ? ", " : "") + ":" + key; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
        }
        String formatQuery = String.format("insert into gd.gddocs (%s) values (%s);", fields, values);
        log.info("Ritorno query:\n" + formatQuery);
        return formatQuery;
    }

    public Map<String, Object> createGddoc(Integer idAzienda, String nome, String tipologiaDocumentale) throws Exception {
        String idGddoc = IndeUtils.generateIndeID();
        UUID guidNuovoGddoc = java.util.UUID.randomUUID();
        Map<String, Object> genericRowFromGddoc = getGenericRowFromGddoc(idAzienda);
        String insertQueryTemplateByGddoc = getInsertQueryTemplateByGddoc(genericRowFromGddoc);

        log.info("Genero una connessione...");
        Connection connection = getConnection(idAzienda);
        log.info("Genero la query da oggetto connection");
        Query createQuery = connection.createQuery(insertQueryTemplateByGddoc);
        log.info("Ciclo le chiavi (" + genericRowFromGddoc.entrySet().size() + ")");

        for (Map.Entry<String, Object> entry : genericRowFromGddoc.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (key.equals("id_gddoc")) {
                val = idGddoc;
            } else if (key.equals("guid_gddoc")) {
                val = guidNuovoGddoc;
            } else if (key.equals("nome_gddoc")) {
                val = nome;
            } else if (key.equals("guid_gddoc")) {
                val = guidNuovoGddoc;
            } else if (key.equals("id_documento_origine")) {
                val = "babel_suite_" + guidNuovoGddoc;
            } else if (key.equals("tipo_gddoc")) {
                val = "d";
            } else if (key.equals("stato_gd_doc")) {
                val = 1;
            } else if (key.equals("data_gddoc")) {
                val = new Date();
            } else if (key.equals("id_oggetto_origine") || key.equals("codice")) {
                val = "babel_suite_" + guidNuovoGddoc;
            } else if (key.equals("annullato")) {
                val = false;
            } else if (key.equals("multiplo")) {
                val = 0;
            } else {

                // mancano tutti i campi in cui il gddoc potrebbe essere un fascicolo
                val = null;
            }

            createQuery = createQuery.addParameter(key, val);

        }
        log.info("QUERY INSERT: \n" + createQuery.toString());
        Connection executeUpdate = createQuery.executeUpdate();  // c'è l'autocommit
        log.info("Ora ricerco il gddoc con id " + idGddoc);
        Map<String, Object> gddocByIdGddoc = getGddocByIdGddoc(idAzienda, idGddoc);
        log.info(gddocByIdGddoc.toString());
        return gddocByIdGddoc;
    }

}
