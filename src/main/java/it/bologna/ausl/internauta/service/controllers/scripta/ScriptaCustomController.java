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
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
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
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageDocRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.MessageDoc;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.PersonaVedente;
import it.bologna.ausl.model.entities.scripta.QPersonaVedente;
import it.bologna.ausl.model.entities.scripta.projections.generated.AllegatoWithIdAllegatoPadre;
import it.bologna.ausl.model.entities.shpeck.MessageInterface;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataArchiviation;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataTagComponent;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;

/**
 *
 * @author Mido
 */
@RestController
@RequestMapping(value = "${scripta.mapping.url.root}")
public class ScriptaCustomController {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptaCustomController.class);

//    private MinIOWrapperFileInfo savedFileOnRepository = null;
    private final List<MinIOWrapperFileInfo> savedFilesOnRepository = new ArrayList();
//    private List<Allegato> savedFilesOnInternauta = new ArrayList();

    @Autowired
    private CachedEntities cachedEntities;

    @Autowired
    private ArchivioRepository archivioRepository;
    
    @Autowired
    private ArchivioDocRepository archivioDocRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private MessageDocRepository messageDocRepository;
    
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
    private DocDetailRepository docDetailRepository;

    @Autowired
    private RegistroDocRepository registroDocRepository;
    
    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private PecRepository pecRepository;
    
    @Autowired
    private PersonaVedenteRepository personaVedenteRepository;

    @Autowired
    private ScriptaUtils scriptaUtils;

    @Autowired
    private ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private AllegatoRepository allegatoRepository;

    @Autowired
    private ProjectionFactory projectionFactory;

    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    private ShpeckUtils shpeckUtils;

    @Autowired
    private GeneratePE generatePE;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    @Autowired
    private RestControllerEngineImpl restControllerEngine;
    
    @Autowired
    ScriptaDownloadUtils scriptaDownloadUtils;
    
    @Value("${babelsuite.webapi.eliminapropostadaedi.url}")
    private String EliminaPropostaDaEdiUrl;

    @Value("${babelsuite.webapi.eliminapropostadaedi.method}")
    private String eliminaPropostaDaEdiMethod;

    private static final Logger log = LoggerFactory.getLogger(ScriptaCustomController.class);

    /**
     * Controller chiamato dal PEIS per salvare una lista di allegati
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
     * @throws it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException
     * @throws it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException
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
                        try (InputStream fileConvertito = scriptaDownloadUtils.downloadOriginalAndConvertToPdf(allegato, null)) {
                            response.setHeader("Content-Type", "application/pdf");
                            StreamUtils.copy(fileConvertito, response.getOutputStream());
                        }
                    } else {
                        throw new Http404ResponseException("4", "Dettaglio allegato richiesto non tovato. Sembra non essere mai esistito");
                    }
                } else {
                    String idRepository = dettaglioAllegato.getIdRepository();
                    MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
                    try (InputStream fileRichiesto = minIOWrapper.getByFileId(idRepository)) {
                        if (fileRichiesto == null) {
                            switch (tipoDettaglioAllegato) {
                                case CONVERTITO:
                                    // File convertito ma scaduto, lo converto e lo scarico
                                    try (InputStream fileConvertito = scriptaDownloadUtils.downloadOriginalAndConvertToPdf(allegato, idRepository)) {
                                        response.setHeader("Content-Type", "application/pdf");
                                        StreamUtils.copy(fileConvertito, response.getOutputStream());
                                    }
                                    break;
                                case ORIGINALE:
                                    // File scaduto, lo riestraggo e lo scarico
                                    try (InputStream fileOrginale = scriptaDownloadUtils.downloadOriginalAttachment(allegato)) {
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

    // SE arrivi qui e vedi che è passato il1 15 giugno 2022 cancella sto metodo commentato
//    @RequestMapping(value = "getResponsabili", method = RequestMethod.GET)
//    public JSONObject getResponsabili(@RequestParam("id") String idArchivio) throws Http500ResponseException {
//        JSONObject json = new JSONObject();
//        JSONArray jsonArray = new JSONArray();
//        Archivio archivio = archivioRepository.getById(Integer.parseInt(idArchivio));
//        ArchivioDetail dettaglio = archivioDetailRepository.getById(Integer.parseInt(idArchivio));
//        Persona personaResponsabile = dettaglio.getIdPersonaResponsabile();
//        Integer[] idVicari = dettaglio.getIdVicari();
//        List<Persona> listVicari = new ArrayList();
//        for (Integer id : idVicari) {
//            Optional<Persona> p = personaRepository.findById(id);
//            listVicari.add(p.get());
//        }
//        json.put("descrizione", personaResponsabile.getDescrizione());
//        json.put("ruolo", "Responsabile");
//        json.put("id", personaResponsabile.getId());
//        json.put("struttura", dettaglio.getIdStruttura().getNome());
//        jsonArray.add(json);
//        for (Persona vic : listVicari) {
//            json = new JSONObject();
//            json.put("descrizione", vic.getDescrizione());
//            json.put("ruolo", "Vicario");
//            json.put("id", vic.getId());
//            json.put("struttura", dettaglio.getIdStruttura().getNome());
//            jsonArray.add(json);
//        }
//        JSONObject jsonReturn = new JSONObject();
//        jsonReturn.put("responsabili", jsonArray);
//        return jsonReturn;
//    }

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
        AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
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
        try (Response response = call.execute();) {
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
        log.info("numeraArchivio" + idArchivio);

        // Numero l'archivio
        //Archivio archivioToSave = archivioRepository.getById(idArchivio);
        log.info("Numero archivio...");
        Integer numeroGenerato = archivioRepository.numeraArchivio(idArchivio);
        log.info("Numerato " + numeroGenerato);
        //DA QUESTO MOMENTO I DATI DI NUMERO, ANNO, NUMERAZIONE GER. SONO GIA' SALVATI SUL DB
        // Ricarico i dati
        log.info("Reload...");
        Archivio archivioToSave = archivioRepository.getById(idArchivio);
        log.info("Numero: " + archivioToSave.getNumero());
        log.info("Anno: " + archivioToSave.getAnno());
        log.info("Numerazione Gerarchica: " + archivioToSave.getNumerazioneGerarchica());

        // Ritorno la projection coi dati aggiornati
        log.info("Recupero projection by name " + projection);
        Class<?> projectionClass = restControllerEngine.getProjectionClass(projection, archivioRepository);
        projectionsInterceptorLauncher.setRequestParams(null, request);
        log.info("Chiamo la facrtory della projection...");
        Object projectedObject = projectionFactory.createProjection(
                projectionClass, archivioToSave
        );

        log.info("Ritorno la projectionCreata");
        return projectedObject;
    }

   
    /**
     * Dato un archivio chiama la store procedure che calcola i permessi espliciti dello stesso
     * @param idArchivioRadice
     * @param request
     * @return 
     */
    @RequestMapping(value = "calcolaPermessiEspliciti", method = RequestMethod.POST)
    public ResponseEntity<?> calcolaPermessiEspliciti(
            @RequestParam("idArchivioRadice") Integer idArchivioRadice,
            HttpServletRequest request) {
        
        archivioRepository.calcolaPermessiEspliciti(idArchivioRadice);
        
        return new ResponseEntity("", HttpStatus.OK);
    }
    
    /**
     * Dato l'idEsterno di un Doc, la funzione torna una lista contentente gli idPersona di tutti coloro che hanno un permesso
     * con bit >= di minBit negli archivi in cui il doc è archiviato
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
    ) throws IOException, FileNotFoundException, NoSuchAlgorithmException, Throwable {
        
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
                Doc doc = new  Doc(file.getOriginalFilename(), authenticatedUserProperties.getPerson(), archivio.getIdAzienda(), DocDetailInterface.TipologiaDoc.DOCUMENT_UTENTE.toString());
                doc = docRepository.save(doc);
                idDocList.add(doc.getId());
                scriptaUtils.creaAndAllegaAllegati(doc, file.getInputStream(), file.getOriginalFilename(), true);

                //archvivio il document
                ArchivioDoc archiviazione = new ArchivioDoc(archivio, doc, persona);
                ArchivioDoc save = archivioDocRepository.save(archiviazione);
                archivioDiInteresseRepository.aggiungiArchivioRecente(archivio.getIdArchivioRadice().getId(), persona.getId());
                
                personaVedenteRepository.aggiungiPersoneVedentiSuDocDaPermessiArchivi(doc.getId());
            }
        } catch (Exception e) {
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
    

    @RequestMapping(value = "archiveMessage/{idMessage}/{idArchivio}", method = RequestMethod.POST)
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<?> archiveMessage(
            HttpServletRequest request,
            @PathVariable(required = true) Integer idMessage,
            @PathVariable(required = true) Integer idArchivio
    ) throws IOException, FileNotFoundException, NoSuchAlgorithmException, Throwable {
        projectionsInterceptorLauncher.setRequestParams(null, request); // Necessario per poter poi creare una projection
        
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Utente utente = authenticatedUserProperties.getUser();
        Archivio archivio = archivioRepository.findById(idArchivio).get();
        Message message = messageRepository.findById(idMessage).get();
        Azienda azienda = archivio.getIdAzienda();
        
        // Controlli di sicurezza
        // 1
        // Se il messaggio non è archiviabile perché non ha ancora l'idRepository
        // Nel frontend l'icona è disattiva quindi qui non dovrei mai entrare.
        if (message.getUuidRepository() == null) {
            throw new Http404ResponseException("1", "Messaggio senza idRepository");
        }
        
        // 2
        // Controllo che l'utente abbia permessi nella casella pec del message
        Map<Integer, List<String>> permessiPec = userInfoService.getPermessiPec(persona);
        if (!permessiPec.isEmpty()) {
            List<Integer> pecList = new ArrayList();
            pecList.addAll(permessiPec.keySet());
            if (!pecList.contains(message.getIdPec().getId())) {
                throw new Http403ResponseException("2", "Utente senza permessi sulla casella pec");
            }
        } else {
            throw new Http403ResponseException("2", "Utente senza permessi sulla casella pec");
        }
        
        // 3
        // Controllo che l'utente abbia almeno permesso di modifica sull'archivio
        if (!scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), archivio.getId(), PermessoArchivio.DecimalePredicato.MODIFICA)) {
            throw new Http403ResponseException("3", "Utente senza permesso di modificare l'archivio");
        }
        
        
        /*
        Ora vedo se il doc già esiste ( lo becco dentro messages_docs. )
        Se non esiste allora dovrò crearlo e quindi dovrò creare i vari allegati.
         */
        Doc doc = null;
        List<MessageDoc> messageDocList = message.getMessageDocList().stream().filter(md -> md.getScope().equals(MessageDoc.ScopeMessageDoc.ARCHIVIAZIONE)).collect(Collectors.toList());
        for (MessageDoc md : messageDocList) {
            if (md.getIdDoc().getIdAzienda().getId().equals(azienda.getId())) {
                doc = md.getIdDoc();
                break;
            }
        }
       
        if (doc == null) {
            File downloadEml = shpeckUtils.downloadEml(ShpeckUtils.EmlSource.MESSAGE, idMessage);
            MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
            
            doc = new  Doc("Pec_" + message.getId().toString(), persona, archivio.getIdAzienda(), DocDetailInterface.TipologiaDoc.DOCUMENT_PEC.toString());
            doc = docRepository.save(doc);
            MessageDoc.TipoMessageDoc tipo = null;
            if (message.getInOut().equals(MessageInterface.InOut.IN)) {
                tipo = MessageDoc.TipoMessageDoc.IN;
            } else {
                tipo = MessageDoc.TipoMessageDoc.OUT;
            }
            MessageDoc md = new MessageDoc(message, doc, tipo, MessageDoc.ScopeMessageDoc.ARCHIVIAZIONE);
            messageDocRepository.save(md);
            
            try {
                scriptaUtils.creaAndAllegaAllegati(doc, new FileInputStream(downloadEml), "Pec_" + message.getId().toString(), true, true, message.getUuidRepository(), false, null); // downloadEml.getName()
            } catch (Exception e) {
                if (savedFilesOnRepository != null && !savedFilesOnRepository.isEmpty()) {
                    for (MinIOWrapperFileInfo minIOWrapperFileInfo : savedFilesOnRepository) {
                        minIOWrapper.removeByFileId(minIOWrapperFileInfo.getFileId(), false);
                    }
                }
                e.printStackTrace();
                throw new Http500ResponseException("4", "Qualcosa è andato storto nelle creazione degli allegati");
            }
        }
        
        // Ora che o il doc lo archivio
        ArchivioDoc archiviazione = new ArchivioDoc(archivio, doc, persona);
        archivioDocRepository.save(archiviazione);
        archivioDiInteresseRepository.aggiungiArchivioRecente(archivio.getIdArchivioRadice().getId(), persona.getId());
        
        // Ora aggiungo il tag di archiviazione sul message
        AdditionalDataTagComponent.idUtente utenteAdditionalData = new AdditionalDataTagComponent.idUtente(utente.getId(), persona.getDescrizione());
        AdditionalDataTagComponent.idAzienda aziendaAdditionalData = new AdditionalDataTagComponent.idAzienda(azienda.getId(), azienda.getNome(), azienda.getDescrizione());
        AdditionalDataTagComponent.idArchivio archivioAdditionalData = new AdditionalDataTagComponent.idArchivio(archivio.getId(), archivio.getOggetto(), archivio.getNumerazioneGerarchica());
        AdditionalDataArchiviation additionalDataArchiviation = new AdditionalDataArchiviation(utenteAdditionalData, aziendaAdditionalData, archivioAdditionalData, LocalDateTime.now());
        shpeckUtils.SetArchiviationTag(message.getIdPec(), message, additionalDataArchiviation, utente, true, true);
        
        personaVedenteRepository.aggiungiPersoneVedentiSuDocDaPermessiArchivi(doc.getId());
        
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
            @RequestParam("idArchivioRadice") Integer idArchivioRadice,
            HttpServletRequest request) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        archivioDiInteresseRepository.aggiungiArchivioRecente(idArchivioRadice, persona.getId());
        return new ResponseEntity("", HttpStatus.OK);
    }
}
