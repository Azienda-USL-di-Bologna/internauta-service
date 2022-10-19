package it.bologna.ausl.internauta.service.controllers.scripta;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http501ResponseException;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.FileUtilities;
import it.bologna.ausl.internauta.service.utils.ScriptaUtils;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.projections.azienda.AziendaProjectionUtils;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.tools.SupportedFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
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
import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ScriptaDownloadUtils {
    
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
    
    /**
     * Torna in inputStream l'allegato richiesto nel suo dettaglio orginale. 
     * Potrebbe essere che l'allegato fosse in un bucket temporaneo e il file non più disponibile.
     * In quel caso la funzione prende il contenitore radice dell'allegato e risalva tutto sul bucket temporaneo.
     * @param allegatoToDownload
     * @return
     * @throws MinIOWrapperException
     * @throws Http404ResponseException 
     * @throws java.security.NoSuchAlgorithmException 
     */
    public InputStream downloadOriginalAttachment(Allegato allegatoToDownload) throws MinIOWrapperException, Http404ResponseException, NoSuchAlgorithmException, Throwable {
        Allegato.DettagliAllegato dettagliAllegatoToDownload = allegatoToDownload.getDettagli();
        if (dettagliAllegatoToDownload != null) {
            Allegato.DettaglioAllegato originaleToDowload = dettagliAllegatoToDownload.getOriginale();
            if (originaleToDowload != null) {
                MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
                InputStream file = minIOWrapper.getByFileId(originaleToDowload.getIdRepository());
                if (file == null) {
                    // Allegato originale non trovato, forse era un file temporaneo, se è così deve avere il padre
                    if (allegatoToDownload.getIdAllegatoPadre() != null) {
                        Allegato allegatoRadice = getAllegatoRadice(allegatoToDownload);
                        InputStream fileRadice = downloadOriginalAttachment(allegatoRadice);
                        if (fileRadice != null) {
                            // Faccio la select for update dell'allegato radice, in modo da mettermi in coda su di esso
                            allegatoRadice = selectForUpdateAllegato(allegatoRadice);
                            
                            // Select for update eseguita, è il mio turno, controllo se l'allegato che mi interessa ha un nuovo idRepository
                            String idRepositoryOld = originaleToDowload.getIdRepository();
                            Allegato allegatoForseAggiornato = allegatoRepository.getById(allegatoToDownload.getId());
                            String idRepositoryNew = allegatoForseAggiornato.getDettagli().getOriginale().getIdRepository();
                            if (idRepositoryNew.equals(idRepositoryOld)) {
                                // L'idRepository è rimasto lo stesso. Allora sono io a dover riuploadare tutto il pacchetto
                                HashMap<String, Allegato> mappaAllegatiToUpdate = new HashMap();
                                fillMapAllegatiToUpdate(allegatoRadice, mappaAllegatiToUpdate);
                                scriptaUtils.creaAndAllegaAllegati(allegatoToDownload.getIdDoc(), fileRadice, allegatoRadice.getNome(), true, true, null, true, mappaAllegatiToUpdate);
                                Allegato allegatoAggiornato = allegatoRepository.getById(allegatoToDownload.getId());
                                return minIOWrapper.getByFileId(allegatoAggiornato.getDettagli().getOriginale().getIdRepository());
                            } else {
                                // C'è un nuovo idRepository, lo scarico
                                return minIOWrapper.getByFileId(idRepositoryNew);
                            }
                        } else {
                            throw new Http404ResponseException("8", "Il file originale dell'allegato radice, dell'allegato richiesto, non è stato trovato");
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
     * 
     * @param allegato
     * @param fileToConvert
     * @param idRepositoryScadutoDelConvertito
     * @return
     * @throws MinIOWrapperException 
     */
    private InputStream convertToPdf(Allegato allegato, InputStream fileToConvert, String idRepositoryScadutoDelConvertito) 
            throws MinIOWrapperException, IOException, UnsupportedEncodingException, MimeTypeException, Http500ResponseException, BlackBoxPermissionException, Http501ResponseException {
        // Eseguo la select for update
        Allegato allegatoForUpdate = selectForUpdateAllegato(allegato);
        
        // Ora che ho l'allegato, controllo se qualcun'altro ha appena salavato il convertito
        // Controllo quindi se ora il convertito c'è e se ha l'idRepository diverso da quello scaduto passato come parametro
        Allegato.DettagliAllegato dettagliForUpdate = allegatoForUpdate.getDettagli();
        Allegato.DettaglioAllegato convertitoForUpdate = dettagliForUpdate.getConvertito();
        
        if (convertitoForUpdate != null && !convertitoForUpdate.getIdRepository().equals(idRepositoryScadutoDelConvertito)) {
            MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
            return minIOWrapper.getByFileId(convertitoForUpdate.getIdRepository());
        }
        
        // Il convertito nuovo non c'è già. sta a me crearlo e aggiornare i dati dell'allegato
        // Devo intanto vedere se il file è convertibile
        if (isConvertibile(fileToConvert)) {
            // Costruisco la chiamata per convertire il file es url: https://babel.ausl.bologna.it/firmasemplice/PdfConvert
            AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
            String baseUrl = aziendaProjectionUtils.getBaseUrl(authenticatedSessionData.getUser().getIdAzienda());
            String urlFirmaSemplicePdfConvert = baseUrl + pdfConvertUrl;  // TODO: chiedere a gdm, Va bene fatto così?
            
            OkHttpClient httpClient = httpClientManager.getHttpClient(Duration.ofMinutes(10));
            String mimeType = FileUtilities.getMimeTypeFromInputStream(fileToConvert);
            
            // TODO: chiedere a gdm, Va bene fatto così?
            okhttp3.RequestBody requestBody = new MultipartBody.Builder()
                        .addFormDataPart("file", allegato.getNome(), okhttp3.RequestBody.create(MediaType.parse(mimeType), IOUtils.toByteArray(fileToConvert)))
                        .build();
            
            Response response = httpClient.newCall(
                    new Request.Builder()
                        .url(urlFirmaSemplicePdfConvert)
                        .post(requestBody).build()).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                return response.body().byteStream();
            } else {
                throw new Http500ResponseException("10", "Errore nella conversione in pdf");
            }
        } else {
            // TODO: Generare segnaposto
            throw new Http501ResponseException("11", "Generazione segnaposto non implementata");
        }
    }
    
    /**
     * Metodo chiamato se si voleva un convertito che però non c'è (magari è scaduto),
     * il metodo scarica l'orginale dell'allegato passatto e lo converte tornando l'inputStream del file convertito.
     * Il file convertito potrebbe essere un segnaposto in caso di non convertibilità.
     * @param allegato
     * @param idRepositoryScadutoDelConvertito
     * @return
     * @throws MinIOWrapperException
     * @throws Http404ResponseException 
     */
    public InputStream downloadOriginalAndConvertToPdf(Allegato allegato, String idRepositoryScadutoDelConvertito) throws MinIOWrapperException, Http404ResponseException, Throwable {
        InputStream originale = downloadOriginalAttachment(allegato);
        if (originale == null) {
            throw new Http404ResponseException("5", "Il file originale dell'allegato richiesto non è stato trovato");
        } else {
            return convertToPdf(allegato, originale, idRepositoryScadutoDelConvertito);
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
    private boolean isConvertibile(InputStream file) throws IOException, UnsupportedEncodingException, MimeTypeException, Http500ResponseException {
        String mimeType = FileUtilities.getMimeTypeFromInputStream(file);
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
}
