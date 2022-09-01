package it.bologna.ausl.internauta.service.argo.utils.gd;

import it.bologna.ausl.internauta.service.argo.utils.ArgoConnectionManager;
import it.bologna.ausl.internauta.service.argo.utils.IndeUtils;
import it.bologna.ausl.internauta.service.exceptions.sai.GddocCreationException;
import it.bologna.ausl.internauta.service.exceptions.sai.TooManyObjectsException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
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

    private final String CODICE_GDDOC_TEMPLATE = "babel_suite_id_outbox_[idOutbox]";
    
    private IndeUtils indeUtils;

    @Autowired
    private ArgoConnectionManager connectionManager;

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
        List result = null;
        try  {
            result = connectionManager.queryAndFetcth("select * from gd.gddocs limit 1;", idAzienda);
        } catch (Throwable t) {
            throw new Exception("Errore nel reperire un gddoc", t);
        }
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

    /**
     * Se esiste un gddoc con il codice indentificato dall'idOutbox passato, nell'azienda passata, lo ritorna. Altrimenti torna null
     * @param idAzienda
     * @param idOutbox
     * @return il gddoc con il codice indentificato dall'idOutbox passato, nell'azienda passata, null altrimenti
     * @throws TooManyObjectsException se trova più di un gdDoc
     */
    public Map<String, Object> getGdDocByIdOutbox(Integer idAzienda, Integer idOutbox) throws Exception {
        log.info("Genero una connessione...");
        try (Connection connection = getConnection(idAzienda)) {
            log.info("Genero la query da oggetto connection");
            String query = "select * from gd.gddocs where codice = :codice";
            List<Map<String, Object>> gdDocs = connection.createQuery(query)
                    .addParameter("codice", CODICE_GDDOC_TEMPLATE.replace("[idOutbox]", String.valueOf(idOutbox.intValue())))
                    .executeAndFetchTable().asList();
            if (gdDocs == null) {
                String errorMessage = String.format("La query di ricerca del gdDoc ha tornato null, questo non dovrebbe accadere. IdAzienda %s, idOutbox: %s", idAzienda, idOutbox);
                log.error(errorMessage);
                throw new Exception(errorMessage);
            } else if (gdDocs.isEmpty()) {
                return null;
            } else if (gdDocs.size() > 1) {
                String errorMessage = String.format("trovati %s gdDocs per l'azienda %s e idOutbox %s", gdDocs.size(), idAzienda, idOutbox);
                log.error(errorMessage);
                throw new TooManyObjectsException(errorMessage);
            } else {
                return gdDocs.get(0);
            }
        }
    }
    
    public Map<String, Object> createGddoc(Integer idAzienda, String nome, String tipologiaDocumentale, Integer idOutbox) throws Exception {
        String idGddoc = IndeUtils.generateIndeID();
        UUID guidNuovoGddoc = java.util.UUID.randomUUID();
        Map<String, Object> genericRowFromGddoc = getGenericRowFromGddoc(idAzienda);
        String insertQueryTemplateByGddoc = getInsertQueryTemplateByGddoc(genericRowFromGddoc);

        log.info("Genero una connessione...");
        try (Connection connection = getConnection(idAzienda)) {
            log.info("Genero la query da oggetto connection");
            Query createQuery = connection.createQuery(insertQueryTemplateByGddoc);
            log.info("Ciclo le chiavi (" + genericRowFromGddoc.entrySet().size() + ")");

            for (Map.Entry<String, Object> entry : genericRowFromGddoc.entrySet()) {
                String key = entry.getKey();
                Object val;

                switch (key) {
                    case "id_gddoc":
                        val = idGddoc;
                        break;
                    case "guid_gddoc":
                        val = guidNuovoGddoc;
                        break;
                    case "nome_gddoc":
                        val = nome;
                        break;
                    case "id_documento_origine":
                        val = "babel_suite_" + guidNuovoGddoc;
                        break;
                    case "tipo_gddoc":
                        val = "d";
                        break;
                    case "stato_gd_doc":
                        val = 1;
                        break;
                    case "data_gddoc":
                        val = new Date();
                        break;
                    case "id_oggetto_origine":
                        val = "babel_suite_" + guidNuovoGddoc;
                        break;
                    case "annullato":
                        val = false;
                        break;
                    case "multiplo":
                        val = 0;
                        break;
                    case "codice":
                        // inserisco un codice al gddoc in modo da identificarlo in modo univoco in base all'idOutbox
                        val = CODICE_GDDOC_TEMPLATE.replace("[idOutbox]", String.valueOf(idOutbox.intValue()));
                        break;
                    default:
                        // TODO mancano tutti i campi in cui il gddoc potrebbe essere un pg/dete/deli/registro
                        val = null;
                        break;
                }

                createQuery = createQuery.addParameter(key, val);

            }
            log.info("QUERY INSERT: \n" + createQuery.toString());
            createQuery.executeUpdate();  // c'è l'autocommit
        }

        log.info("Ora ricerco il gddoc con id " + idGddoc);
        Map<String, Object> gddocByIdGddoc = getGddocByIdGddoc(idAzienda, idGddoc);
        log.info(gddocByIdGddoc.toString());
        return gddocByIdGddoc;
    }

}
