/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.utils.gd;

import it.bologna.ausl.internauta.service.argo.utils.ArgoConnectionManager;
import it.bologna.ausl.internauta.service.argo.utils.IndeUtils;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
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
public class SottoDocumentiUtils {

    @Autowired
    ArgoConnectionManager argoConnectionManager;

    private static final Logger log = LoggerFactory.getLogger(SottoDocumentiUtils.class);

    public List<String> getTableFieldsName(Integer idAzienda) throws Exception {
        List<String> fields = null;
        String query = "SELECT column_name\n"
                + "  FROM information_schema.columns\n"
                + " WHERE table_schema = 'gd'\n"
                + "   AND table_name   = 'sotto_documenti';";
        try (Connection connection = argoConnectionManager.getConnection(idAzienda)) {
            Query createQuery = connection.createQuery(query);
            fields = createQuery.executeAndFetch(String.class);
        } catch (Exception e) {
            throw new Exception("Errore nel reperire i nomi dei campi del sottodocumento", e);
        }
        return fields;
    }

    private String getInsertQueryTemplateByIdAzienda(Integer idAzienda) throws Exception {
        List<String> tableFieldsName = getTableFieldsName(idAzienda);
        String fields = "";
        String values = "";
        for (String field : tableFieldsName) {

            fields += (fields != "" ? ", " : "") + field; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
            values += (values != "" ? ", " : "") + ":" + field; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
        }
        return String.format("insert into gd.sotto_documenti (%s) values (%s);", fields, values);
    }

    public Map<String, Object> createSottoDocumento(Integer idAzienda,
            String idGddoc,
            MinIOWrapperFileInfo fileInfo,
            String tipoDocumento) throws Exception {
        Map sottoDocumento = null;
        String idSottoDocumento = IndeUtils.generateIndeID();
        try (Connection conn = argoConnectionManager.getConnection(idAzienda)) {
            String insertQueryString = getInsertQueryTemplateByIdAzienda(idAzienda);
            List<String> tableFieldsName = getTableFieldsName(idAzienda);
            Query createQuery = conn.createQuery(insertQueryString);
            for (String field : tableFieldsName) {
                Object val = null;
                if (field.equals("id_sottodocumento")) {
                    val = idSottoDocumento;
                } else if (field.equals("id_gddoc")) {
                    val = idGddoc;
                } else if (field.equals("nome_sottodocumento")) {
                    val = fileInfo.getFileName();
                } else if (field.equals("uuid_mongo_originale")) {
                    val = fileInfo.getMongoUuid();
                } else if (field.equals("guid_sottodocumento")) {
                    val = java.util.UUID.randomUUID();
                } else if (field.equals("dimensione_originale")) {
                    val = fileInfo.getSize();
                } else if (field.equals("convertibile_pdf")) {
                    val = 0;
                } else if (field.equals("principale")) {
                    val = 0;
                } else if (field.equals("tipo_sottodocumento")) {
                    val = tipoDocumento;
                } else if (field.equals("da_spedire_pecgw")) {
                    val = 0;
                } else if (field.equals("spedisci_originale_pecgw")) {
                    val = 0;
                } else if (field.equals("pubblicazione_albo")) {
                    val = 0;
                }

                createQuery = createQuery.addParameter(field, val);
            }
            log.info("QUERY INSERT: \n" + createQuery.toString());
            createQuery.executeUpdate();  // c'è l'autocommit

            log.info("Ora ricerco il gddoc con id " + idGddoc);
            sottoDocumento = getSottoDocumentoByIdSottoDocumento(idAzienda, idSottoDocumento);
            log.info(sottoDocumento.toString());
        }
        return sottoDocumento;
    }

    private Map<String, Object> getSottoDocumentoByIdSottoDocumento(Integer idAzienda, String isSottoDocumento) throws Exception {
        List results = argoConnectionManager.queryAndFetcth("select * from gd.sotto_documenti "
                + "where id_sottodocumento = '" + isSottoDocumento + "'", idAzienda);
        return (Map<String, Object>) results.get(0);
    }
}
