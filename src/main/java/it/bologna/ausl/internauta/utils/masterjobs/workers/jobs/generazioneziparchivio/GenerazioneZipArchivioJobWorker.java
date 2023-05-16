package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.scripta.ScriptaArchiviUtils;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.utils.authorizationutils.DownloaderTokenCreator;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import javax.servlet.http.HttpServletRequest;

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
    ApplicazioneRepository applicazioneRepository;
    
    @PersistenceContext
    private EntityManager em;
    
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        
        Persona persona = getWorkerData().getPersona();
        Archivio archivio = getWorkerData().getArchivio();
        HttpServletRequest request = getWorkerData().getRequest();
        
        String numero = archivio.getNumerazioneGerarchica().substring(0, archivio.getNumerazioneGerarchica().indexOf("/"));
        String archivioZipName = String.format("%s-%d-%s.zip", numero, archivio.getAnno(), archivio.getOggetto().trim());
        
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        
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
        
        String urlToDownload = null;
        try {
            urlToDownload = firmaRemotaDownloaderUtils.uploadToUploader(bis, archivioZipName, "application/zip", true, request);
        } catch (FirmaRemotaHttpException ex) {
            log.error("errore nell'upload e generazione del url per il download", ex);
        }
        
        Applicazione app = applicazioneRepository.getById("gediInt");
        Attivita a = new Attivita(null, archivio.getIdAzienda(), Attivita.TipoAttivita.NOTIFICA.toString(), ZonedDateTime.now(), ZonedDateTime.now());
        a.setUrls(String.format("[{\"url\": \"$s\", \"label\": \"Scarica archivio\"}]", urlToDownload));
        a.setDescrizione("Archivio zip generato per lo scaricamento asincrono");
        a.setIdApplicazione(app);
        a.setIdAzienda(archivio.getIdAzienda());
        a.setIdPersona(persona);
        a.setOggetto(archivioZipName);
        attivitaRepository.saveAndFlush(a);
        
        return null;
    }
    
}
