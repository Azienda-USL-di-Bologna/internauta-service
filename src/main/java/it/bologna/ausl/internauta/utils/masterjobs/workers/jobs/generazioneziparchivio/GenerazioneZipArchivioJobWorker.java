package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.scripta.ScriptaArchiviUtils;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils;
    
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
        String uploadUrl = getWorkerData().getUploadUrl();
        //calcolo numero e filename da utilizzare sucessivamente per il salvataggio e scaricamento dello zip
        String numero = archivio.getNumerazioneGerarchica().substring(0, archivio.getNumerazioneGerarchica().indexOf("/"));
        String archivioZipName = String.format("%s-%d-%s.zip", numero, archivio.getAnno(), archivio.getOggetto().trim());
        
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        //creo lo zip, prima come outstream poi lo converto in inputstream
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(byteOutStream))) {
            scriptaArchiviUtils.buildArchivio(archivio, "", persona, zipOut, jPAQueryFactory, minIOWrapper);
        } catch (IOException ex) {
            log.error("errore nella generazione dell'archivio", ex);
        } catch (Http404ResponseException ex) {
            log.error("errore nella generazione dell'archivio", ex);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(byteOutStream.toByteArray());
        //carico e ottengo il url per il download con token valido per un minuto
        String urlToDownload = null;
        try {
            urlToDownload = firmaRemotaDownloaderUtils.uploadToUploader(bis, archivioZipName, "application/zip", true, downloadUrl, uploadUrl);
        } catch (FirmaRemotaHttpException ex) {
            log.error("errore nell'upload e generazione del url per il download", ex);
        }
        //genero la nofica che apparirà sulla scivania con il link per il download e della nuova applicazione
        Applicazione app = applicazioneRepository.getById("downloader");
        Attivita a = new Attivita(null, archivio.getIdAzienda(), Attivita.TipoAttivita.NOTIFICA.toString(), ZonedDateTime.now(), ZonedDateTime.now());
        HashMap<String,String> urlsMap = new HashMap();
        urlsMap.put("url", urlToDownload);
        urlsMap.put("label", "Scarica");
        List<HashMap<String,String>> listaUrls = new ArrayList(); 
//        a.setUrls(String.format("[{\"url\": \"%s\", \"label\": \"Scarica\"}]", urlToDownload));
        a.setUrls(listaUrls);
        a.setDescrizione("Archivio zip generato per lo scaricamento asincrono");
        a.setIdApplicazione(app);
        a.setIdAzienda(aziendaRepository.getById(archivio.getIdAzienda().getId()));
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
