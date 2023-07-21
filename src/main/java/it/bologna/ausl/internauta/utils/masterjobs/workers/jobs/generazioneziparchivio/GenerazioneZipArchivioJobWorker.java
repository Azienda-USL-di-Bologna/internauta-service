package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.scripta.ScriptaArchiviUtils;
import it.bologna.ausl.internauta.service.downloader.utils.DownloaderUtils;
import it.bologna.ausl.internauta.service.exceptions.DownloaderUtilsException;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.downloader.controllers.DownloaderController;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderPluginException;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderPluginFactory;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipOutputStream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author conte
 */
@MasterjobsWorker
public class GenerazioneZipArchivioJobWorker extends JobWorker<GenerazioneZipArchivioJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(GenerazioneZipArchivioJobWorker.class);
    private final String name = GenerazioneZipArchivioJobWorker.class.getSimpleName();
    
    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;
    
    @Autowired
    DownloaderUtils downloaderUtils;
    
    @Autowired
    ScriptaArchiviUtils scriptaArchiviUtils;
    
    @Autowired
    AttivitaRepository attivitaRepository;
    
    @Autowired
    AziendaRepository aziendaRepository;
    
    @Autowired
    ApplicazioneRepository applicazioneRepository;
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    private DownloaderController downloaderController;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;    
    
    @PersistenceContext
    private EntityManager em;
    
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        //ricavo i dati necessari dal workerData
        Persona persona = getWorkerData().getPersona();
        Archivio archivio = getWorkerData().getArchivio();
        String downloadUrl = getWorkerData().getDownloadUrl();
     
        //calcolo numero e filename da utilizzare sucessivamente per il salvataggio e scaricamento dello zip
        String numero = archivio.getNumerazioneGerarchica().substring(0, archivio.getNumerazioneGerarchica().indexOf("/"));
        String archivioZipName = String.format("%s-%d-%s.zip", numero, archivio.getAnno(), archivio.getOggetto().trim());
        Azienda aziendaArch = aziendaRepository.getById(archivio.getIdAzienda().getId());
        Integer downloadArchivioZipTokenExpireSeconds = downloaderUtils.getTokenExpireSeconds();
        List<ParametroAziende> parameters = parametriAziende.getParameters("downloadArchivioZipTokenExpireSeconds", new Integer[]{aziendaArch.getId()});
        if (parameters != null && !parameters.isEmpty()) {
            downloadArchivioZipTokenExpireSeconds = parametriAziende.getValue(parameters.get(0), Integer.class);
        }
        
        String errorMessage;
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        //creo lo zip, prima come outstream poi lo converto in inputstream
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(byteOutStream))) {
            scriptaArchiviUtils.buildArchivio(archivio, "", persona, zipOut, jPAQueryFactory, minIOWrapper);
        } catch (IOException ex) {
            errorMessage = "errore nella generazione dell'archivio";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        } catch (Http404ResponseException ex) {
            log.error(ex.getMessage(), ex);
            throw new MasterjobsWorkerException(ex.getMessage(), ex);
        }
        
        ByteArrayInputStream bis = new ByteArrayInputStream(byteOutStream.toByteArray());
        //carico e ottengo il url per il download con token valido per un minuto
        String urlToDownload = null;
        try {
            Map<String, Object> uploaderPluginParams = downloaderUtils.getUploaderPluginParams(archivioZipName, null);
            Map<String, Object> params = downloaderController.upload(DownloaderPluginFactory.TargetRepository.MinIO, uploaderPluginParams, bis, "/internauta/archivi-zip", archivioZipName);
            Map<String, Object> downloadParams = downloaderUtils.getDownloaderPluginParams(params, archivioZipName, "application/zip");
            urlToDownload = downloaderUtils.buildDownloadUrl(archivioZipName, "application/zip", downloadParams, true, downloadUrl, downloadArchivioZipTokenExpireSeconds);
        } catch (DownloaderUtilsException | IOException | AuthorizationUtilsException | NoSuchAlgorithmException |InvalidKeySpecException ex) {
            errorMessage = "Errore nella generazione dell'url per il download";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        } catch (DownloaderPluginException ex) {
            errorMessage = "Errore nell'upload su MinIO";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        //genero la nofica che apparirà sulla scivania con il link per il download e della nuova applicazione
        Applicazione app = applicazioneRepository.getById("downloader");
        Attivita a = new Attivita(null, archivio.getIdAzienda(), Attivita.TipoAttivita.NOTIFICA.toString(), ZonedDateTime.now(), ZonedDateTime.now());
        HashMap<String,String> urlsMap = new HashMap();
        urlsMap.put("url", urlToDownload);
        urlsMap.put("label", "Scarica");
        List<HashMap<String,String>> listaUrls = new ArrayList(); 
        listaUrls.add(urlsMap);
//        a.setUrls(String.format("[{\"url\": \"%s\", \"label\": \"Scarica\"}]", urlToDownload));
        a.setUrls(listaUrls);
        a.setDescrizione("Archivio zip generato per lo scaricamento asincrono");
        a.setIdApplicazione(app);
        a.setIdAzienda(aziendaArch);
        a.setIdPersona(personaRepository.getById(persona.getId()));
        a.setOggetto(String.format("Fascicolo: %s", archivioZipName));
        a.setProvenienza(app.getNome());
        a.setOggettoEsterno(archivio.getId().toString());
        a.setTipoOggettoEsterno("ArchivioInternauta");
        a.setOggettoEsternoSecondario(archivio.getId().toString());
        a.setTipoOggettoEsternoSecondario("ArchivioInternauta");
        a.setAperta(false);
        attivitaRepository.saveAndFlush(a);
        
        return null;
    }
    
}
