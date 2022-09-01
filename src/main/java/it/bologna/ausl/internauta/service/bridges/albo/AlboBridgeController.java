package it.bologna.ausl.internauta.service.bridges.albo;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.argo.utils.gd.SottoDocumentiUtils;
import it.bologna.ausl.internauta.service.bridges.albo.exceptions.AlboBridgeException;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.MasterChefUtils;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${bridges.mapping.url.albo}")
public class AlboBridgeController implements ControllerHandledExceptions {

    private static final Logger LOG = LoggerFactory.getLogger(AlboBridgeController.class);

    private final String RELATE_FILENAME_TEMPLATE = "relata_[numeroPubblicazione]_[annoPubblicazione]_[registro][numeroRegistro]_[annoRegistro].pdf";
    private final String RELATE_TEMPLATE_NAME = "[codiceAzienda]_relata.xhtml";
    private final String RELATE_MINIO_PATH = "/relate";
    private final String RELATE_BUCKET_NAME = "albi";

    private final boolean gediInternauta = false;

    @Autowired
    private MasterChefUtils masterChefUtils;

    @Autowired
    private ReporitoryConnectionManager reporitoryConnectionManager;

    @Autowired
    private SottoDocumentiUtils sottoDocumentiUtils;

    @Autowired
    private CachedEntities cachedEntities;

    @Autowired
    private ObjectMapper objectMapper;

    public static enum TipiAllegato {
        STAMPA_UNICA, RELATA
    }

    /**
     * Torna lo stream dell'allegato richiesto
     * @param azienda il codice azienda (es, 105, 106, 109, ecc.)
     * @param codice il codice del file da reperire, nel caso di relata è il codice tornalto dalla makeAndSaveRelata
     * @param tipo il tipo di allegato (es. STAMPA_UNICA, RELATA)
     * @param forceDownload se "true" viene settato l'header Content-disposition con "attachment;filename=..." per poter forzare il download
     * @param request
     * @param response
     * @throws AlboBridgeException in caso di eccezione gestita con message raprpesentante l'errore rilevato
     */
    @RequestMapping(value = {"getAllegato"}, method = RequestMethod.GET)
    public void getAllegato(
            @RequestParam(required = true) String azienda,
            @RequestParam(required = true) String codice,
            @RequestParam(required = true) TipiAllegato tipo,
            @RequestParam(required = false, defaultValue = "false") Boolean forceDownload,
            HttpServletRequest request,
            HttpServletResponse response) throws AlboBridgeException {

        InputStream fileToSend = null;
        String errorMessage = null;

        Azienda aziendaObj = cachedEntities.getAziendaFromCodice(azienda);
        MinIOWrapper minIOWrapper = this.reporitoryConnectionManager.getMinIOWrapper();
        String repositoryFileId = null;
        String mimeType = null;
        String fileName = null;
        if (gediInternauta) {
            //TODO: da fare
        } else {
            switch (tipo) {
                case STAMPA_UNICA:
                    List<Map<String, Object>> sottoDocumenti = null;
                    try {
                        sottoDocumenti = sottoDocumentiUtils.getSottoDocumentoByCodice(aziendaObj.getId(), codice);
                    } catch (Exception ex) {
                        throw new AlboBridgeException(String.format("errore nel reperimento del sottodocumento gedi per l'azienda %s, con il codice %s di tipo %s", azienda, codice, tipo));
                    }
                    if (sottoDocumenti == null || sottoDocumenti.isEmpty()) {
                        throw new AlboBridgeException(String.format("sottodocumento non trovato in gedi per l'azienda %s, con il codice %s di tipo %s", azienda, codice, tipo));
                    } else if (sottoDocumenti.size() > 1) {
                        throw new AlboBridgeException(String.format("trovato più di un sottodocumento in gedi per l'azienda %s, con il codice %s di tipo %s", azienda, codice, tipo));
                    }
                    repositoryFileId = (String) sottoDocumenti.get(0).get("uuid_mongo_pdf");
                    mimeType = "application/pdf";
                    if (!StringUtils.hasText(repositoryFileId)) {
                        repositoryFileId = (String) sottoDocumenti.get(0).get("uuid_mongo_originale");
                        mimeType = (String) sottoDocumenti.get(0).get("mimetype_file_originale");
                    }

                    try {
                        fileName = minIOWrapper.getFileInfoByUuid(repositoryFileId).getFileName();
                    } catch (MinIOWrapperException ex) {
                        errorMessage = String.format("errore nel reperimento del nome del file da minIO con mongoUuid %s", repositoryFileId);
                        throw new AlboBridgeException(errorMessage);
                    }
                    break;

                case RELATA:
                    try {
                        // le relate sono memorizzate nel bucket albi di minio, ma nella tabela repo.files come azienda hanno l'azienda a cui la pubblicazione fa riferimento
                        // come path hanno "relate" e come nome file il codice passato (che è una cosa del tipo relata_[numeroPubblicazione]_[annoPubblicazione]_[registro][numeroRegistro]_[annoRegistro].pdf)
                        MinIOWrapperFileInfo minIOWrapperFileInfo = minIOWrapper.getFileInfoByPathAndFileName(RELATE_MINIO_PATH, codice, azienda);
                        repositoryFileId = minIOWrapperFileInfo.getFileId();
                        fileName = minIOWrapperFileInfo.getFileName();
                        mimeType = "application/pdf";
                    } catch (MinIOWrapperException ex) {
                        errorMessage = String.format("errore nel reperimento del file da minIO con path %s e nome %s per l'azienda %s", RELATE_MINIO_PATH, codice, azienda);
                        LOG.error(errorMessage, ex);
                    }
                    break;

                default:
                    errorMessage = String.format("il tipo %s non è valido", tipo.toString());
                    throw new AlboBridgeException(errorMessage);
            }
        }

        if (StringUtils.hasText(repositoryFileId)) {
            try {
                // nel caso di relata il reposotoryId è il fileId, per cui lo cerco con getByFileId();
                if (tipo == TipiAllegato.RELATA) {
                    fileToSend = minIOWrapper.getByFileId(repositoryFileId);
                } else { // nel caso di gedi il reposotoryId è il mongo_uuid generato dalla libreria di minIO, per cui lo devo cercare con getByUuid()
                    fileToSend = minIOWrapper.getByUuid(repositoryFileId);
                }
            } catch (MinIOWrapperException ex) {
                errorMessage = String.format("errore nel reperimento del file da minIO con mongoUuid %s", repositoryFileId);
                LOG.error(errorMessage, ex);
            }
        } else {
            errorMessage = String.format("il repositoryFileId recuperato dal sottodocumento è nullo o vuoto: %s", repositoryFileId);
            LOG.error(errorMessage);
        }
        try {
            mimeType = ContentType.parse(mimeType).getMimeType();
        } catch (Exception ex) {
            LOG.warn(String.format("il mimetype %s non è corretto, verrà usato application/octet-stream", mimeType), ex);
        }
        if (fileToSend != null) {
            LOG.info("settaggio degli header...");
            response.addHeader("Content-Type", mimeType);
            if (forceDownload) {
                response.addHeader("Content-disposition", "attachment;filename=" + "\"" + fileName + "\"");
            }
            else {
                response.addHeader("Content-disposition", "inline;filename=" + "\"" + fileName + "\"");
            }
            try {
                LOG.info("invio dello stream...");
                IOUtils.copyLarge(fileToSend, response.getOutputStream());
                LOG.info("fine.");
            } catch (Exception ex) {
                errorMessage = "errore nell'invio del file";
                LOG.error(errorMessage, ex);
                throw new AlboBridgeException(errorMessage, ex);
            }
            finally {
                IOUtils.closeQuietly(fileToSend);
            }
        } else {
            throw new AlboBridgeException(errorMessage);
        }
    }

    /**
     * Crea la relata e la salva nel repository delle relate (bucket "albi" path logico "relate")
     * @param codiceAzienda il codice azienda (es, 105, 106, 109, ecc.)
     * @param titolo il titolo della relata (es. RELATA DI PUBBLICAZIONE, RELATA DI PUBBLICAZIONE PROFILO DEL COMMITTENTE, ecc.)
     * @param intestazione intestazione della relata (es.Registrazione Albo n. [numero] del [anno])
     * @param corpo il corpo delle relata (es. Si attesta che l'atto indicato in oggetto viene pubblicato all'Albo...)
     * @param registro il registro (es. Determine, Delibere, ecc.)
     * @param numeroRegistro il numero registro
     * @param annoRegistro anno del registro
     * @param oggetto l'oggetto del documento
     * @param numeroPubblicazione il numero di pubblicazione
     * @param annoPubblicazione l'anno di pubblicazione
     * @param request
     * @param response
     * @return il codice della relata creata, serve per poterla reperire con il metodo getAllegato
     * @throws AlboBridgeException in caso di eccezione gestita con message raprpesentante l'errore rilevato
     */
    @RequestMapping(value = {"makeAndSaveRelata"}, method = RequestMethod.GET)
    public String makeAndSaveRelata(
            @RequestParam(required = true) String codiceAzienda,
            @RequestParam(required = true) String titolo,
            @RequestParam(required = true) String intestazione,
            @RequestParam(required = true) String corpo,
            @RequestParam(required = true) String registro,
            @RequestParam(required = true) Integer numeroRegistro,
            @RequestParam(required = true) String annoRegistro,
            @RequestParam(required = true) String oggetto,
            @RequestParam(required = true) String numeroPubblicazione,
            @RequestParam(required = true) String annoPubblicazione,
            HttpServletRequest request,
            HttpServletResponse response) throws AlboBridgeException {

        try {
            // leggo i parametri del masterchef aziendale
            Azienda aziendaObj = cachedEntities.getAziendaFromCodice(codiceAzienda);
            AziendaParametriJson aziendaParametriJson = AziendaParametriJson.parse(objectMapper, aziendaObj.getParametri());
            AziendaParametriJson.MasterChefParmas masterchefParams = aziendaParametriJson.getMasterchefParams();

            // calcolo il nome del template della relata (c'è un template per ogni azienda)
            String templateNameRelata = RELATE_TEMPLATE_NAME.replace("[codiceAzienda]", codiceAzienda);

            // aggiungo gli zeri davanti al numero registro
            String numeroRegistroZeroPadded = String.format("%07d", numeroRegistro);
            
            // creo il json con i dati per la relata da passare al masterchef
            Map<String, String> templateData = new HashMap();
            templateData.put("titolo", titolo);
            templateData.put("intestazione", intestazione);
            templateData.put("corpo", corpo);
            templateData.put("registro", registro);
            templateData.put("numero_registro", numeroRegistroZeroPadded);
            templateData.put("anno_registro", annoRegistro);
            templateData.put("oggetto", oggetto);
            
            // creo i parametri per il job
            MasterChefUtils.MasterchefJobDescriptor masterchefJobDescriptor = masterChefUtils.buildReporterMasterchefJob(templateNameRelata, templateData, null, null);

            // accodo il job e ne attendo il risultato
            MasterChefUtils.MasterchefJobResult resMasterchef = masterChefUtils.sendMasterChefJobAndWaitResult(masterchefJobDescriptor, masterchefParams);

            // se tutto ok devo scaricare la relata dal repository temporaneo e copiarla nel repository delle relate
            if (resMasterchef.isSuccesful()) {
                String relataUuid = (String) resMasterchef.getResult().get("pdf");
                MinIOWrapper minIOWrapper = this.reporitoryConnectionManager.getMinIOWrapper();

                // calcolo il nome da attribuire alla relata in base al template
                String relataFileName = RELATE_FILENAME_TEMPLATE
                                            .replace("[numeroPubblicazione]", numeroPubblicazione)
                                            .replace("[annoPubblicazione]", annoPubblicazione)
                                            .replace("[registro]", registro)
                                            .replace("[numeroRegistro]", numeroRegistroZeroPadded)
                                            .replace("[annoRegistro]", annoRegistro);

                // carico la relata nel posto giusto
                MinIOWrapperFileInfo relataFileInfo = minIOWrapper.putWithBucket(minIOWrapper.getByUuid(relataUuid), codiceAzienda, RELATE_MINIO_PATH, relataFileName, null, false, UUID.randomUUID().toString(), RELATE_BUCKET_NAME);
                String codiceRelata = relataFileInfo.getFileName();

                // torno il nome delle relata che sarà il codice per poterla poi reperire (il nome è univoco all'interno dello stesso path)
                return codiceRelata;
            } else {
                String errorMessage = "errore nella creazione della relata dal masterchef";
                LOG.error(errorMessage);
                throw new AlboBridgeException(errorMessage);
            }
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione della relata dal masterchef";
            LOG.error(errorMessage, ex);
            throw new AlboBridgeException(errorMessage, ex);
        }
    }
}
