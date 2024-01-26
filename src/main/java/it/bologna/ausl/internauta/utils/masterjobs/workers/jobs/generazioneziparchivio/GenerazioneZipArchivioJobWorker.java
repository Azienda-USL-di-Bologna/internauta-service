package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio;

import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.scripta.ScriptaArchiviUtils;
import it.bologna.ausl.internauta.service.downloader.utils.DownloaderUtils;
import it.bologna.ausl.internauta.service.exceptions.DownloaderUtilsException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.downloader.controllers.DownloaderController;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderPluginException;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderPluginFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    ArchivioRepository archivioRepository;
    
    @Autowired
    private DownloaderController downloaderController;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;    
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di {}", getName());
        //ricavo i dati necessari dal workerData
        Integer idPersona = getWorkerData().getIdPersona();
        Persona persona = personaRepository.getById(idPersona);
        Integer idArchivio = getWorkerData().getIdArchivio();
        Archivio archivio = archivioRepository.getById(idArchivio);
        String downloadUrl = getWorkerData().getDownloadUrl();
        
        // calcolo numero e filename con un uuid nel nome per evitare che nello stesso momento
        // un altro processo dello stesso job mi legga/scriva lo stesso file corrompendo i dati
        String numerazioneGerarchicaFascicolo = archivio.getNumerazioneGerarchica().substring(0, archivio.getNumerazioneGerarchica().indexOf("/"));
        String archivioZipNameUnivoco = String.format("%s$%s-%d-%s.zip", UUID.randomUUID().toString(), numerazioneGerarchicaFascicolo, archivio.getAnno(), archivio.getOggetto().trim());
        
        Integer archivioZipNameUnivocoLength = archivioZipNameUnivoco.length();
        System.out.println(archivioZipNameUnivocoLength);
        if(archivioZipNameUnivocoLength >= 200){
            String primaParte = archivioZipNameUnivoco.substring(0,150);
            String secondaParte = archivioZipNameUnivoco.substring(archivioZipNameUnivocoLength - 14);
            archivioZipNameUnivoco = primaParte.concat("...".concat(secondaParte));
        }
        
        archivioZipNameUnivoco = archivioZipNameUnivoco.replace("/", "-");
        // tolgo l'uuid dal nome dell'allegato 
        String archivioZipName = archivioZipNameUnivoco.substring(archivioZipNameUnivoco.lastIndexOf("$") + 1);

        if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), archivio.getId(), PermessoArchivio.DecimalePredicato.VISUALIZZA)) {
            return null;
        }
        // ottengo il tempo di durata del token dalla tabella configurazione.parametri_aziende
        Azienda aziendaArch = aziendaRepository.getById(archivio.getIdAzienda().getId());
        Integer downloadArchivioZipTokenExpireSeconds = downloaderUtils.getTokenExpireSeconds();
        List<ParametroAziende> parameters = parametriAziende.getParameters("downloadArchivioZipTokenExpireSeconds", new Integer[]{aziendaArch.getId()});
        if (parameters != null && !parameters.isEmpty()) {
            downloadArchivioZipTokenExpireSeconds = parametriAziende.getValue(parameters.get(0), Integer.class);
        }
       
        // preparo la stringa per l'url per scaricare lo zip
        String urlToDownload = null;
        
        try {
            // creo lo zip wrappando un FileOutputStream cosicché posso salvarlo su disco e non tenerlo in memoria 
            // evitando così la possibilità di out of range data da fascicoli, o singoli allegati, troppo grossi
            try (FileOutputStream fos = new FileOutputStream(archivioZipNameUnivoco)) {
                log.info("parto a creare lo zip");
                scriptaArchiviUtils.createZipArchivio(archivio, persona, fos);
            } catch (IOException ex) {
                String errorMessage = "errore nella generazione dell'archivio";
                log.error(errorMessage, ex);
                throw new MasterjobsWorkerException(errorMessage, ex);
            }
            FileInputStream fis = new FileInputStream(archivioZipNameUnivoco);
            log.info("zip concluso correttamente, procedo a salvarlo su minio e generare il link per scaricarlo");
            // leggo lo zip scritto in locale come FileInputStream e succesivamente
            // lo carico su minio e ottengo l'url per il download con token valido per un giorno
            // a questi metodi passo archivioZipName al posto di archivioZipNameUnivoco perché il secondo contiene un uuid
            Map<String, Object> uploaderPluginParams = downloaderUtils.getUploaderPluginParams(archivioZipName, null);
            Map<String, Object> params = downloaderController.upload(DownloaderPluginFactory.TargetRepository.MinIO, uploaderPluginParams, fis, "/internauta/archivi-zip", archivioZipName);
            urlToDownload = downloaderUtils.buildDownloadUrl(archivioZipName, "application/zip", params, true, downloadUrl, downloadArchivioZipTokenExpireSeconds);
            fis.close();
        } catch (DownloaderUtilsException | IOException | AuthorizationUtilsException | NoSuchAlgorithmException |InvalidKeySpecException ex) {
            String errorMessage = "Errore nella generazione dell'url per il download";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        } catch (DownloaderPluginException ex) {
            String errorMessage = "Errore nell'upload su MinIO";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        } finally {
            log.info("elimino lo zip");
            // una volta finito di lavorare con il file dello zip lo cancello da locale
            File f = new File(archivioZipNameUnivoco);
            if (f.exists()) {
                f.delete();
            }
        }
        
        
        
        log.info("genero l'attività");
        //genero la nofica che apparirà sulla scivania con il link per il download e della nuova applicazione
        Applicazione app = applicazioneRepository.getById("downloader");
        Attivita a = new Attivita(null, archivio.getIdAzienda(), Attivita.TipoAttivita.NOTIFICA.toString(), ZonedDateTime.now(), ZonedDateTime.now());
        HashMap<String,String> urlsMap = new HashMap();
        urlsMap.put("url", urlToDownload);
        urlsMap.put("label", "Scarica");
        List<HashMap<String,String>> listaUrls = new ArrayList(); 
        listaUrls.add(urlsMap);
        a.setUrls(listaUrls);
        a.setDescrizione("Fascicolo zip");
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
        a.setDatiAggiuntivi(new HashMap<>());
        attivitaRepository.saveAndFlush(a);
        log.info("job finito!");
        return null;
    }
    
}
