package it.bologna.ausl.internauta.service.argo.utils.gd;

import it.bologna.ausl.internauta.service.argo.utils.ArgoConnectionManager;
import it.bologna.ausl.internauta.service.argo.utils.IndeUtils;
import it.bologna.ausl.internauta.service.exceptions.argo.utils.ArgoConnectionException;
import it.bologna.ausl.internauta.service.exceptions.sai.SottoDocumentoNotFoundException;
import it.bologna.ausl.internauta.service.exceptions.sai.TooManyObjectsException;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import java.util.List;
import java.util.Map;
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
    
    private final String CODICE_SOTTODOCUMENTO_TEMPLATE = "babel_suite_id_outbox_[idOutbox]";
    private static final Logger log = LoggerFactory.getLogger(SottoDocumentiUtils.class);
    
    @Autowired
    ArgoConnectionManager argoConnectionManager;

    public List<String> getTableFieldsName(Integer idAzienda) throws ArgoConnectionException {
        List<String> fields = null;
        String query = "SELECT column_name\n"
                + "  FROM information_schema.columns\n"
                + " WHERE table_schema = 'gd'\n"
                + "   AND table_name   = 'sotto_documenti';";
        try (Connection connection = argoConnectionManager.getConnection(idAzienda)) {
            Query createQuery = connection.createQuery(query);
            fields = createQuery.executeAndFetch(String.class);
        }
        return fields;
    }

    private String getInsertQueryTemplateByIdAzienda(Integer idAzienda) throws ArgoConnectionException {
        List<String> tableFieldsName = getTableFieldsName(idAzienda);
        String fields = "";
        String values = "";
        for (String field : tableFieldsName) {

            fields += (!"".equals(fields) ? ", " : "") + field; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
            values += (!"".equals(values) ? ", " : "") + ":" + field; // cioè se la string non è vuota accoda una virgola poi scrivi comunque il campo
        }
        return String.format("insert into gd.sotto_documenti (%s) values (%s);", fields, values);
    }

    public Map<String, Object> createSottoDocumento(Integer idAzienda,
            String idGddoc,
            MinIOWrapperFileInfo fileInfo,
            String tipoDocumento,
            Integer idOutbox) throws ArgoConnectionException {
        Map sottoDocumento;
        String idSottoDocumento = IndeUtils.generateIndeID();
        try (Connection conn = argoConnectionManager.getConnection(idAzienda)) {
            String insertQueryString = getInsertQueryTemplateByIdAzienda(idAzienda);
            List<String> tableFieldsName = getTableFieldsName(idAzienda);
            Query createQuery = conn.createQuery(insertQueryString);
            for (String field : tableFieldsName) {
                Object val = null;
                switch (field) {
                    case "id_sottodocumento":
                        val = idSottoDocumento;
                        break;
                    case "id_gddoc":
                        val = idGddoc;
                        break;
                    case "nome_sottodocumento":
                        val = fileInfo.getFileName();
                        break;
                    case "uuid_mongo_originale":
                        val = fileInfo.getMongoUuid();
                        break;
                    case "guid_sottodocumento":
                        val = java.util.UUID.randomUUID();
                        break;
                    case "dimensione_originale":
                        val = fileInfo.getSize();
                        break;
                    case "tipo_sottodocumento":
                        val = tipoDocumento;
                        break;
                    case "codice_sottodocumento":
                        val = CODICE_SOTTODOCUMENTO_TEMPLATE.replace("[idOutbox]", String.valueOf(idOutbox.intValue()));
                        break;
                    case "convertibile_pdf":
                    case "principale":
                    case "da_spedire_pecgw":
                    case "spedisci_originale_pecgw":
                    case "pubblicazione_albo":
                        val = 0;
                        break;
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

    /**
     * Torna il sottodocumento identificato dall'idOutbox passato nell'azienda passata, oppure null se non esiste.
     * @param idAzienda
     * @param idOutbox
     * @return il sottodocumento identificato dall'idOutbox passato nell'azienda passata, oppure null se non esiste.
     * @throws TooManyObjectsException se trova più di un sottodocumento
     */
    public Map<String, Object> getSottoDocumentoByIdOutbox(Integer idAzienda, Integer idOutbox) throws SottoDocumentoNotFoundException, TooManyObjectsException, ArgoConnectionException {
        List<Map<String, Object>> sottodocumenti;
        sottodocumenti = getSottoDocumentoByCodice(idAzienda, CODICE_SOTTODOCUMENTO_TEMPLATE.replace("[idOutbox]", String.valueOf(idOutbox.intValue())));
        if (sottodocumenti == null) {
            String errorMessage = String.format("La query di ricerca del sottodocumento ha tornato null, questo non dovrebbe accadere. IdAzienda %s, idOutbox: %s", idAzienda, idOutbox);
            log.error(errorMessage);
            throw new SottoDocumentoNotFoundException(errorMessage);
        } else if (sottodocumenti.size() > 1) {
            String errorMessage = String.format("trovati %s sottodocumenti per l'azienda %s e id %s", sottodocumenti.size(), idAzienda, idOutbox);
            log.error(errorMessage);
            throw new TooManyObjectsException(errorMessage);
        } else if (sottodocumenti.isEmpty()) {
            return null;
        } else {
            return sottodocumenti.get(0);
        }
    }
    
    public List<Map<String, Object>> getSottoDocumentoByCodice(Integer idAzienda, String codice) throws ArgoConnectionException {
        String query = "select * from gd.sotto_documenti where codice_sottodocumento = :codice_sottodocumento";
        List<Map<String, Object>> sottodocumenti;
        try (Connection conn = argoConnectionManager.getConnection(idAzienda)) {
            sottodocumenti = conn.createQuery(query)
                    .addParameter("codice_sottodocumento", codice)
                    .executeAndFetchTable().asList();
        }
        return sottodocumenti;
    }
    
    private Map<String, Object> getSottoDocumentoByIdSottoDocumento(Integer idAzienda, String idSottoDocumento) throws ArgoConnectionException {
        List results = argoConnectionManager.queryAndFetcth("select * from gd.sotto_documenti "
                + "where id_sottodocumento = '" + idSottoDocumento + "'", idAzienda);
        return (Map<String, Object>) results.get(0);
    }
}
