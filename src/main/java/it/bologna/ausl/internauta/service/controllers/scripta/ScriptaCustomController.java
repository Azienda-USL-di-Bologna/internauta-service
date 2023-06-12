package it.bologna.ausl.internauta.service.controllers.scripta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.documentgenerator.GeneratePE;
import it.bologna.ausl.documentgenerator.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Related;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http409ResponseException;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRecenteRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDiInteresseRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.RegistroDocRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.internauta.service.utils.ScriptaUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import java.io.FileNotFoundException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Formatter;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import it.bologna.ausl.internauta.service.repositories.scripta.DocDetailRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PersonaVedenteRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.utils.firma.utils.CommonUtils;
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio.GenerazioneZipArchivioJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio.GenerazioneZipArchivioJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.ArchivioRecente;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.PersonaVedente;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QPersonaVedente;
import it.bologna.ausl.model.entities.scripta.projections.generated.AllegatoWithIdAllegatoPadre;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.lang.reflect.InvocationTargetException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;

/**
 *
 * @author Mido
 */
@RestController
@RequestMapping(value = "${scripta.mapping.url.root}")
public class ScriptaCustomController implements ControllerHandledExceptions {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptaCustomController.class);

//    private MinIOWrapperFileInfo savedFileOnRepository = null;
    private final List<MinIOWrapperFileInfo> savedFilesOnRepository = new ArrayList();
//    private List<Allegato> savedFilesOnInternauta = new ArrayList();

    @Autowired
    private ConfigParams configParams;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private ArchivioRepository archivioRepository;

    @Autowired
    private ArchivioDocRepository archivioDocRepository;

    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ScriptaArchiviUtils scriptaArchiviUtils;

    @Autowired
    private ScriptaCopyUtils scriptaCopyUtils;

    @Autowired
    private NonCachedEntities nonCachedEntities;

    @Autowired
    private DocRepository docRepository;

    @Autowired
    private PermessoArchivioRepository permessoArchivioRepository;

    @Autowired
    private ArchivioDiInteresseRepository archivioDiInteresseRepository;

    @Autowired
    private ArchivioRecenteRepository archivioRecenteRepository;

    @Autowired
    private DocDetailRepository docDetailRepository;

    @Autowired
    private RegistroDocRepository registroDocRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private PecRepository pecRepository;
    
    @PersistenceContext
    private EntityManager em;

    @Autowired
    private PersonaVedenteRepository personaVedenteRepository;

    @Autowired
    private ScriptaUtils scriptaUtils;

    @Autowired
    private ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private AllegatoRepository allegatoRepository;

    @Autowired
    private ProjectionFactory projectionFactory;

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private GeneratePE generatePE;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Autowired
    private ScriptaDownloadUtils scriptaDownloadUtils;

    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;

    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;

    @Autowired
    private KrintUtils krintUtils;

    @Autowired
    private KrintScriptaService krintScriptaService;
    
    @Value("${babelsuite.webapi.eliminapropostadaedi.url}")
    private String EliminaPropostaDaEdiUrl;

    @Value("${babelsuite.webapi.eliminapropostadaedi.method}")
    private String eliminaPropostaDaEdiMethod;

    private static final Logger log = LoggerFactory.getLogger(ScriptaCustomController.class);

    /**
     * Controller chiamato dal PEIS per salvare una lista di allegati
     *
     * @param request
     * @param idDoc
     * @param numeroProposta
     * @param files
     * @return
     * @throws MinIOWrapperException
     * @throws NoSuchAlgorithmException
     * @throws Throwable
     */
    @RequestMapping(value = "saveAllegato", method = RequestMethod.POST)
    public ResponseEntity<?> saveAllegato(
            HttpServletRequest request,
            @RequestParam("idDoc") Integer idDoc,
            @RequestParam("numeroProposta") String numeroProposta,
            @RequestParam("files") List<MultipartFile> files) throws MinIOWrapperException, NoSuchAlgorithmException, Throwable {
        projectionsInterceptorLauncher.setRequestParams(null, request); // Necessario per poter poi creare una projection
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        Iterable<Allegato> tuttiAllegati = null;
        try {
            Optional<Doc> optionalDoc = docRepository.findById(idDoc); // Cerco il doc su cui mettere gli allegati
            Doc doc = null;
            if (!optionalDoc.isPresent()) {
                throw new Http500ResponseException("1", "documento non trovato");
            } else {
                doc = optionalDoc.get();
            }

            // Ciclo i files passati e poi vado a gestirli
            for (MultipartFile file : files) {
                scriptaUtils.creaAndAllegaAllegati(doc, file.getInputStream(), file.getOriginalFilename(), false);
            }

            // Carico tutti gli allegati del documento perché i voglio tornare al client
            tuttiAllegati = allegatoRepository.findAll(QAllegato.allegato.idDoc.id.eq(idDoc));
        } catch (Exception e) {
            if (savedFilesOnRepository != null && !savedFilesOnRepository.isEmpty()) {
                for (MinIOWrapperFileInfo minIOWrapperFileInfo : savedFilesOnRepository) {
                    minIOWrapper.removeByFileId(minIOWrapperFileInfo.getFileId(), false);
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        if (tuttiAllegati != null) {
            Stream<Allegato> stream = StreamSupport.stream(tuttiAllegati.spliterator(), false);
            return ResponseEntity.ok(stream.map(a -> projectionFactory.createProjection(AllegatoWithIdAllegatoPadre.class, a)).collect(Collectors.toList()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Effettua l'upload sul client dello stream del file richiesto
     *
     * @param idAllegato
     * @param tipoDettaglioAllegato
     * @param response
     * @param request
     * @throws IOException
     * @throws MinIOWrapperException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException
     */
    @Transactional
    @RequestMapping(value = "allegato/{idAllegato}/{tipoDettaglioAllegato}/download", method = RequestMethod.GET)
    public void downloadAttachment(
            @PathVariable(required = true) Integer idAllegato,
            @PathVariable(required = true) Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException, MinIOWrapperException, Http500ResponseException, Http404ResponseException, Throwable {
        LOG.info("downloadAllegato", idAllegato, tipoDettaglioAllegato);
        Allegato allegato = allegatoRepository.getById(idAllegato);
        if (allegato != null) {
            try {
                // L'utente ha diritto di vedere l'allegato in questione?
                AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
                Persona person = authenticatedSessionData.getPerson();
                QPersonaVedente qPersonaVedente = QPersonaVedente.personaVedente;
                BooleanExpression filter = qPersonaVedente.idPersona.id.eq(person.getId()).and(qPersonaVedente.pienaVisibilita.eq(Boolean.TRUE).and(qPersonaVedente.idDocDetail.id.eq(allegato.getIdDoc().getId())));
                Optional<PersonaVedente> personaVedente = personaVedenteRepository.findOne(filter);
                if (!personaVedente.isPresent()) {
                    throw new Http403ResponseException("0", "L'utente non ha piena visibilità sul documento dell'allegato. Non può quindi vederlo");
                }

                Allegato.DettagliAllegato dettagli = allegato.getDettagli();
                Allegato.DettaglioAllegato dettaglioAllegato;

                try {
                    dettaglioAllegato = dettagli.getDettaglioAllegato(tipoDettaglioAllegato);
                } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    LOG.info("errore nel recuperare il metodo get del tipo dettaglio allegato richiesto", ex);
                    throw new Http500ResponseException("1", "Errore generico, probabile dato malformato");
                }

                response.setHeader("X-Frame-Options", "sameorigin");
                response.setHeader("Content-Disposition", ";filename=" + allegato.getNome() + ".pdf");

                if (dettaglioAllegato == null) {
                    if (tipoDettaglioAllegato.equals(Allegato.DettagliAllegato.TipoDettaglioAllegato.CONVERTITO)) {
                        // File mai convertito, lo converto e lo scarico
                        try ( InputStream fileConvertito = scriptaDownloadUtils.downloadOriginalAndConvertToPdf(allegato, null)) {
                            response.setHeader("Content-Type", "application/pdf");
                            StreamUtils.copy(fileConvertito, response.getOutputStream());
                        }
                    } else {
                        throw new Http404ResponseException("4", "Dettaglio allegato richiesto non tovato. Sembra non essere mai esistito");
                    }
                } else {
                    String idRepository = dettaglioAllegato.getIdRepository();
                    MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
                    try ( InputStream fileRichiesto = minIOWrapper.getByFileId(idRepository)) {
                        if (fileRichiesto == null) {
                            switch (tipoDettaglioAllegato) {
                                case CONVERTITO:
                                    // File convertito ma scaduto, lo converto e lo scarico
                                    try ( InputStream fileConvertito = scriptaDownloadUtils.downloadOriginalAndConvertToPdf(allegato, idRepository)) {
                                    response.setHeader("Content-Type", "application/pdf");
                                    StreamUtils.copy(fileConvertito, response.getOutputStream());
                                }
                                break;
                                case ORIGINALE:
                                    // File scaduto, lo riestraggo e lo scarico
                                    try ( InputStream fileOrginale = scriptaDownloadUtils.downloadOriginalAttachment(allegato)) {
                                    response.setHeader("Content-Type", allegato.getDettagli().getOriginale().getMimeType());
                                    StreamUtils.copy(fileOrginale, response.getOutputStream());
                                }
                                break;
                                default:
                                    // Il file riciesto non è ne l'orginale ne il convertito. E' impossibile dunque recuperarlo.
                                    throw new Http404ResponseException("3", "Dettaglio allegato richiesto non tovato");
                            }
                        } else {
                            response.setHeader("Content-Type", allegato.getDettagli().getDettaglioAllegato(tipoDettaglioAllegato).getMimeType());
                            StreamUtils.copy(fileRichiesto, response.getOutputStream());
                        }
                    }
                }
            } finally {
                scriptaDownloadUtils.svuotaTempFiles();
            }
        } else {
            throw new Http404ResponseException("2", "Allegato richiesto non tovato");
        }
        response.flushBuffer();
    }

    /**
     * Effettua l'upload sul client dello stream dello zip contenente gli
     * allegati originale del doc richiesto
     *
     * @param idDoc
     * @param response
     * @param request
     * @throws IOException
     * @throws MinIOWrapperException
     */
    @RequestMapping(value = "downloadAllAttachments/{idDoc}", method = RequestMethod.GET, produces = "application/zip")
    public void downloadAllAttachments(
            @PathVariable(required = true) Integer idDoc,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException, MinIOWrapperException {
        LOG.info("downloadAllAttachments", idDoc);
        Doc doc = docRepository.getOne(idDoc);
        List<Allegato> allegati = doc.getAllegati();
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();

        ZipOutputStream zos = null;
        try {
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=allegati.zip");
            zos = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()));
            Integer i;
            for (Allegato allegato : allegati) {
                i = 0;
                Boolean in_error = true;
                while (in_error) {
                    try {
                        String s = "";
                        if (i > 0) {
                            s = "_" + Integer.toString(i);
                        }
                        zos.putNextEntry(new ZipEntry((String) allegato.getNome() + s + "." + allegato.getDettagli().getOriginale().getEstensione()));
                        in_error = false;
                    } catch (ZipException ex) {
                        i++;
                    }
                }
                StreamUtils.copy((InputStream) minIOWrapper.getByFileId(allegato.getDettagli().getOriginale().getIdRepository()), zos);
            }
            response.flushBuffer();
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }
    
    /**
     * Api per il download di un archivio con tutto il suo contenuto.
     * @param idArchivio L'id dell'archivio da scaricare.
     * @param response Http Response.
     * @param request Http request.
     * @throws Http403ResponseException Eccezioni in caso di mancanza di permessi.
     * @throws Http404ResponseException Eccezione lanciata quando il fascicolo da scaricare non ha nè documenti nè figli.
     * @throws Http500ResponseException Eccezioni in caso di errori nella generazione del file zip.
     * @throws BlackBoxPermissionException Errori della blackbox.
     */
    @RequestMapping(value = "downloadArchivioZip/{idArchivio}", method = RequestMethod.GET, produces = "application/zip")
    public void downloadArchivioZip(
            @PathVariable(required = true) Integer idArchivio,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws Http403ResponseException, Http404ResponseException, Http500ResponseException, BlackBoxPermissionException, MasterjobsWorkerException  {
        LOG.info("downloadArchivioZip: {}", idArchivio);
        
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Archivio archivio = archivioRepository.findById(idArchivio).orElseThrow(ResourceNotFoundException::new);
        
        if (Archivio.StatoArchivio.BOZZA.equals(archivio.getStato()))
            throw new Http403ResponseException("1", "L'archivio non può essere scaricato in quanto bozza.");
       
        if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), archivio.getId(), PermessoArchivio.DecimalePredicato.VISUALIZZA))
            throw new Http403ResponseException("1", "Utente senza permesso di visualizzare l'archivio");
        String scheme = request.getScheme();
        String hostname = CommonUtils.getHostname(request);
        Integer port = request.getServerPort();

        String downloadUrl = this.configParams.getDownloaderUrl(scheme, hostname, port);
        String uploaderUrl = this.configParams.getUploaderUrl(scheme, hostname, port);
        GenerazioneZipArchivioJobWorkerData data = new GenerazioneZipArchivioJobWorkerData(
                persona,
                archivio,
                downloadUrl,
                uploaderUrl,
                "Servizio per generare lo zip dell'archivio e fornire il download"
        );
        GenerazioneZipArchivioJobWorker worker = masterjobsObjectsFactory.getJobWorker(
                    GenerazioneZipArchivioJobWorker.class,
                    data,
                    false
        );
        try {
            masterjobsJobsQueuer.queue(
                    worker, 
                    null, 
                    null, 
                    Applicazione.Applicazioni.gedi.toString(), 
                    false, 
                    it.bologna.ausl.model.entities.masterjobs.Set.SetPriority.NORMAL
            );
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = "Errore nell'accodamento del job CalcoloPermessiGerarchiaArchivio";
            log.error(errorMessage);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }

//        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
//        scriptaArchiviUtils.createZipArchivio(archivio, persona, response, jPAQueryFactory);
          
        LOG.info("downloadArchivioZip: {} completato.", idArchivio);
        if (authenticatedUserProperties.getRealUser() == null || !userInfoService.isSD(authenticatedUserProperties.getRealUser()))
            krintScriptaService.writeArchivioUpdate(archivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_SCARICA_ZIP_FASCICOLO);
    }

    private JSONObject getJSONObjectPecMessageDetail(Doc doc) {
        JSONObject pecMessageDetail = new JSONObject();
        Related mittente = scriptaUtils.getMittentePE(doc);
        Message message = scriptaUtils.getPecMittenteMessage(doc);
        pecMessageDetail.put("idSorgentePec", message.getId());
        pecMessageDetail.put("subject", message.getSubject());
        pecMessageDetail.put("mittente", mittente.getDescrizione());
//        pecMessageDetail.put("dataArrivo", message.getReceiveTime());
        Spedizione spedizioneMittente = scriptaUtils.getSpedizioneMittente(mittente);
        ZonedDateTime data = spedizioneMittente.getData();
        pecMessageDetail.put("dataArrivo", data.toLocalDateTime());
        pecMessageDetail.put("messageID", message.getUuidMessage());
        Pec pecDaCuiProtocollo = pecRepository.findById(message.getIdPec().getId()).get();
        pecMessageDetail.put("indirizzoPecOrigine", pecDaCuiProtocollo.getIndirizzo());

        return pecMessageDetail;
    }

    private List<Related> getRelatedDestinatari(Doc doc) {
        List<Related> related = new ArrayList();

        List<Related> competenti = doc.getCompetenti();
        related.addAll(competenti);

        List<Related> coinvolti = doc.getCoinvolti();
        related.addAll(coinvolti);

        return related;
    }

    private Map<String, Object> getParametersMap(Doc doc) throws JsonProcessingException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente loggedUser = authenticatedUserProperties.getUser();
        // TODO: oggetto dei parametri poi tradotto in string. trasformare i json parametri in mappa
        Map<String, Object> parametersMap = new HashMap();
        parametersMap.put("azienda", doc.getIdAzienda().getCodiceRegione() + doc.getIdAzienda().getCodice());
        parametersMap.put("applicazione_chiamante", authenticatedUserProperties.getApplicazione().toString());
//        parametersMap.put("numero_documento_origine", doc.getId().toString());
        parametersMap.put("id_doc_esterno", doc.getId());
//        parametersMap.put("anno_documento_origine", doc.getDataCreazione().getYear());
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        String dateFormat = doc.getDataCreazione().format(formatter);
//        parametersMap.put("data_registrazione_origine", dateFormat);
        parametersMap.put("oggetto", doc.getOggetto());
        //TODO da decommentare quando ci saranno i campi sul db e bisogna mettere la data in stringa
        //parametersMap.put("data_arrivo_origine", doc.getMittenti().get(0).getSpedizioneList().get(0).getData());
        //da elimare quando ci saranno i campi sul db
//        parametersMap.put("data_arrivo_origine", dateFormat);
        parametersMap.put("utente_protocollante", loggedUser.getIdPersona().getCodiceFiscale());
        //TODO da mettere quando avremo le fascicolazioni
        //da decommentare quando avremo le tabelle della fascicolazione
        //parametersMap.put("fascicoli_babel", "fascicolo_origine_1");
        parametersMap.put("riservato", "no");
        parametersMap.put("visibilita_limitata", "no");
        Related mittentePE = scriptaUtils.getMittentePE(doc);
        parametersMap.put("mittente", buildMittente(mittentePE));

        List<Related> related = getRelatedDestinatari(doc);

        parametersMap.put("destinatari", buildDestinarari(related));
        parametersMap.put("pecMessageDetail", getJSONObjectPecMessageDetail(doc).toString());

        return parametersMap;
    }

    private MultipartFile getMultiPartFromAllegato(Allegato allegato, Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato) throws MinIOWrapperException, IOException, Http500ResponseException {
        MultipartFile multipartDaTornare = null;
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        Allegato.DettaglioAllegato dettaglioAllegatoRichiesto;
        try {
            dettaglioAllegatoRichiesto = allegato.getDettagli().getDettaglioAllegato(tipoDettaglioAllegato);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.info("errore nel recuperare il metodo get del tipo dettaglio allegato richiesto", ex);
            throw new Http500ResponseException("1", "Errore generico, probabile dato malformato");
        }
        InputStream allegatoIS = minIOWrapper.getByFileId(dettaglioAllegatoRichiesto.getIdRepository()
        );
        String nomeFileConEstensione = allegato.getNome()
                + "." + dettaglioAllegatoRichiesto.getEstensione();

        multipartDaTornare = new MockMultipartFile(
                nomeFileConEstensione,
                nomeFileConEstensione,
                dettaglioAllegatoRichiesto.getMimeType(),
                allegatoIS);
        return multipartDaTornare;
    }

    private MultipartFile manageAndReturnAllegatoPrincipaleMultipart(Doc doc) throws MinIOWrapperException, IOException, Http500ResponseException {
        Allegato allegatoPrincipale = scriptaUtils.getAllegatoPrincipale(doc);
        MultipartFile multipartPrincipale = null;
        if (allegatoPrincipale != null) {
            multipartPrincipale = getMultiPartFromAllegato(allegatoPrincipale, Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE);
        }
        return multipartPrincipale;
    }

    private List<MultipartFile> manageAndReturnAllegatiNonPrincipaliMultiPartList(Doc doc) throws MinIOWrapperException, IOException, Http500ResponseException {
        List<MultipartFile> multipartList = new ArrayList();
        List<Allegato> allegati = doc.getAllegati();
        for (Allegato allegato : allegati) {
            if (!allegato.getPrincipale()) {
                //devo prendere gli 'ORIGINALI' NON FIGLI
                MultipartFile multipart = getMultiPartFromAllegato(allegato,
                        Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE);
                multipartList.add(multipart);
            }
        }
        return multipartList;
    }

    @RequestMapping(value = "createPE", method = RequestMethod.POST)
    public ResponseEntity<?> createPE(
            @RequestParam("id_doc") Integer idDoc,
            HttpServletRequest request) throws HttpInternautaResponseException,
            Throwable {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona loggedPersona = nonCachedEntities.getPersona(authenticatedUserProperties.getPerson().getId());

        Optional<Doc> docOp = docRepository.findById(idDoc);
        Doc doc;
        if (docOp.isPresent()) {
            doc = docOp.get();
        } else {
            return null;
        }

        Map<String, Object> parametersMap = getParametersMap(doc);
        List<Allegato> allegati = doc.getAllegati();

        // Se non ho allegati lancio errore
        if (!(allegati != null && !allegati.isEmpty())) {
            throw new Throwable("Allegati non presenti");
        }

        MultipartFile multipartPrincipale = manageAndReturnAllegatoPrincipaleMultipart(doc);
        List<MultipartFile> multipartList = manageAndReturnAllegatiNonPrincipaliMultiPartList(doc);

        Boolean minIOActive = false;
        List<ParametroAziende> mongoAndMinIOActive = parametriAziende.getParameters("mongoAndMinIOActive");
        if (mongoAndMinIOActive != null && !mongoAndMinIOActive.isEmpty()) {
            minIOActive = parametriAziende.getValue(mongoAndMinIOActive.get(0), Boolean.class);
        }
        generatePE.init(
                loggedPersona.getCodiceFiscale(),
                parametersMap,
                multipartPrincipale,
                Optional.of(multipartList),
                aziendeConnectionManager.getAziendeParametriJson(),
                minIOActive,
                aziendeConnectionManager.getMinIOConfig(),
                true // dobbiamo evitare l'estrazione ricorsiva degli allegati
        );

        String resultJson = generatePE.create(null);
        LOG.info("generatePE.create() ha tornato '" + resultJson + "'");
        if (!StringUtils.hasText(resultJson)) {
            throw new Throwable("Errore nella protocollazione del PE");
        }

        Map<String, Object> resObj = objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {
        });
        saveRegistriDoc(resObj, doc, loggedPersona);

        ResponseEntity res = ResponseEntity.ok(resObj);
        return res;
    }

    private void saveRegistriDoc(Map<String, Object> resObj, Doc doc, Persona loggedPersona) throws JsonProcessingException {
        Integer numeroProtocollo = Integer.parseInt((String) resObj.get("numeroProtocollo"));
        Integer annoProtocollo = (Integer) resObj.get("annoProtocollo");
        String numeroPropostaConAnno = (String) resObj.get("numeroProposta");
        Integer numeroProposta = Integer.parseInt(numeroPropostaConAnno.split("-")[1]);
        Integer annoProposta = Integer.parseInt(numeroPropostaConAnno.split("-")[0]);
        Integer idStrutturaProtocollante = (Integer) resObj.get("idStrutturaProtocollante");

        Struttura struttura = nonCachedEntities.getStruttura(idStrutturaProtocollante);
        Registro registroPropostaPico = nonCachedEntities.getRegistro(doc.getIdAzienda().getId(), Registro.CodiceRegistro.PROP_PG);
        Registro registroProtocolloPico = nonCachedEntities.getRegistro(doc.getIdAzienda().getId(), Registro.CodiceRegistro.PG);

        String dataRegistrazioneString = (String) resObj.get("dataRegistrazione");
        LocalDateTime dataRegistrazioneLocal = LocalDateTime.parse(dataRegistrazioneString);

        RegistroDoc proposta = new RegistroDoc();
        proposta.setAnno(annoProposta);
        proposta.setDataRegistrazione(ZonedDateTime.of(dataRegistrazioneLocal, ZoneId.systemDefault()));
        proposta.setIdDoc(doc);
        proposta.setIdPersonaRegistrante(loggedPersona);
        proposta.setIdStrutturaRegistrante(struttura);
        proposta.setNumero(numeroProposta);
        proposta.setIdRegistro(registroPropostaPico);

        RegistroDoc protocollo = new RegistroDoc();
        protocollo.setAnno(annoProtocollo);
        protocollo.setDataRegistrazione(ZonedDateTime.of(dataRegistrazioneLocal, ZoneId.systemDefault()));
        protocollo.setIdDoc(doc);
        protocollo.setIdPersonaRegistrante(loggedPersona);
        protocollo.setIdStrutturaRegistrante(struttura);
        protocollo.setNumero(numeroProtocollo);
        protocollo.setIdRegistro(registroProtocolloPico);

        registroDocRepository.saveAll(Arrays.asList(proposta, protocollo));
    }

    @Transactional(rollbackFor = Throwable.class)
    private Allegato saveFileOnInternauta(Allegato allegato) {
        Allegato saved = allegatoRepository.save(allegato);
        return saved;
    }

    private Map<String, Object> buildMittente(Related mittenteDoc) throws JsonProcessingException {
        Map<String, Object> mittente = new HashMap();
        mittente.put("descrizione", mittenteDoc.getDescrizione());
        Spedizione spedizioneMittente = scriptaUtils.getSpedizioneMittente(mittenteDoc);
        mittente.put("indirizzo_spedizione", spedizioneMittente.getIndirizzo().getCompleto());
        Mezzo mezzo = spedizioneMittente.getIdMezzo();
        mittente.put("mezzo_spedizione", mezzo.ottieniCodiceArgo());
        return mittente;
    }

    private List<Map<String, Object>> buildDestinarari(List<Related> destinarariDoc) {
        List<Map<String, Object>> destinarari = new ArrayList();

        for (Related destinatario : destinarariDoc) {
            Map<String, Object> AoCC = new HashMap();
            AoCC.put("tipo", destinatario.getTipo().toString());
            // TODO:
            //mettere gli assegnatari quando si avranno sul db
            //dal contatto devo beccare la struttura
            //recuperare cf della persona dal contatto
            //da sistemare quando si potranno mettere in interfaccia
            //AoCC.put("assegnatari", destinatario.getIdContatto().getDettaglioContattoList().get(0).getIdContattoEsterno());
            //probabilmente da modificare con la spedizione
            AoCC.put("struttura", destinatario.getIdContatto().getIdEsterno().toString());

            //da settare quando si avra il reponsabile del procedimento
            //AoCC.put("utente_responsabile", destinatario.getIdContatto().getIdEsterno());
            destinarari.add(AoCC);
        }
        return destinarari;
    }

    public static String getHashFromFile(InputStream is, String algorithmName) throws FileNotFoundException, IOException, NoSuchAlgorithmException {

        MessageDigest algorithm = MessageDigest.getInstance(algorithmName);
        DigestInputStream dis = new DigestInputStream(is, algorithm);

        byte[] buffer = new byte[8192];
        while ((dis.read(buffer)) != -1) {
        }
        dis.close();
        byte[] messageDigest = algorithm.digest();

        Formatter fmt = new Formatter();
        for (byte b : messageDigest) {
            fmt.format("%02X", b);
        }
        String hashString = fmt.toString();

//        BigInteger hashInt = new BigInteger(1, messageDigest);
//        String hashString = hashInt.toString(16);
//        int digestLength = algorithm.getDigestLength();
//        while (hashString.length() < digestLength * 2) {
//            hashString = "0" + hashString;
//        }
        return hashString;
    }
    
    /**
     * SE arrivi qui e vedi che è passato il1 15 giugno 2022 cancella sto metodo commentato
     */
    /**
    @RequestMapping(value = "getResponsabili", method = RequestMethod.GET)
    public JSONObject getResponsabili(@RequestParam("id") String idArchivio) throws Http500ResponseException {
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        Archivio archivio = archivioRepository.getById(Integer.parseInt(idArchivio));
        ArchivioDetail dettaglio = archivioDetailRepository.getById(Integer.parseInt(idArchivio));
        Persona personaResponsabile = dettaglio.getIdPersonaResponsabile();
        Integer[] idVicari = dettaglio.getIdVicari();
        List<Persona> listVicari = new ArrayList();
        for (Integer id : idVicari) {
            Optional<Persona> p = personaRepository.findById(id);
            listVicari.add(p.get());
        }
        json.put("descrizione", personaResponsabile.getDescrizione());
        json.put("ruolo", "Responsabile");
        json.put("id", personaResponsabile.getId());
        json.put("struttura", dettaglio.getIdStruttura().getNome());
        jsonArray.add(json);
        for (Persona vic : listVicari) {
            json = new JSONObject();
            json.put("descrizione", vic.getDescrizione());
            json.put("ruolo", "Vicario");
            json.put("id", vic.getId());
            json.put("struttura", dettaglio.getIdStruttura().getNome());
            jsonArray.add(json);
        }
        JSONObject jsonReturn = new JSONObject();
        jsonReturn.put("responsabili", jsonArray);
        return jsonReturn;
    }*/
    
    @RequestMapping(value = "eliminaProposta", method = RequestMethod.POST)
    public ResponseEntity<?> eliminaProposta(
            @RequestParam("guid_doc") String guidDoc,
            @RequestParam("id_applicazione") String idApplicazione,
            @RequestParam("id_azienda") String idAzienda) throws HttpInternautaResponseException,
            Throwable {
        Azienda azienda = aziendaRepository.findById(Integer.parseInt(idAzienda)).get();

        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();

        String cf = "";
        String cfReale = "";
        try {
            cf = authenticatedUserProperties.getPerson().getCodiceFiscale();
            cfReale = authenticatedUserProperties.getRealPerson().getCodiceFiscale();
        } catch (Exception e) {
            cfReale = authenticatedUserProperties.getPerson().getCodiceFiscale();
        }

        Applicazione applicazione = cachedEntities.getApplicazione(idApplicazione);
        AziendaParametriJson parametriAzienda = azienda.getParametri();
        String url = String.format("%s%s%s", parametriAzienda.getBabelSuiteWebApiUrl(), applicazione.getBaseUrl(), EliminaPropostaDaEdiUrl);
//        String url = "http://localhost:8080/Procton/EliminaPropostaDaEdi";
//        String url = "http://localhost:8080/Dete/EliminaPropostaDaEdi";
//        String url = "http://localhost:8080/Deli/EliminaPropostaDaEdi";

        Map<String, String> hm = new HashMap<String, String>();
        hm.put("guidDoc", guidDoc);
        hm.put("idApplicazione", idApplicazione);
        hm.put("idAzienda", idAzienda);
        hm.put("cf", cf);
        hm.put("cfReale", cfReale);

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.get("application/json; charset=utf-8"),
                objectMapper.writeValueAsString(hm));
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(12, TimeUnit.MINUTES).build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("X-HTTP-Method-Override", eliminaPropostaDaEdiMethod)
                .build();

        Call call = client.newCall(request);
        HashMap readValue = null;
        try ( Response response = call.execute();) {
            int responseCode = response.code();
            if (response.isSuccessful()) {
                readValue = objectMapper.readValue(response.body().string(), HashMap.class);
//                r.put(resp);
                log.info("Chiamata a webapi inde effettuata con successo");
                docDetailRepository.deleteByGuidDocumento(guidDoc);
            } else {
                log.info("Errore nella chiamata alla webapi InDe: " + responseCode + " " + response.message());
                throw new IOException(String.format("Errore nella chiamata alla WepApi InDe: %s", response.message()));
            }

        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }

        return new ResponseEntity(readValue, HttpStatus.OK);
    }

    @RequestMapping(value = "numeraArchivio", method = RequestMethod.POST)
    @Transactional(rollbackFor = Throwable.class)
    public Object numeraArchivio(@RequestParam("idArchivio") Integer idArchivio,
            @RequestParam("projection") String projection,
            HttpServletRequest request) throws HttpInternautaResponseException,
            Throwable {
        log.info("Numerazione archivio: " + idArchivio + "...");
        Integer numeroGenerato = archivioRepository.numeraArchivio(idArchivio);
        log.info("Numero generato: " + numeroGenerato);
        //DA QUESTO MOMENTO I DATI DI NUMERO, ANNO, NUMERAZIONE GER. SONO GIA' SALVATI SUL DB
        // Ricarico i dati
        log.info("Reload...");
        Archivio archivioNumerato = archivioRepository.getById(idArchivio);
        log.info("Numero: " + archivioNumerato.getNumero());
        log.info("Anno: " + archivioNumerato.getAnno());
        log.info("Numerazione Gerarchica: " + archivioNumerato.getNumerazioneGerarchica());

        // Ritorno la projection coi dati aggiornati
        log.info("Recupero projection by name " + projection);
        Class<?> projectionClass = restControllerEngine.getProjectionClass(projection, archivioRepository);
        projectionsInterceptorLauncher.setRequestParams(null, request);
        log.info("Chiamo la facrtory della projection...");
        Object projectedObject = projectionFactory.createProjection(
                projectionClass, archivioNumerato
        );
        
        if (krintUtils.doIHaveToKrint(request)) {
            // Utilizziamo il writeArchivioCreation al posto dell'Update perché fa già il controllo necessario sul livello
            // e scrive il log sul padre, quindi non c'è bisogno di rifare di nuovo i controlli
            krintScriptaService.writeArchivioCreation(archivioNumerato, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_NUMERO_UPDATE);
        }
        log.info("Ritorno la projectionCreata");
        return projectedObject;
    }

    /**
     * Dato un archivio radice accoda un job che calcola i permessi
     * espliciti dell'intera gerarchia
     *
     * @param idArchivioRadice
     * @param request
     * @return
     * @throws
     * it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    @RequestMapping(value = "calcolaPermessiEsplicitiGerarchiaArchivio", method = RequestMethod.POST)
    public ResponseEntity<?> calcolaPermessiEsplicitiGerarchiaArchivio(
            @RequestParam("idArchivioRadice") Integer idArchivioRadice,
            HttpServletRequest request) throws MasterjobsWorkerException {

        Applicazione applicazione = cachedEntities.getApplicazione("scripta");
        AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
        accodatoreVeloce.accodaCalcolaPermessiGerarchiaArchivio(idArchivioRadice, idArchivioRadice.toString(), "scripta_archivio", applicazione);
//        accodatoreVeloce.accodaCalcolaPersoneVedentiDaArchiviRadice(new HashSet(Arrays.asList(idArchivioRadice)), idArchivioRadice.toString(), "scripta_archivio", applicazione);

        return new ResponseEntity("", HttpStatus.OK);
    }
    
    /**
     * Dato un archivio accoda un job che calcola i permessi del'archivio
     *
     * @param idArchivio
     * @param request
     * @return
     * @throws
     * it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    @RequestMapping(value = "calcolaPermessiEsplicitiArchivio", method = RequestMethod.POST)
    public ResponseEntity<?> calcolaPermessiEsplicitiArchivio(
            @RequestParam("idArchivio") Integer idArchivio,
            HttpServletRequest request) throws MasterjobsWorkerException {

        Applicazione applicazione = cachedEntities.getApplicazione("scripta");
        AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
        accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivio, idArchivio.toString(), "scripta_archivio", applicazione.getId());
//        accodatoreVeloce.accodaCalcolaPersoneVedenti(new HashSet(Arrays.asList(idArchivioRadice)), idArchivioRadice.toString(), "scripta_archivio", applicazione);
        QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
        List<Integer> idDocsDaArchivio = jpaQueryFactory
                .select(qArchivioDoc.idDoc.id)
                .from(qArchivioDoc)
                .where(qArchivioDoc.idArchivio.id.eq(idArchivio))
                .fetch();
        log.info("idDocsDaArchivi calcolati");
        if (idDocsDaArchivio != null) {
            log.info("idDocsDaArchivi non e' null");
            for (Integer idDoc : idDocsDaArchivio) {
                accodatoreVeloce.accodaCalcolaPersoneVedentiDoc(idDoc, idArchivio.toString(), "scripta_archivio", applicazione);
            }
        }

        return new ResponseEntity("", HttpStatus.OK);
    }

    /**
     * Dato l'idEsterno di un Doc, la funzione torna una lista contentente gli
     * idPersona di tutti coloro che hanno un permesso con bit >= di minBit
     * negli archivi in cui il doc è archiviato
     *
     * @param idEsterno
     * @param minBit
     * @param response
     * @param request
     * @return
     */
    @RequestMapping(value = "getIdPersoneConPermessoSuArchiviazioniDelDocByIdEsterno/{idEsterno}/{minBit}", method = RequestMethod.GET)
    public ResponseEntity<?> getIdPersoneConPermessoSuArchiviazioniDelDocByIdEsterno(
            @PathVariable(required = true) String idEsterno,
            @PathVariable(required = false) Integer minBit,
            HttpServletResponse response,
            HttpServletRequest request) {
        if (minBit == null) {
            minBit = 0;
        }
        List<Integer> idPersone = permessoArchivioRepository.getIdPersoneConPermessoSuArchiviazioniDelDocByIdEsterno(idEsterno, minBit);
        return new ResponseEntity(idPersone, HttpStatus.OK);
    }

    /**
     * Servlet chiamata quando l'utente vuole caricare dei file su un archivio
     *
     * @param request
     * @param files
     * @param idArchivio
     * @return
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     * @throws java.security.NoSuchAlgorithmException
     */
    @RequestMapping(value = "uploadDocument", method = RequestMethod.POST)
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<?> uploadDocument(
            HttpServletRequest request,
            @RequestPart("documents") MultipartFile[] files,
            @RequestParam("idArchivio") int idArchivio
    ) throws IOException, FileNotFoundException, NoSuchAlgorithmException, MinIOWrapperException, Http403ResponseException, BlackBoxPermissionException {

        projectionsInterceptorLauncher.setRequestParams(null, request); // Necessario per poter poi creare una projection
        Archivio archivio = archivioRepository.findById(idArchivio).get();
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();

        if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), archivio.getId(), PermessoArchivio.DecimalePredicato.MODIFICA)) {
            throw new Http403ResponseException("3", "Utente senza permesso di modificare l'archivio");
        }

        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        List<Integer> idDocList = new ArrayList();
        try {
            for (MultipartFile file : files) {
                Doc doc = new Doc(file.getOriginalFilename(), authenticatedUserProperties.getPerson(), archivio.getIdAzienda(), DocDetailInterface.TipologiaDoc.DOCUMENT_UTENTE.toString());
                doc = docRepository.save(doc);
                em.refresh(doc);
                idDocList.add(doc.getId());
                scriptaUtils.creaAndAllegaAllegati(doc, file.getInputStream(), file.getOriginalFilename(), true);

                //archvivio il document
                ArchivioDoc archiviazione = new ArchivioDoc(archivio, doc, persona);
                ArchivioDoc save = archivioDocRepository.save(archiviazione);
                archivioDiInteresseRepository.aggiungiArchivioRecente(archivio.getIdArchivioRadice().getId(), persona.getId());
                
                PersonaVedente pv = new PersonaVedente();
                pv.setIdAzienda(doc.getIdAzienda());
                pv.setIdDocDetail(doc.getIdDocDetail());
                pv.setIdPersona(persona);
                pv.setDataCreazione(doc.getDataCreazione());
                pv.setPienaVisibilita(Boolean.TRUE);
                pv.setMioDocumento(Boolean.TRUE);
                personaVedenteRepository.save(pv);
                
                AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
                accodatoreVeloce.accodaCalcolaPersoneVedentiDoc(doc.getId());
                if (krintUtils.doIHaveToKrint(request)) {
                    krintScriptaService.writeArchivioDoc(save, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_DOC_LOAD);
                }
            }
        } catch (Exception e) {
            LOG.error("Errore nell'upload del doc",e);
            if (savedFilesOnRepository != null && !savedFilesOnRepository.isEmpty()) {
                for (MinIOWrapperFileInfo minIOWrapperFileInfo : savedFilesOnRepository) {
                    minIOWrapper.removeByFileId(minIOWrapperFileInfo.getFileId(), false);
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return new ResponseEntity(idDocList, HttpStatus.OK);
        //return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequestMapping(value = "archiveMessage/{idMessage}/{idArchivio}/{nomeDocDaPec}", method = RequestMethod.POST)
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<?> archiveMessage(
            HttpServletRequest request,
            @PathVariable(required = true) Integer idMessage,
            @PathVariable(required = true) Integer idArchivio,
            @PathVariable(required = true) String nomeDocDaPec
    ) throws IOException, FileNotFoundException, NoSuchAlgorithmException, BlackBoxPermissionException, Http404ResponseException, Http403ResponseException, BadParamsException, MinIOWrapperException, Http500ResponseException, MasterjobsWorkerException, Http409ResponseException {
        projectionsInterceptorLauncher.setRequestParams(null, request); // Necessario per poter poi creare una projection

        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Utente utente = authenticatedUserProperties.getUser();
        Archivio archivio = archivioRepository.findById(idArchivio).get();
        Message message = messageRepository.findById(idMessage).get();
        Azienda azienda = archivio.getIdAzienda();
        Integer idDoc;
        try {
            idDoc = scriptaArchiviUtils.archiveMessage(message, nomeDocDaPec, archivio, persona, azienda, utente);
        } catch (DataIntegrityViolationException ex) {
            String errore = "Il documento è già presente nel fascicolo selezionato.";
            throw new Http409ResponseException("409", errore);
        }
        AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
        accodatoreVeloce.accodaCalcolaPersoneVedentiDoc(idDoc);
        
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequestMapping(value = "copiaArchiviazioni", method = RequestMethod.POST)
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<?> copiaArchiviazioni(
            HttpServletRequest request,
            @RequestBody Map<String, String> requestData
    ) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Doc docOrigine = docRepository.findByIdEsterno(requestData.get("guidDocumentoOrigine"));
        Doc docDestinazione = docRepository.findByIdEsterno(requestData.get("guidDocumentoDestinazione"));
        return new ResponseEntity(scriptaCopyUtils.copiaArchiviazioni(docOrigine, docDestinazione, persona), HttpStatus.OK);
    }

    @RequestMapping(value = "aggiungiArchivioRecente", method = RequestMethod.POST)
    public ResponseEntity<?> aggiungiArchivioRecente(
            @RequestParam("idArchivio") Integer idArchivio,
            HttpServletRequest request) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Archivio archivioRadice = archivioRepository.findById(idArchivio).get();
        ZonedDateTime data_recentezza = ZonedDateTime.now();
        Optional<ArchivioRecente> archivio = archivioRecenteRepository.getArchivioFromPersonaAndArchivio(idArchivio, persona.getId());
        boolean isPresent = archivio.isPresent();
        if (isPresent) {
            ArchivioRecente archivioUpdate = archivio.get();
            archivioUpdate.setDataRecentezza(data_recentezza);
            archivioRecenteRepository.save(archivioUpdate);
        } else {
            ArchivioRecente archivioUpdate = new ArchivioRecente();
            archivioUpdate.setIdArchivio(archivioRadice.getIdArchivioDetail());
            archivioUpdate.setIdPersona(persona);
            archivioUpdate.setDataRecentezza(data_recentezza);
            archivioRecenteRepository.save(archivioUpdate);
        }
        return new ResponseEntity("", HttpStatus.OK);
    }

    @RequestMapping(value = "archiviaRgInFascicoloSpeciale", method = RequestMethod.POST)
    public ResponseEntity<?> archiviaRgInFascicoloSpeciale(
            @RequestBody Map<String, String> registroGiornaliero,
            HttpServletRequest request) throws BlackBoxPermissionException, Http500ResponseException, JsonProcessingException {

        log.info("sono dentro il controller per archiviare i registri giornalieri in internauta");
        log.info(objectMapper.writeValueAsString(registroGiornaliero));
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Azienda azienda = aziendaRepository.findByCodice(registroGiornaliero.get("codiceAzienda"));
        Integer anno = Integer.parseInt(registroGiornaliero.get("anno"));

        Doc doc = new Doc();
        try {
            doc.setIdEsterno((String) registroGiornaliero.get("id"));
            doc.setTipologia(objectMapper.readValue(registroGiornaliero.get("codice_registro"), DocDetailInterface.TipologiaDoc.class));
            doc.setIdAzienda(azienda);
            doc = docRepository.save(doc);
        } catch (Exception ex) {
            log.error("errore nella creazione del doc internauta", ex);
            // Forse esisteva già per via del cannone quindi lo recupero
            doc = docRepository.findByIdEsterno((String) registroGiornaliero.get("id"));
            if (doc == null) {
                throw new Http500ResponseException("2", "Documento non trovato. E non creabile");
            }
        }

        Integer numeroSottoarchivioSpeciale = null;
        switch ((String) registroGiornaliero.get("codice_registro")) {
            case "RGPICO":
                numeroSottoarchivioSpeciale = 1;
                break;
            case "RGDETE":
                numeroSottoarchivioSpeciale = 2;
                break;
            case "RGDELI":
                numeroSottoarchivioSpeciale = 3;
                break;
        }
        QArchivio qArchivioSpeciale = QArchivio.archivio;
        BooleanExpression filter = qArchivioSpeciale.tipo.eq("SPECIALE")
                .and(qArchivioSpeciale.idAzienda.eq(azienda))
                .and(qArchivioSpeciale.livello.eq(3))
                .and(qArchivioSpeciale.anno.eq(anno))
                .and(qArchivioSpeciale.numero.eq(numeroSottoarchivioSpeciale));
        Optional<Archivio> archivioSpeciale = archivioRepository.findOne(filter);
        if (archivioSpeciale.isPresent()) {
            log.info("ho trovato il fascicolo speciale");
            if (!archivioDocRepository.exists(QArchivioDoc.archivioDoc.idArchivio.id.eq(archivioSpeciale.get().getId()).and(QArchivioDoc.archivioDoc.idDoc.id.eq(doc.getId())))) {
                log.info("non essite la fascicolazione quindi la eseguo");
                ArchivioDoc archivioDoc = new ArchivioDoc();
                archivioDoc.setIdArchivio(archivioSpeciale.get());
                archivioDoc.setIdDoc(doc);
                Persona babelBDS = personaRepository.getById(1);
                archivioDoc.setIdPersonaArchiviazione(babelBDS);
                archivioDocRepository.save(archivioDoc);
            } else {
                log.warn(String.format("La fascicolazione del registro %s nel fascicolo speciale %s esiste già", doc.getId(), archivioSpeciale.get().getId()));
            }

        } else {
            log.error("non ho trovato il fascicolo speciale");
            throw new Http500ResponseException("1", "Non ho trovato il fascicolo speciale");
        }

        log.info("Ho terminato l'archiviazione");
        return new ResponseEntity("", HttpStatus.OK);
    }

    @RequestMapping(value = "archivioHasDoc", method = RequestMethod.POST)
    public boolean archivioHasDoc(
            @RequestParam("idArchivio") String idArchivio,
            HttpServletRequest request) {
        Integer idArchivioInt = Integer.parseInt(idArchivio);
        Optional<Archivio> a = archivioRepository.findById(idArchivioInt);
        if (a.isPresent()) {
            Archivio archivio = a.get();
            List<ArchivioDoc> documenti = archivioDocRepository.findByIdArchivio(archivio);
            if (documenti.size() > 0) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @RequestMapping(value = "deleteArchivio", method = RequestMethod.POST)
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<?> deleteArchivio(
            @RequestParam("idArchivio") String idArchivio,
            HttpServletRequest request) throws BlackBoxPermissionException, Http403ResponseException {
        Integer idArchivioInt = Integer.parseInt(idArchivio);
        Optional<Archivio> a = archivioRepository.findById(idArchivioInt);
        if (a.isPresent()) {
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
            Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
            if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), idArchivioInt, PermessoArchivio.DecimalePredicato.RESPONSABILE))
                throw new Http403ResponseException("1", "Utente non ha il permesso per fare questa operazione.");
            Archivio entity = a.get();
            archivioRepository.delete(entity);
            
            boolean iHaveToKrint = krintUtils.doIHaveToKrint(request);
            if (iHaveToKrint)
                krintScriptaService.writeArchivioDelete(entity, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_DELETE);
            return new ResponseEntity("", HttpStatus.OK);
        }
        return new ResponseEntity("", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "archiviaDeteDeliInFascicoloSpeciale", method = RequestMethod.POST)
    public ResponseEntity<?> archiviaDeteDeliInFascicoloSpeciale(
            @RequestBody Map<String, String> datiFascicolazione,
            HttpServletRequest request) throws BlackBoxPermissionException, Http500ResponseException, JsonProcessingException {

        Azienda azienda = aziendaRepository.findByCodice(datiFascicolazione.get("codice_azienda"));
        Integer anno = Integer.parseInt(datiFascicolazione.get("anno"));
        log.info(String.format("si tratta di inserire una %s", datiFascicolazione.get("registro")));
        Integer numeroSottoarchivioSpeciale = null;
        switch ((String) datiFascicolazione.get("registro")) {
            case "DETE":
                numeroSottoarchivioSpeciale = 2;
                break;
            case "DELI":
                numeroSottoarchivioSpeciale = 3;
                break;
        }
        log.info(String.format("invece metto nel fascicolo %s", numeroSottoarchivioSpeciale));
        QArchivio qArchivioSpeciale = QArchivio.archivio;
        BooleanExpression filter = qArchivioSpeciale.tipo.eq("SPECIALE")
                .and(qArchivioSpeciale.idAzienda.eq(azienda))
                .and(qArchivioSpeciale.livello.eq(2))
                .and(qArchivioSpeciale.anno.eq(anno))
                .and(qArchivioSpeciale.numero.eq(numeroSottoarchivioSpeciale));
        Optional<Archivio> archivioSpeciale = archivioRepository.findOne(filter);

        Doc doc = new Doc();
        try {
            doc = docRepository.findByIdEsterno(datiFascicolazione.get("guid"));
        } catch (Exception ex) {
            throw new Http500ResponseException("2", "Documento non trovato.");
        }

        if (archivioSpeciale.isPresent()) {
            log.info("ho trovato il fascicolo speciale");
            if (!archivioDocRepository.exists(QArchivioDoc.archivioDoc.idArchivio.id.eq(archivioSpeciale.get().getId()).and(QArchivioDoc.archivioDoc.idDoc.id.eq(doc.getId())))) {
                log.info("non esite la fascicolazione quindi la eseguo");
                ArchivioDoc archivioDoc = new ArchivioDoc();
                archivioDoc.setIdArchivio(archivioSpeciale.get());
                archivioDoc.setIdDoc(doc);
                Persona babelBDS = personaRepository.getById(1);
                archivioDoc.setIdPersonaArchiviazione(babelBDS);
                archivioDocRepository.save(archivioDoc);
            } else {
                log.warn(String.format("La fascicolazione di %s nel fascicolo speciale %s esiste già", doc.getId(), archivioSpeciale.get().getId()));
            }

        } else {
            log.error("non ho trovato il fascicolo speciale");
            throw new Http500ResponseException("1", "Non ho trovato il fascicolo speciale");
        }

        return new ResponseEntity("", HttpStatus.OK);
    }
    
    @RequestMapping(value = "spostaArchivio", method = RequestMethod.POST)
    @Transactional
    public Object spostaArchivio(
            @RequestParam("idArchivio") String idArchivio,
            @RequestParam("idArchivioDestinazione") String idArchivioDestinazione,
            @RequestParam("fascicolo") boolean fascicolo,
            @RequestParam("contenuto") boolean contenuto,
            HttpServletRequest request) throws Http500ResponseException, RestControllerEngineException, BlackBoxPermissionException, Http403ResponseException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        //controllo che almeno uno e solo uno tra fascicolo e contenuto sia stato selezionato
        if ((contenuto == false && fascicolo == false) || (contenuto == true && fascicolo == true)){
            throw new Http500ResponseException("1", "Uno e solo uno tra i target fascicolo e contenuto deve essere selezionato");
        }
        //procedo a tirare su tutto ciò che mi serve sull'archivio soggetto del sposta
        Integer idArchivioInt = Integer.valueOf(idArchivio);
        if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), idArchivioInt, PermessoArchivio.DecimalePredicato.VICARIO))
            throw new Http403ResponseException("1", "Utente non ha il permesso per fare questa operazione.");
        Optional<Archivio> a = archivioRepository.findById(idArchivioInt);
        //finalArchivio è l'archivio che verrà usato per crare la projection da restituire al front end
        Archivio finalArchivio = null;
        //controllo l'effettiva presenza dell'archivio da spostare
        if (a.isPresent()) {
            Archivio archivio = a.get();
            boolean haFigli = false;
            //controllo se l'archivio da spostare ha figli
            if (!archivio.getArchiviFigliList().isEmpty()) {
                haFigli = true;
            }
            //procedo a tirare su tutto ciò che mi serve sull'archivio destinazione
            Integer idArchivioIntDestinazione = Integer.valueOf(idArchivioDestinazione);
            Optional<Archivio> aDestinazione = archivioRepository.findById(idArchivioIntDestinazione);
            //controllo l'effettiva presenza dell'archivio destinazione
            if (aDestinazione.isPresent()) {
                Archivio archivioDestinazione = aDestinazione.get();
                JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
                Archivio archivioRif = null;
                boolean iHaveToKrint = krintUtils.doIHaveToKrint(request);
                //controllo se è stato selezionato il target fascicolo e agisco spostando l'archivio con figli e documenti
                //NB: i documenti sono legati all'archivio con una tabella di cross ergo seguiranno l'archivio ovunque
                if (fascicolo) {
                    if (archivioDestinazione.getLivello() == 3){
                        throw new Http500ResponseException("3", "L'azione sposta di un archivio non si può fare verso un archivio di livello 3");
                    }
                    if (3 - scriptaArchiviUtils.getProfonditaArchivio(archivio) < archivioDestinazione.getLivello()){
                        throw new Http500ResponseException("4", "L'azione sposta non può essere eseguita perché andrebbe a creare almeno un archivio di livello 4");
                    }
                    try {
                        // copia del fascicolo per il log nel krint
                        archivioRif = objectMapper.readValue(objectMapper.writeValueAsString(archivio), Archivio.class);
                        archivioRif.setIdArchivioPadre(archivio.getIdArchivioPadre());
                    } catch (JsonProcessingException ex) {
                        log.error("errore nella copia dell'archivio per il krint");
                    }   
                    log.info(String.format("procedo a spostare l'archivio %s", archivio.getId()));
                    
                    //update con cui "sposto" l'archivio da spostare
                    jPAQueryFactory
                            .update(QArchivio.archivio)
                            .set(QArchivio.archivio.numero, 0)
                            .set(QArchivio.archivio.numerazioneGerarchica, archivioDestinazione.getNumerazioneGerarchica().replace("/", "-x/"))
                            .set(QArchivio.archivio.idArchivioPadre, archivioDestinazione)
                            .set(QArchivio.archivio.idArchivioRadice, archivioDestinazione.getIdArchivioRadice())
                            .set(QArchivio.archivio.idTitolo, archivioDestinazione.getIdTitolo())
                            .set(QArchivio.archivio.idMassimario, archivioDestinazione.getIdMassimario())
                            .set(QArchivio.archivio.livello, archivioDestinazione.getLivello() + 1)
                            .set(QArchivio.archivio.numeroSottoarchivi, 0)
                            .where(QArchivio.archivio.id.eq(archivio.getId()))
                            .execute();
                    log.info(String.format("Ho spostato l'archivio %s in %s", archivio.getId(), archivioDestinazione.getId()));
                    em.refresh(archivio);
                    //numero il nuovo archivio
                    archivioRepository.numeraArchivio(archivio.getId());
                    log.info(String.format("ho numerato l'archivio di %s", archivio.getId()));
                    scriptaCopyUtils.setNewAttoriArchivio(archivio, em);
                    
                    //grazie al controllo sulla presenza dei figli fatto in precedenza agisco di conseguenza
                    if (haFigli) {
                        log.info(String.format("procedo a modificare i figli di %s", archivio.getId()));
                        em.refresh(archivio);
                        //update con cui fixo alcuni dati sugli archivi figli di quello spostato
                        jPAQueryFactory
                                .update(QArchivio.archivio)
                                .set(QArchivio.archivio.numerazioneGerarchica, Expressions.asString(archivio.getNumerazioneGerarchica().substring(0, archivio.getNumerazioneGerarchica().indexOf("/")).concat("-")).append(QArchivio.archivio.numero.stringValue().append(QArchivio.archivio.numerazioneGerarchica.substring(QArchivio.archivio.numerazioneGerarchica.indexOf("/")))))
                                .set(QArchivio.archivio.idArchivioRadice, archivio.getIdArchivioRadice())
                                .set(QArchivio.archivio.idArchivioPadre, archivio)
                                .set(QArchivio.archivio.idTitolo, archivio.getIdTitolo())
                                .set(QArchivio.archivio.idMassimario, archivioDestinazione.getIdMassimario())
                                .set(QArchivio.archivio.livello, archivio.getLivello() + 1)
                                .set(QArchivio.archivio.numeroSottoarchivi, 0)
                                .where(QArchivio.archivio.idArchivioPadre.eq(archivio))
                                .execute();
                        log.info(String.format("finito le modifiche ai figli di %s", archivio.getId()));
                    }
                    em.refresh(archivio);
                    for(Archivio archFiglio: archivio.getArchiviFigliList()){
                        scriptaCopyUtils.setNewAttoriArchivio(archFiglio, em);                        
                    }
                    //ricalcolo i permessi per l'achivio spostato e figli
                    archivioRepository.calcolaPermessiEsplicitiGerarchia(archivio.getId());
                    finalArchivio = archivio;
                } else if (contenuto) {
                    //è stato selezionato il target contenuto e agisco spostando solo i documenti dell'archivio selezionato
                    log.info(String.format("procedo a spostare i documenti di %s", archivio.getId()));
                    
                    List<Integer> idDocsDaSpostare = jPAQueryFactory
                            .select(QArchivioDoc.archivioDoc.idDoc.id)
                            .from(QArchivioDoc.archivioDoc)
                            .where(QArchivioDoc.archivioDoc.idArchivio.eq(archivio))
                            .fetch();
                    List<Integer> idDocsDaSpostareCheCiSonoGia = jPAQueryFactory
                            .select(QArchivioDoc.archivioDoc.idDoc.id)
                            .from(QArchivioDoc.archivioDoc)
                            .where(QArchivioDoc.archivioDoc.idDoc.id.in(idDocsDaSpostare)
                                    .and(QArchivioDoc.archivioDoc.idArchivio.eq(archivioDestinazione)))
                            .fetch();
                            
                    jPAQueryFactory
                            .update(QArchivioDoc.archivioDoc)
                            .set(QArchivioDoc.archivioDoc.idArchivio, archivioDestinazione)
                            .set(QArchivioDoc.archivioDoc.dataInserimentoRiga, ZonedDateTime.now())
                            .where(QArchivioDoc.archivioDoc.idArchivio.eq(archivio)
                                    .and(QArchivioDoc.archivioDoc.idDoc.id.notIn(idDocsDaSpostareCheCiSonoGia)))
                            .execute();
                    finalArchivio = archivioDestinazione;
                }
                if (iHaveToKrint) {
                    if (contenuto) {
                        krintScriptaService.writeArchivioUpdate(archivio, finalArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_SPOSTA_CONTENUTO);
                        krintScriptaService.writeArchivioUpdate(finalArchivio, archivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_SPOSTA_CONTENUTO_DESTINAZIONE);
                    } else {                
                        krintScriptaService.writeArchivioUpdate(archivio, archivioRif, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_SPOSTA, true);
                        if (haFigli)
                            archivio.getArchiviFigliList().stream().
                                    forEach(archFiglio -> krintScriptaService.writeArchivioUpdate(archFiglio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_SPOSTA));
                    }
                }
            }
            String projection = "CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo";
            // Ritorno la projection coi dati aggiornati
            log.info("Recupero projection by name " + projection);
            Class<?> projectionClass = restControllerEngine.getProjectionClass(projection, archivioRepository);
            projectionsInterceptorLauncher.setRequestParams(null, request);
            log.info("Chiamo la facrtory della projection...");
            Object projectedObject = projectionFactory.createProjection(
                    projectionClass, finalArchivio
            );

            log.info("Ritorno la projectionCreata");
            return projectedObject;
        }
        throw new Http500ResponseException("5", "Non ho trovato nessun archivio con l'id passato");
    }
    
    @RequestMapping(value = "copiaArchivio", method = RequestMethod.POST)
    @Transactional
    public Object copiaArchivio(
            @RequestParam("idArchivio") String idArchivio,
            @RequestParam("idArchivioDestinazione") String idArchivioDestinazione,
            @RequestParam("fascicolo") boolean fascicolo,
            @RequestParam("contenuto") boolean contenuto,
            HttpServletRequest request) throws Http500ResponseException, CloneNotSupportedException, JsonProcessingException, EntityReflectionException, BlackBoxPermissionException, RestControllerEngineException, Http403ResponseException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Archivio finalArchivio = null;
        //controllo che almeno uno tra fascicolo e contenuto sia stato selezionato
        if (contenuto == false && fascicolo == false){
            throw new Http500ResponseException("1", "Deve essere selezionato almeno uno tra fascicolo e contenuto");
        }
        //procedo a tirare su tutto ciò che mi serve
        Integer idArchivioInt = Integer.valueOf(idArchivio);
        if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), idArchivioInt, PermessoArchivio.DecimalePredicato.VICARIO))
            throw new Http403ResponseException("1", "Utente non ha il permesso per fare questa operazione.");
        Optional<Archivio> a = archivioRepository.findById(idArchivioInt);
        //controllo l'effettiva presenza dell'archivio da copiare
        if (a.isPresent()) {
            Archivio archivio = a.get();
            boolean iHaveToKrint = krintUtils.doIHaveToKrint(request);
            boolean haFigli = false;
            //controllo se l'archivio da copiare ha figli
            if (!archivio.getArchiviFigliList().isEmpty()){
                haFigli = true;
            }
            //procedo a tirare su tutto ciò che mi serve sull'archivio destinazione
            Integer idArchivioIntDestinazione = Integer.valueOf(idArchivioDestinazione);
            Optional<Archivio> aDestinazione = archivioRepository.findById(idArchivioIntDestinazione);
            //controllo l'effettiva presenza dell'archivio destinazione
            if (aDestinazione.isPresent()) {
                Archivio archivioDestinazione = aDestinazione.get();
                if (archivioDestinazione.getLivello() == 3){
                    throw new Http500ResponseException("3", "L'azione copia non si può fare verso un archivio di livello 3");
                }
                if (3 - scriptaArchiviUtils.getProfonditaArchivio(archivio) < archivioDestinazione.getLivello()){
                    throw new Http500ResponseException("2", "L'azione copia non può essere eseguita perché andrebbe a creare almeno un archivio di livello 4");
                } 
                //procedo con le modifiche
                if (fascicolo) {
                    log.info(String.format("inzio a copiare %s con i suoi documenti", archivio.getId()));              
                    Archivio savedArchivio = scriptaCopyUtils.copiaArchivioConDoc(archivio, archivioDestinazione, persona, em, Boolean.TRUE, Boolean.TRUE, contenuto);
                    log.info(String.format("finito di copiare %s con i suoi documenti", archivio.getId()));
                    if (haFigli) {
                        log.info(String.format("procedo a copiare i figli di %s", archivio.getId()));
                        for (Archivio arch : archivio.getArchiviFigliList()) {
                            em.refresh(savedArchivio);
                            
                            log.info(String.format("inzio a copiare %s, figlio di %s, con i suoi documenti", arch.getId(), archivio.getId()));              
                            Archivio newArch = scriptaCopyUtils.copiaArchivioConDoc(arch, savedArchivio, persona, em, Boolean.FALSE, contenuto);
                            log.info(String.format("finito di copiare %s, figlio di %s, con i suoi documenti", arch.getId(), archivio.getId()));
                            if (iHaveToKrint) // Log nel fascicolo che è stato creato da una copia
                                krintScriptaService.writeArchivioCreation(newArch, arch, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_COPIA);
                        }
                        log.info(String.format("ho copiato anche i figli di %s", archivio.getId()));
                    }
                    finalArchivio = savedArchivio;
                    archivioRepository.copiaPermessiArchivi(archivio.getId(), finalArchivio.getId());
                    archivioRepository.calcolaPermessiEsplicitiGerarchia(finalArchivio.getId());
                }else if(contenuto){
                    log.info(String.format("procedo a copiare i documenti di %s", archivio.getId()));
                    scriptaCopyUtils.copiaArchivioDoc(archivio, archivioDestinazione, persona, em);
                    log.info(String.format("I documenti sono stati copiati correttamente dall'archivio: " + archivio.getId() + " all'archivio: " + archivioDestinazione.getId()));
                    finalArchivio = archivioDestinazione;
                }
                em.refresh(finalArchivio);
                if (iHaveToKrint) {
                    if (fascicolo && contenuto) {
                        krintScriptaService.writeArchivioUpdate(finalArchivio.getIdArchivioCopiato(), finalArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_COPIA);
                        krintScriptaService.writeArchivioCreation(finalArchivio, finalArchivio.getIdArchivioCopiato(), OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_COPIA);
                        krintScriptaService.writeArchivioUpdate(finalArchivio.getIdArchivioCopiato(), finalArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_COPIA_CONTENUTO);
                        krintScriptaService.writeArchivioUpdate(finalArchivio, finalArchivio.getIdArchivioCopiato(), OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_COPIA_CONTENUTO_DESTINAZIONE);
                    } else if (contenuto) {
                        krintScriptaService.writeArchivioUpdate(finalArchivio.getIdArchivioCopiato(), finalArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_COPIA_CONTENUTO);
                        krintScriptaService.writeArchivioUpdate(finalArchivio, finalArchivio.getIdArchivioCopiato(), OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_COPIA_CONTENUTO_DESTINAZIONE);
                    } else {                
                        krintScriptaService.writeArchivioUpdate(finalArchivio.getIdArchivioCopiato(), finalArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_COPIA);
                        krintScriptaService.writeArchivioCreation(finalArchivio, finalArchivio.getIdArchivioCopiato(), OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_COPIA);
                    }
                }
            }
            String projection = "CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo";
            log.info("Recupero projection by name " + projection);
            Class<?> projectionClass = restControllerEngine.getProjectionClass(projection, archivioRepository);
            projectionsInterceptorLauncher.setRequestParams(null, request);
            log.info("Chiamo la facrtory della projection...");
            Object projectedObject = projectionFactory.createProjection(
                    projectionClass, finalArchivio
            );
            log.info("Ritorno la projectionCreata");
            return projectedObject;
        }
        throw new Http500ResponseException("3", "Non ho trovato nessun archivio con l'id passato");
    }
    
    @RequestMapping(value = "duplicaArchivio", method = RequestMethod.POST)
    @Transactional
    public Object duplicaArchivio(
            @RequestParam("idArchivio") String idArchivio,
            @RequestParam("fascicolo") boolean fascicolo,
            @RequestParam("contenuto") boolean contenuto,
            HttpServletRequest request) throws Http500ResponseException, CloneNotSupportedException, JsonProcessingException, EntityReflectionException, BlackBoxPermissionException, RestControllerEngineException, Http403ResponseException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        if ((contenuto == false && fascicolo == false) || (contenuto == true && fascicolo == true)){
            throw new Http500ResponseException("1", "Uno e solo uno tra i target fascicolo e contenuto deve essere selezionato");
        }
        //procedo a tirare su tutto ciò che mi serve per l'archivio da duplicare
        Integer idArchivioInt = Integer.parseInt(idArchivio);
        if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), idArchivioInt, PermessoArchivio.DecimalePredicato.VICARIO))
            throw new Http403ResponseException("1", "Utente non ha il permesso per fare questa operazione.");
        Optional<Archivio> a = archivioRepository.findById(idArchivioInt);
        //controllo l'effettiva presenza dell'archivio da spostare
        if (a.isPresent()) {
            Archivio archivio = a.get();
            boolean haFigli = false;
            boolean iHaveToKrint = krintUtils.doIHaveToKrint(request);
            //controllo se l'archivio da copiare ha figli
            if (archivio.getArchiviFigliList().size() > 0){
                haFigli = true;
            }
            log.info(String.format("inzio a duplicare %s con i suoi documenti", archivio.getId()));              
            Archivio savedArchivio = scriptaCopyUtils.copiaArchivioConDoc(archivio, archivio.getIdArchivioPadre(), persona, em, Boolean.TRUE, Boolean.TRUE, contenuto);
            log.info(String.format("finito di duplicare %s con i suoi documenti", archivio.getId()));
//            
//            log.info(String.format("inizio a duplicare l'archivio %s", archivio.getId()));
//            Archivio savedArchivio = scriptaCopyUtils.copiaArchivio(archivio, archivio.getIdArchivioPadre(), persona, em);
//            log.info(String.format("finito di duplicare l'archivio %s", archivio.getId()));
//
//            if(contenuto){
//                em.refresh(savedArchivio);
//                log.info(String.format("procedo a copiare i documenti di %s", archivio.getId()));
//                scriptaCopyUtils.copiaArchivioDoc(archivio, savedArchivio, persona, em);
//                log.info(String.format("I documenti sono stati copiati correttamente dall'archivio: " + archivio.getId() + " all'archivio: " + savedArchivio.getId()));
//            }
            
            if (haFigli) {

                log.info(String.format("procedo a duplicare i figli e nipoti di %s", archivio.getId()));
                for(Archivio archFiglio : archivio.getArchiviFigliList()){
                    log.info(String.format("inzio a duplicare %s, figlio di %s, con i suoi documenti", archFiglio.getId(), archivio.getId()));              
                    Archivio savedFiglioArchivio = scriptaCopyUtils.copiaArchivioConDoc(archFiglio, savedArchivio, persona, em, Boolean.TRUE, contenuto);
                    log.info(String.format("finito di duplicare %s, figlio di %s, con i suoi documenti", archFiglio.getId(), archivio.getId()));
                    if (iHaveToKrint)
                        krintScriptaService.writeArchivioUpdate(savedFiglioArchivio, archFiglio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_DUPLICA);
//                    Archivio savedFiglioArchivio = scriptaCopyUtils.copiaArchivio(archFiglio, savedArchivio, persona, em);
//                    if(contenuto){
//                        log.info(String.format("procedo a duplicare i documenti di %s", archFiglio.getId()));
//                        scriptaCopyUtils.copiaArchivioDoc(archFiglio, savedFiglioArchivio, persona, em);
//                        log.info(String.format("I documenti sono stati duplicati correttamente dall'archivio: " + archFiglio.getId() + " all'archivio: " + savedFiglioArchivio.getId()));
//                    }
//                    em.refresh(savedFiglioArchivio);
                    for(Archivio archNipote : archFiglio.getArchiviFigliList()){
                        log.info(String.format("inzio a duplicare %s, nipote di %s, con i suoi documenti", archNipote.getId(), archivio.getId()));              
                        Archivio savedInsArchivio = scriptaCopyUtils.copiaArchivioConDoc(archNipote, savedFiglioArchivio, persona, em, Boolean.TRUE, contenuto);
                        log.info(String.format("finito di duplicare %s, nipote di %s, con i suoi documenti", archNipote.getId(), archivio.getId()));
                        if (iHaveToKrint)
                            krintScriptaService.writeArchivioUpdate(savedInsArchivio, archNipote, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_DUPLICA);
//                        Archivio savedInsArchivio = scriptaCopyUtils.copiaArchivio(archNipote, savedFiglioArchivio, persona, em);
//                        if(contenuto){
//                            log.info(String.format("procedo a duplicare i documenti di %s", archNipote.getId()));
//                            scriptaCopyUtils.copiaArchivioDoc(archNipote, savedInsArchivio, persona, em);
//                            log.info(String.format("I documenti sono stati duplicati correttamente dall'archivio: " + archNipote.getId() + " all'archivio: " + savedInsArchivio.getId()));
//                        }
                    }
                }
                log.info(String.format("finito le duplicare i figli e nipoti di %s", archivio.getId()));
            }
            em.refresh(savedArchivio);
            archivioRepository.copiaPermessiArchivi(archivio.getId(), savedArchivio.getId());
            archivioRepository.calcolaPermessiEsplicitiGerarchia(savedArchivio.getId());
            String projection = "CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo";
            log.info("Recupero projection by name " + projection);
            Class<?> projectionClass = restControllerEngine.getProjectionClass(projection, archivioRepository);
            projectionsInterceptorLauncher.setRequestParams(null, request);
            log.info("Chiamo la facrtory della projection...");
            Object projectedObject = projectionFactory.createProjection(
                    projectionClass, savedArchivio
            );
            if (iHaveToKrint) {
                krintScriptaService.writeArchivioUpdate(archivio, savedArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_DUPLICA);
                krintScriptaService.writeArchivioUpdate(savedArchivio, archivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_DUPLICA);
            }
            log.info("Ritorno la projectionCreata");
            return projectedObject;
        }
        throw new Http500ResponseException("5", "Non ho trovato nessun archivio con l'id passato");
    }
    
    @RequestMapping(value = "rendiFascicolo", method = RequestMethod.POST)
    @Transactional
    public Object rendiFascicolo(
            @RequestParam("idArchivio") String idArchivio,
            HttpServletRequest request) throws Http500ResponseException, CloneNotSupportedException, JsonProcessingException, EntityReflectionException, BlackBoxPermissionException, RestControllerEngineException, Http403ResponseException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        //procedo a tirare su tutto ciò che mi serve
        Integer idArchivioInt = Integer.parseInt(idArchivio);
        if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), idArchivioInt, PermessoArchivio.DecimalePredicato.VICARIO))
            throw new Http403ResponseException("1", "Utente non ha il permesso per fare questa operazione.");
        Optional<Archivio> a = archivioRepository.findById(idArchivioInt);
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
        //controllo l'effettiva presenza dell'archivio da spostare
        if (a.isPresent()) {
            Archivio archivio = a.get();
            Archivio archivioRif = null;
            boolean iHaveToKrint = krintUtils.doIHaveToKrint(request);
            try {
                // copia del fascicolo per il log nel krint
                archivioRif = objectMapper.readValue(objectMapper.writeValueAsString(archivio), Archivio.class);
            } catch (JsonProcessingException ex) {
                log.error("errore nella copia dell'archivio per il krint");
            }
            List<ArchivioDoc> documenti;
            boolean haFigli = false;
            //controllo se l'archivio da copiare ha figli in caso li cancello
            if (archivio.getArchiviFigliList().size() > 0){
                haFigli = true;
            }
            archivioRepository.copiaPermessiRendiFascicolo(archivio.getId());
            log.info(String.format("Ho reso fascicolo l'archivio %s", archivio.getId()));
            //numero il nuovo archivio
            archivioRepository.numeraArchivio(archivio.getId());
            em.refresh(archivio);
            log.info(String.format("ho numerato e calcolato i permessi di %s", archivio.getId()));

            if (haFigli) {
                log.info(String.format("procedo a modificare i figli di %s", archivio.getId()));
                em.refresh(archivio);
                jPAQueryFactory
                    .update(QArchivioDetail.archivioDetail)
                    .setNull(QArchivioDetail.archivioDetail.idPersonaCreazione)
                    .where(QArchivioDetail.archivioDetail.idArchivioPadre.id.eq(archivio.getId()))
                    .execute();
                jPAQueryFactory
                    .update(QArchivio.archivio)
                    .set(QArchivio.archivio.numerazioneGerarchica, Expressions.asString(archivio.getNumerazioneGerarchica().substring(0, archivio.getNumerazioneGerarchica().indexOf("/")).concat("-")).append(QArchivio.archivio.numero.stringValue().append(QArchivio.archivio.numerazioneGerarchica.substring(QArchivio.archivio.numerazioneGerarchica.indexOf("/")))))
                    .set(QArchivio.archivio.idArchivioRadice, archivio)
                    .set(QArchivio.archivio.idArchivioPadre, archivio)
                    .set(QArchivio.archivio.idTitolo, archivio.getIdTitolo())
                    .set(QArchivio.archivio.idMassimario, archivio.getIdMassimario())
                    .set(QArchivio.archivio.livello, archivio.getLivello() + 1)
                    .set(QArchivio.archivio.numeroSottoarchivi, 0)
                    .where(QArchivio.archivio.idArchivioPadre.eq(archivio))
                    .execute();
                
//                        em.refresh(archivio);
                log.info(String.format("finito le modifiche ai figli di %s", archivio.getId()));

                for (Archivio arch : archivio.getArchiviFigliList()) {
                    jPAQueryFactory
                        .update(QAttoreArchivio.attoreArchivio)
                        .set(QAttoreArchivio.attoreArchivio.dataInserimentoRiga, ZonedDateTime.now())
                        .where(QAttoreArchivio.attoreArchivio.idArchivio.id.eq(arch.getId()))
                        .execute();
                    archivioRepository.numeraArchivio(arch.getId());
                    if (iHaveToKrint)
                        krintScriptaService.writeArchivioUpdate(arch, archivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_RENDI_FASCICOLO_NUOVA_NUMERAZIONE);
                }
                log.info(String.format("ho numerato e calcolato permessi le modifiche ai figli di %s", archivio.getId()));
            }
            em.refresh(archivio);
//            archivioRepository.copiaPermessiArchivi(archivio.getId());
            archivioRepository.calcolaPermessiEsplicitiGerarchia(archivio.getId());
            String projection = "CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo";
            log.info("Recupero projection by name " + projection);
            Class<?> projectionClass = restControllerEngine.getProjectionClass(projection, archivioRepository);
            projectionsInterceptorLauncher.setRequestParams(null, request);
            log.info("Chiamo la facrtory della projection...");
            Object projectedObject = projectionFactory.createProjection(
                    projectionClass, archivio
            );
            if (iHaveToKrint) 
                krintScriptaService.writeArchivioUpdate(archivio, archivioRif, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_RENDI_FASCICOLO);
            
            log.info("Ritorno la projectionCreata");
            return projectedObject;
        }
        throw new Http500ResponseException("5", "Non ho trovato nessun archivio con l'id passato");
    }
    
    @RequestMapping(value = "copiaDoc", method = RequestMethod.POST)
    @Transactional
    public Object copiaDoc(
            @RequestParam("idDoc") String idDoc,
            @RequestParam("idArchivioDestinazione") String idArchivioDestinazione,
            HttpServletRequest request) throws Http500ResponseException, CloneNotSupportedException, JsonProcessingException, EntityReflectionException, BlackBoxPermissionException, RestControllerEngineException, Http403ResponseException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Archivio finalArchivio = null;
        //procedo a tirare su tutto ciò che mi serve
        Integer idDocInt = Integer.valueOf(idDoc);
        Optional<Doc> d = docRepository.findById(idDocInt);
        //controllo l'effettiva presenza dell'archivio da copiare
        if (d.isPresent()) {
            Doc doc = d.get();
            boolean iHaveToKrint = krintUtils.doIHaveToKrint(request);
            //procedo a tirare su tutto ciò che mi serve sull'archivio destinazione
            Integer idArchivioIntDestinazione = Integer.valueOf(idArchivioDestinazione);
            Optional<Archivio> aDestinazione = archivioRepository.findById(idArchivioIntDestinazione);
            //controllo l'effettiva presenza dell'archivio destinazione
            if (aDestinazione.isPresent()) {
                Archivio archivioDestinazione = aDestinazione.get();
                //procedo con le modifiche
                List<ArchivioDoc> archivioDestinazioneDocs = archivioDocRepository.findByIdArchivio(archivioDestinazione);
                for(ArchivioDoc ad: archivioDestinazioneDocs){
                    if(ad.getIdDoc().getId().equals(doc.getId())){
                        throw new Http500ResponseException("2", "L'archivio di destinazione contiene già il doc da copiare");
                    }
                }
                ArchivioDoc archivioDocCopiato = new ArchivioDoc(archivioDestinazione, doc, persona);
                ArchivioDoc save = archivioDocRepository.save(archivioDocCopiato);
                
                log.info(String.format("Il documento è stato copiato correttamente all'archivio: " + archivioDestinazione.getId()));
                finalArchivio = archivioDestinazione;
                
                em.refresh(finalArchivio);
                if (iHaveToKrint) {             
                    krintScriptaService.writeActionDoc(doc, finalArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_DOC_COPIA);
                }
            }
            String projection = "CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo";
            log.info("Recupero projection by name " + projection);
            Class<?> projectionClass = restControllerEngine.getProjectionClass(projection, archivioRepository);
            projectionsInterceptorLauncher.setRequestParams(null, request);
            log.info("Chiamo la facrtory della projection...");
            Object projectedObject = projectionFactory.createProjection(
                    projectionClass, finalArchivio
            );
            log.info("Ritorno la projectionCreata");
            return projectedObject;
        }
        throw new Http500ResponseException("1", "Non ho trovato nessun doc con l'id passato");
    }
    
    @RequestMapping(value = "spostaDoc", method = RequestMethod.POST)
    @Transactional
    public Object spostaDoc(
            @RequestParam("idDoc") String idDoc,
            @RequestParam("idArchivioPartenza") String idArchivioPartenza,
            @RequestParam("idArchivioDestinazione") String idArchivioDestinazione,
            HttpServletRequest request) throws Http500ResponseException, RestControllerEngineException, BlackBoxPermissionException, Http403ResponseException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        //finalArchivio è l'archivio che verrà usato per crare la projection da restituire al front end
        Archivio finalArchivio = null;
        //procedo a tirare su tutto ciò che mi serve sul doc da spostare
        Integer idDocInt = Integer.valueOf(idDoc);
        Optional<Doc> d = docRepository.findById(idDocInt);
        //controllo l'effettiva presenza del doc da spostare
        if (d.isPresent()) {
            Doc doc = d.get();
            //procedo a tirare su tutto ciò che mi serve sull'archivio partenza
            Integer idArchivioIntPartenza = Integer.valueOf(idArchivioPartenza);
            Optional<Archivio> aPartenza = archivioRepository.findById(idArchivioIntPartenza);
            if (aPartenza.isPresent()) {
                Archivio archivioPartenza = aPartenza.get();
                //procedo a tirare su tutto ciò che mi serve sull'archivio destinazione
                Integer idArchivioIntDestinazione = Integer.valueOf(idArchivioDestinazione);
                Optional<Archivio> aDestinazione = archivioRepository.findById(idArchivioIntDestinazione);
                //controllo l'effettiva presenza dell'archivio destinazione
                if (aDestinazione.isPresent()) {
                    Archivio archivioDestinazione = aDestinazione.get();
                    JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
                    boolean iHaveToKrint = krintUtils.doIHaveToKrint(request);
                        log.info(String.format("procedo a spostare il documento %s", doc.getId()));
                        List<ArchivioDoc> archivioDestinazioneDocs = archivioDocRepository.findByIdArchivio(archivioDestinazione);
                        for(ArchivioDoc ad: archivioDestinazioneDocs){
                            if(ad.getIdDoc().getId().equals(doc.getId())){
                                throw new Http500ResponseException("2", "L'archivio di destinazione contiene già il doc da spostare");
                            }
                        }    
                        jPAQueryFactory
                                .update(QArchivioDoc.archivioDoc)
                                .set(QArchivioDoc.archivioDoc.idArchivio, archivioDestinazione)
                                .set(QArchivioDoc.archivioDoc.dataInserimentoRiga, ZonedDateTime.now())
                                .where(QArchivioDoc.archivioDoc.idArchivio.id.eq(archivioPartenza.getId())
                                        .and(QArchivioDoc.archivioDoc.idDoc.id.eq(doc.getId())))
                                .execute();
                        finalArchivio = archivioDestinazione;
                        log.info(String.format("il documento %s è stato spostato con successo", doc.getId()));

                    if (iHaveToKrint) { 
                        krintScriptaService.writeActionDoc(doc, archivioDestinazione, OperazioneKrint.CodiceOperazione.SCRIPTA_DOC_SPOSTA); 
                    }
                }
            }
            String projection = "CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo";
            // Ritorno la projection coi dati aggiornati
            log.info("Recupero projection by name " + projection);
            Class<?> projectionClass = restControllerEngine.getProjectionClass(projection, archivioRepository);
            projectionsInterceptorLauncher.setRequestParams(null, request);
            log.info("Chiamo la facrtory della projection...");
            Object projectedObject = projectionFactory.createProjection(
                    projectionClass, finalArchivio
            );

            log.info("Ritorno la projectionCreata");
            return projectedObject;
        }
        throw new Http500ResponseException("5", "Non ho trovato nessun archivio con l'id passato");
    }

}
