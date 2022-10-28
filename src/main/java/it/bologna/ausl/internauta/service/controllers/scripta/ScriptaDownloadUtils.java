package it.bologna.ausl.internauta.service.controllers.scripta;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http501ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpResponseRuntimeException;
import it.bologna.ausl.internauta.service.exceptions.scripta.AllegatoException;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.FileUtilities;
import it.bologna.ausl.internauta.service.utils.ScriptaUtils;
import it.bologna.ausl.mimetypeutilities.Detector;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.projections.azienda.AziendaProjectionUtils;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.tools.SupportedFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.mime.MimeTypeException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author gusgus
 */
@Component
public class ScriptaDownloadUtils {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ScriptaDownloadUtils.class);
    
    private List<File> tempfiles = new ArrayList();
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private ReporitoryConnectionManager aziendeConnectionManager;
    
    @Autowired
    private ScriptaUtils scriptaUtils;
    
    @Autowired
    private AllegatoRepository allegatoRepository;
    
    @Autowired
    private HttpClientManager httpClientManager;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Value("${firmasemplice.pdf-convert.url}")
    private String pdfConvertUrl;
    
    @Autowired
    private AziendaProjectionUtils aziendaProjectionUtils;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    /**
     * Torna in inputStream l'allegato richiesto nel suo dettaglio orginale.Potrebbe essere che l'allegato fosse in un bucket temporaneo e il file non sia più disponibile.In quel caso la funzione prende il contenitore radice dell'allegato e risalva tutto sul bucket temporaneo. 
     * @param allegatoToDownload
     * @return
     * @throws MinIOWrapperException
     * @throws Http404ResponseException 
     * @throws java.security.NoSuchAlgorithmException 
     * @throws it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException 
     * @throws java.io.FileNotFoundException 
     * @throws it.bologna.ausl.estrattore.exception.ExtractorException 
     * @throws java.io.UnsupportedEncodingException 
     * @throws org.apache.tika.mime.MimeTypeException 
     * @throws it.bologna.ausl.internauta.service.exceptions.scripta.AllegatoException 
     */
    public InputStream downloadOriginalAttachment(Allegato allegatoToDownload) 
            throws MinIOWrapperException, Http404ResponseException, NoSuchAlgorithmException, Http500ResponseException, FileNotFoundException, ExtractorException, 
            UnsupportedEncodingException, MimeTypeException, AllegatoException {
        Allegato.DettagliAllegato dettagliAllegatoToDownload = allegatoToDownload.getDettagli();
        if (dettagliAllegatoToDownload != null) {
            Allegato.DettaglioAllegato originaleToDowload = dettagliAllegatoToDownload.getOriginale();
            if (originaleToDowload != null) {
                MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
                InputStream file = minIOWrapper.getByFileId(originaleToDowload.getIdRepository());
                if (file == null) {
                    // Allegato originale non trovato, forse era un file temporaneo (estratto), se è così deve avere il padre
                    if (allegatoToDownload.getIdAllegatoPadre() != null) {
                        Allegato allegatoRadice = getAllegatoRadice(allegatoToDownload);
                        try (InputStream fileRadice = downloadOriginalAttachment(allegatoRadice)) {
                            if (fileRadice != null) {
                                // Faccio la select for update dell'allegato radice, in modo da mettermi in coda su di esso
                                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                                InputStream res = transactionTemplate.execute(a -> {
                                    Allegato allegatoRadiceForUpdate = selectForUpdateAllegato(allegatoRadice);
                                    try {
                                        // Select for update eseguita, è il mio turno, controllo se l'allegato che mi interessa ha un nuovo idRepository
                                        String idRepositoryOld = originaleToDowload.getIdRepository();
                                        Allegato allegatoForseAggiornato = allegatoRepository.getById(allegatoToDownload.getId());
                                        String idRepositoryNew = allegatoForseAggiornato.getDettagli().getOriginale().getIdRepository();
                                        if (idRepositoryNew.equals(idRepositoryOld)) {
                                            // L'idRepository è rimasto lo stesso. Allora sono io a dover riuploadare tutto il pacchetto
                                            HashMap<String, Allegato> mappaAllegatiToUpdate = new HashMap();
                                            fillMapAllegatiToUpdate(allegatoRadiceForUpdate, mappaAllegatiToUpdate);
                                            try {
                                                scriptaUtils.creaAndAllegaAllegati(
                                                        allegatoToDownload.getIdDoc(), 
                                                        fileRadice, 
                                                        allegatoRadiceForUpdate.getNome(), 
                                                        true, 
                                                        true, 
                                                        allegatoRadiceForUpdate.getDettagli().getOriginale().getIdRepository(), 
                                                        true, 
                                                        mappaAllegatiToUpdate
                                                );
                                            } catch (MinIOWrapperException ex) {
                                                throw new Http500ResponseException("24", "Minio exception", ex);
                                            } catch (IOException | NoSuchAlgorithmException | MimeTypeException ex) {
                                                throw new Http500ResponseException("25", "File Exception", ex);
                                            } catch (ExtractorException ex) {
                                                throw new Http500ResponseException("26", "ExtractorException", ex);
                                            } catch (AllegatoException ex) {
                                                throw new Http500ResponseException("27", "AllegatoException", ex);
                                            }

                                            Allegato allegatoAggiornato = allegatoRepository.getById(allegatoToDownload.getId());
                                            try {
                                                return minIOWrapper.getByFileId(allegatoAggiornato.getDettagli().getOriginale().getIdRepository());
                                            } catch (MinIOWrapperException ex) {
                                                throw new Http500ResponseException("28", "Minio exception", ex);
                                            }
                                        } else {
                                            try {
                                                // C'è un nuovo idRepository, lo scarico
                                                return minIOWrapper.getByFileId(idRepositoryNew);
                                            } catch (MinIOWrapperException ex) {
                                                throw new Http500ResponseException("29", "Minio exception", ex);
                                            }
                                        }
                                    } catch (Http500ResponseException ex) {
                                        throw new HttpResponseRuntimeException(ex);
                                    }
                                });
                                return res;
                            } else {
                                throw new Http404ResponseException("8", "Il file originale dell'allegato radice, dell'allegato richiesto, non è stato trovato");
                            }
                        } catch (IOException ex) {
                            throw new Http500ResponseException("23", "IOException", ex);
                        }
                    }
                } else {
                    return file; // Allegato originale trovato
                }
            } else {
                throw new Http404ResponseException("7", "Allegato malformato, senza originale");
            }
        } else {
            throw new Http404ResponseException("6", "Allegato malformato, senza dettagli");
        }
        return null;
    }
    
    /**
     * Prende il fileToConvert, lo converte se possibile e lo restituisce.
     * In alternativa creerà un segnaposto e restituirà quello.
     * @param allegato
     * @param fileToConvert
     * @param idRepositoryScadutoDelConvertito
     * @return
     * @throws MinIOWrapperException 
     */
    private InputStream convertToPdf(Allegato allegato, InputStream fileToConvert, String idRepositoryScadutoDelConvertito) 
            throws Http500ResponseException, Http501ResponseException {
        // Eseguo la select for update
        Allegato allegatoForUpdate = allegato;
        
        // Ora che ho l'allegato, controllo se qualcun'altro ha appena salavato il convertito
        // Controllo quindi se ora il convertito c'è e se ha l'idRepository diverso da quello scaduto passato come parametro
        Allegato.DettagliAllegato dettagliForUpdate = allegatoForUpdate.getDettagli();
        Allegato.DettaglioAllegato convertitoForUpdate = dettagliForUpdate.getConvertito();
        Allegato.DettaglioAllegato originale = dettagliForUpdate.getOriginale();
        
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        
        if (convertitoForUpdate != null && !convertitoForUpdate.getIdRepository().equals(idRepositoryScadutoDelConvertito)) {
            try {
                return minIOWrapper.getByFileId(convertitoForUpdate.getIdRepository());
            } catch (MinIOWrapperException ex) {
                throw new Http500ResponseException("16", "Minio exception", ex);
            }
        }
        
        // Il convertito nuovo non c'è già. sta a me crearlo e aggiornare i dati dell'allegato
        // Devo intanto vedere se il file è convertibile
        File file = null;        
        String mimeType = null;
        try {
            file = File.createTempFile("Allegato_", FilenameUtils.getExtension(originale.getNome()));
            tempfiles.add(file);
            FileUtils.copyInputStreamToFile(fileToConvert, file);
            mimeType = FileUtilities.getMimeTypeFromPath(file.getAbsolutePath());
        } catch (IOException | MimeTypeException ex) {
            throw new Http500ResponseException("23", "IOException", ex);
        }
        try {
            if (isConvertibile(mimeType)) {
                // Costruisco la chiamata per convertire il file es url: https://babel.ausl.bologna.it/firmasemplice/PdfConvert
                AuthenticatedSessionData authenticatedSessionData = null;
                try {
                    authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
                } catch (BlackBoxPermissionException ex) {
                    throw new Http500ResponseException("15", "Blackbox exception", ex);
                }
                String baseUrl = aziendaProjectionUtils.getBaseUrl(authenticatedSessionData.getUser().getIdAzienda());
                String urlFirmaSemplicePdfConvert = baseUrl + pdfConvertUrl;
                String nomeAllegatoConEstensione = allegato.getNome() + "." + allegato.getDettagli().getOriginale().getEstensione();
                
                OkHttpClient httpClient = httpClientManager.getHttpClient(Duration.ofMinutes(10));
                
                okhttp3.RequestBody requestBody = new MultipartBody.Builder()
                        .addFormDataPart("file", nomeAllegatoConEstensione, okhttp3.RequestBody.create(MediaType.parse(mimeType), file))
                        .build();
                
                Response response = null;
                try {
                    response = httpClient.newCall(
                            new Request.Builder()
                                    .url(urlFirmaSemplicePdfConvert)
                                    .post(requestBody).build()).execute();
                } catch (IOException ex) {
                    throw new Http500ResponseException("19", "IO Exception", ex);
                }
                
                if (response != null && response.isSuccessful() && response.body() != null) {
                    try (InputStream convertito = response.body().byteStream()) {
                        if (convertito != null) {
                            File fileConvertito = File.createTempFile("Allegato_", FilenameUtils.getExtension(originale.getNome()));
                            tempfiles.add(fileConvertito);
                            FileUtils.copyInputStreamToFile(convertito, fileConvertito);
                            MinIOWrapperFileInfo info;
                            info = minIOWrapper.put(
                                    fileConvertito,
                                    allegatoForUpdate.getIdDoc().getIdAzienda().getCodice() + "t",
                                    allegatoForUpdate.getIdDoc().getId().toString(),
                                    "convertito_" + allegato.getNome(),
                                    null,
                                    false
                            );
                            
                            String mimeTypeConvertito = Detector.MEDIA_TYPE_APPLICATION_PDF.toString();
                            allegatoForUpdate.getDettagli().setConvertito(scriptaUtils.buildNewDettaglioAllegato(allegato, fileConvertito, "convertito_" + allegato.getNome(), mimeTypeConvertito, info.getSize(), info));
                            return new FileInputStream(fileConvertito);
                        } else {
                            throw new Http500ResponseException("13", "Non sono riuscito a convertire il file. Qualcosa è andato storto");
                        }
                    } catch (MinIOWrapperException ex) {
                        throw new Http500ResponseException("16", "Minio exception", ex);
                    } catch (IOException | NoSuchAlgorithmException | MimeTypeException ex) {
                        throw new Http500ResponseException("18", "File Exception", ex);
                    }
                } else {
                    throw new Http500ResponseException("10", "Errore nella conversione in pdf");
                }
            } else {
                // TODO: Generare segnaposto
                throw new Http501ResponseException("11", "Generazione segnaposto non implementata");
            }
        } catch (IOException ex) {
            throw new Http500ResponseException("20", "IOException", ex);
        } catch (MimeTypeException ex) {
            throw new Http500ResponseException("21", "MimeTypeException", ex);
        } catch (BlackBoxPermissionException ex) {
            throw new Http500ResponseException("22", "BlackBoxPermissionException", ex);
        }
    }
    
    /**
     * Metodo chiamato se si voleva un convertito che però non c'è (magari è scaduto),
     * il metodo scarica l'orginale dell'allegato passatto e lo converte tornando l'inputStream del file convertito.Il file convertito potrebbe essere un segnaposto in caso di non convertibilità.
     * @param allegato
     * @param idRepositoryScadutoDelConvertito
     * @return
     * @throws MinIOWrapperException
     * @throws Http404ResponseException 
     * @throws it.bologna.ausl.estrattore.exception.ExtractorException 
     * @throws java.security.NoSuchAlgorithmException 
     * @throws it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException 
     * @throws java.io.FileNotFoundException 
     * @throws org.apache.tika.mime.MimeTypeException 
     * @throws it.bologna.ausl.internauta.service.exceptions.scripta.AllegatoException 
     * @throws java.io.IOException 
     * @throws java.io.UnsupportedEncodingException 
     */
    public InputStream downloadOriginalAndConvertToPdf(Allegato allegato, String idRepositoryScadutoDelConvertito) 
            throws MinIOWrapperException, Http404ResponseException, HttpInternautaResponseException, NoSuchAlgorithmException, Http500ResponseException, FileNotFoundException, 
            ExtractorException, UnsupportedEncodingException, MimeTypeException, AllegatoException, IOException {
        try (InputStream originale = downloadOriginalAttachment(allegato)) {
            if (originale == null) {
                throw new Http404ResponseException("5", "Il file originale dell'allegato richiesto non è stato trovato");
            } else {
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                InputStream res = transactionTemplate.execute(a -> {
                     Allegato allegatoForUpdate = selectForUpdateAllegato(allegato);
                    try {
                        return convertToPdf(allegatoForUpdate, originale, idRepositoryScadutoDelConvertito);
                    } catch (Http500ResponseException | Http501ResponseException ex) {
                        throw new HttpResponseRuntimeException(ex);
                    }
                });
                return res; 
            }
        } catch (HttpResponseRuntimeException ex) {
            throw ex.getHttpInternautaResponseException();
        }
    }
    
    /**
     * Faccio la select for update di un allegato. In modo da mettermi in coda su di esso.
     * @param allegato
     * @return 
     */
    private Allegato selectForUpdateAllegato(Allegato allegato) {
        QAllegato qAllegato = QAllegato.allegato;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        Allegato allegatoForUpdate = queryFactory.query().setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .select(qAllegato)
            .from(qAllegato)
            .where(qAllegato.id.eq(allegato.getId()))
            .fetchOne();

        return allegatoForUpdate;
    }
    
    /**
     * Trona true se il file passato come InputStream è convertibile in pdf
     * @param file
     * @return
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws MimeTypeException 
     */
    private boolean isConvertibile(String mimeType) throws IOException, UnsupportedEncodingException, MimeTypeException, Http500ResponseException {
        Map<String, SupportedFile> supportedFiles = cachedEntities.getSupportedFiles();
        SupportedFile f = supportedFiles.get(mimeType);
        if (f != null) {
            return f.getConvertibilePdf();
        } else {
            throw new Http500ResponseException("9", "MimeType non previsto");
        }
    }
    
    /**
     * Torna l'allegato contenitore radice dell'allegato passato.
     * Se l'allegato non ha padri, torna l'allegato stesso.
     * @param allegato
     * @return 
     */
    private Allegato getAllegatoRadice(Allegato allegato) {
        Allegato idAllegatoPadre = allegato.getIdAllegatoPadre();
        if (idAllegatoPadre != null) {
            return getAllegatoRadice(idAllegatoPadre);
        } else {
            return allegato;
        }
    }
    
    /**
     * A partire dall'allegato contenitore passato tutti i figli e nipoti vengono inseiriti nella mappa,
     * la chiave della mappa è l'md5, il valore è l'allegato figlio.
     * @param allegato
     * @param map 
     */
    private void fillMapAllegatiToUpdate(Allegato allegato, HashMap<String, Allegato> map) {
        List<Allegato> allegatiFigliList = allegato.getAllegatiFigliList();
        for (Allegato a : allegatiFigliList) {
            map.put(a.getDettagli().getOriginale().getHashMd5(), a);
            fillMapAllegatiToUpdate(a, map);
        }
    }
    
    /**
     * Elimina i file temporanei della classe
     */
    public void svuotaTempFiles() {
        for (File f : tempfiles) {
            try {
                if (f != null) {
                    Files.deleteIfExists(f.toPath());
                }
            } catch (IOException ex) {
                LOG.error("Errore nella cancellazione del file", ex);
            }
        }
    }
}
