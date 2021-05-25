package it.bologna.ausl.internauta.service.controllers.scripta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.documentgenerator.GeneratePE;
import it.bologna.ausl.documentgenerator.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.internauta.service.utils.ProjectionBeans;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Related;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.apache.commons.io.FilenameUtils;
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
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DettaglioAllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.RegistroDocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.RegistroRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.ScriptaUtils;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.scripta.DettaglioAllegato;
import it.bologna.ausl.model.entities.scripta.DettaglioAllegato.TipoDettaglioAllegato;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.AllegatoWithDettagliAllegatiListAndIdAllegatoPadre;
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
import org.json.JSONObject;
import org.springframework.util.StringUtils;

/**
 *
 * @author Mido
 */
@RestController
@RequestMapping(value = "${scripta.mapping.url.root}")
public class ScriptaCustomController {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptaCustomController.class);

    MinIOWrapperFileInfo savedFileOnRepository = null;
    List<MinIOWrapperFileInfo> savedFilesOnRepository = new ArrayList();
    List<Allegato> savedFilesOnInternauta = new ArrayList();

    @Autowired
    CachedEntities cachedEntities;
    
    @Autowired
    DocRepository docRepository;
    
    @Autowired
    RegistroDocRepository registroDocRepository;

    @Autowired
    PecRepository pecRepository;

    @Autowired
    ScriptaUtils scriptaUtils;

    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    ParametriAziende parametriAziende;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    AllegatoRepository allegatoRepository;

    @Autowired
    DettaglioAllegatoRepository dettaglioAllegatoRepository;

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    ProjectionBeans projectionBeans;

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    GeneratePE generatePE;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    @RequestMapping(value = "saveAllegato", method = RequestMethod.POST)
    public ResponseEntity<?> saveAllegato(
            HttpServletRequest request,
            @RequestParam("idDoc") Integer idDoc,
            @RequestParam("numeroProposta") String numeroProposta,
            @RequestParam("files") List<MultipartFile> files) throws MinIOWrapperException {
        projectionsInterceptorLauncher.setRequestParams(null, request);
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        Iterable<Allegato> tuttiAllegati = null;
        try {
            Optional<Doc> optionalDoc = docRepository.findById(idDoc);
            Doc doc = null;
            if (!optionalDoc.isPresent()) {
                throw new Http500ResponseException("1", "documento non trovato");
            } else {
                doc = optionalDoc.get();
            }

            List<Allegato> allegati = doc.getAllegati();
            Integer numeroOrdine = null;
            if (allegati == null || allegati.isEmpty()) {
                numeroOrdine = 0;
            } else {
                numeroOrdine = doc.getAllegati().size();
            }

            for (MultipartFile file : files) {
                numeroOrdine++;
                DateTimeFormatter data = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSSSSS Z");
                String format = ZonedDateTime.now().format(data);

                savedFileOnRepository = minIOWrapper.put(file.getInputStream(), doc.getIdAzienda().getCodice(), numeroProposta, file.getOriginalFilename(), null, true);
                Allegato allegato = new Allegato();
                allegato.setNome(FilenameUtils.getBaseName(file.getOriginalFilename()));
                allegato.setIdDoc(doc);
                allegato.setPrincipale(false);
                allegato.setTipo(Allegato.TipoAllegato.ALLEGATO);
                allegato.setDataInserimento(ZonedDateTime.now());
                allegato.setOrdinale(numeroOrdine);
                allegato.setFirmato(false);
                DettaglioAllegato dettaglioAllegato = new DettaglioAllegato();
                //allegato.setConvertibilePdf(false);
                dettaglioAllegato.setHashMd5(savedFileOnRepository.getMd5());

                dettaglioAllegato.setHashSha256(getHashFromFile(file.getInputStream(), "SHA-256"));
                dettaglioAllegato.setNome(FilenameUtils.getBaseName(file.getOriginalFilename()));
                dettaglioAllegato.setIdAllegato(allegato);
                dettaglioAllegato.setEstensione(FilenameUtils.getExtension(file.getOriginalFilename()));
                dettaglioAllegato.setDimensioneByte(Math.toIntExact(file.getSize()));
                dettaglioAllegato.setIdRepository(savedFileOnRepository.getFileId());
                dettaglioAllegato.setCaratteristica(TipoDettaglioAllegato.ORIGINALE);
                dettaglioAllegato.setMimeType(file.getContentType());
                List<DettaglioAllegato> dettagliAllegatiList = new ArrayList();
                dettagliAllegatiList.add(dettaglioAllegato);
                savedFilesOnRepository.add(savedFileOnRepository);
                allegato.setDettagliAllegatiList(dettagliAllegatiList);
                savedFilesOnInternauta.add(saveFileOnInternauta(allegato));
            }
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
            return ResponseEntity.ok(stream.map(a -> projectionFactory.createProjection(AllegatoWithDettagliAllegatiListAndIdAllegatoPadre.class, a)).collect(Collectors.toList()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Effettua l'upload sul client dello stream del file richiesto
     *
     * @param idAllegato
     * @param response
     * @param request
     * @throws IOException
     * @throws MinIOWrapperException
     *
     */
    @RequestMapping(value = "dettaglioallegato/{idDettaglioAllegato}/download", method = RequestMethod.GET)
    public void downloadAttachment(
            @PathVariable(required = true) Integer idDettaglioAllegato,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException, MinIOWrapperException {
        LOG.info("downloadAllegato", idDettaglioAllegato);
        //TODO si deve instanziare il rest controller engine e poi devi prendere il dettaglio (aggiungere interceptor per vedere se l'utente puo scaricare il file)
        DettaglioAllegato dettaglioAllegato = dettaglioAllegatoRepository.getOne(idDettaglioAllegato);
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        if (dettaglioAllegato != null) {
            StreamUtils.copy(minIOWrapper.getByFileId(dettaglioAllegato.getIdRepository()), response.getOutputStream());
        }
        response.flushBuffer();
    }

    /**
     * Effettua l'upload sul client dello stream dello zip contenente gli
     * allegati del doc richiesto
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
                        zos.putNextEntry(new ZipEntry((String) allegato.getNome() + s + "." + allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getEstensione()));
                        in_error = false;
                    } catch (ZipException ex) {
                        i++;
                    }
                }
                StreamUtils.copy((InputStream) minIOWrapper.getByFileId(allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getIdRepository()), zos);
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
        pecMessageDetail.put("dataArrivo", message.getReceiveTime());
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
        parametersMap.put("numero_documento_origine", doc.getId().toString());
        parametersMap.put("anno_documento_origine", doc.getDataCreazione().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateFormat = doc.getDataCreazione().format(formatter);
        parametersMap.put("data_registrazione_origine", dateFormat);
        parametersMap.put("oggetto", doc.getOggetto());
        //TODO da decommentare quando ci saranno i campi sul db e bisogna mettere la data in stringa
        //parametersMap.put("data_arrivo_origine", doc.getMittenti().get(0).getSpedizioneList().get(0).getData());
        //da elimare quando ci saranno i campi sul db
        parametersMap.put("data_arrivo_origine", dateFormat);
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

    private MultipartFile getMultiPartFromAllegato(Allegato allegato, TipoDettaglioAllegato tipoDettaglioAllegato) throws MinIOWrapperException, IOException {
        MultipartFile multipartDaTornare = null;
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        DettaglioAllegato dettaglioAllegatoRichiesto = allegato.getDettaglioByTipoDettaglioAllegato(tipoDettaglioAllegato);
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

    private MultipartFile manageAndReturnAllegatoPrincipaleMultipart(Doc doc) throws MinIOWrapperException, IOException {
        Allegato allegatoPrincipale = scriptaUtils.getAllegatoPrincipale(doc);
        MultipartFile multipartPrincipale = null;
        if (allegatoPrincipale != null) {
            multipartPrincipale = getMultiPartFromAllegato(allegatoPrincipale, TipoDettaglioAllegato.ORIGINALE);
        }
        return multipartPrincipale;
    }

    private List<MultipartFile> manageAndReturnAllegatiNonPrincipaliMultiPartList(Doc doc) throws MinIOWrapperException, IOException {
        List<MultipartFile> multipartList = new ArrayList();
        List<Allegato> allegati = doc.getAllegati();
        for (Allegato allegato : allegati) {
            if (!allegato.getPrincipale()) {
                //devo prendere gli 'ORIGINALI' NON FIGLI
                MultipartFile multipart = getMultiPartFromAllegato(allegato,
                        TipoDettaglioAllegato.ORIGINALE);
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
        Utente loggedUser = authenticatedUserProperties.getUser();
        Persona loggedPersona = authenticatedUserProperties.getPerson();

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
        
        Map<String, Object> resObj = objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>(){});
        saveRegistriDoc(resObj, doc, loggedPersona);
        
        ResponseEntity res = ResponseEntity.ok(resObj);
        return res;
    }
    
    private void saveRegistriDoc(Map<String, Object> resObj, Doc doc, Persona loggedPersona) throws JsonProcessingException {
        Integer numeroProtocollo = Integer.parseInt((String)resObj.get("numeroProtocollo"));
        Integer annoProtocollo = (Integer)resObj.get("annoProtocollo");
        String numeroPropostaConAnno = (String)resObj.get("numeroProposta");
        Integer numeroProposta = Integer.parseInt(numeroPropostaConAnno.split("-")[1]);
        Integer annoProposta = Integer.parseInt(numeroPropostaConAnno.split("-")[0]);
        Integer idStrutturaProtocollante = (Integer)resObj.get("idStrutturaProtocollante");
        
        Struttura struttura = cachedEntities.getStruttura(idStrutturaProtocollante);
        Registro registroPropostaPico = cachedEntities.getRegistro(doc.getIdAzienda().getId(), Registro.CodiceRegistro.PROP_PG);
        Registro registroProtocolloPico = cachedEntities.getRegistro(doc.getIdAzienda().getId(), Registro.CodiceRegistro.PG);
        
        String dataRegistrazioneString = (String)resObj.get("dataRegistrazione");
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
}
