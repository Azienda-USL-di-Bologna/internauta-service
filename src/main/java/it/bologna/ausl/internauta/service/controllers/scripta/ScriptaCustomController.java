package it.bologna.ausl.internauta.service.controllers.scripta;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.documentgenerator.GeneratePE;
import it.bologna.ausl.documentgenerator.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.internauta.service.utils.ProjectionBeans;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.projections.generated.AllegatoWithPlainFields;
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
import it.bologna.ausl.internauta.service.repositories.scripta.DettaglioAllegatoRepository;
import it.bologna.ausl.model.entities.scripta.DettaglioAllegato;
import it.bologna.ausl.model.entities.scripta.DettaglioAllegato.TipoDettaglioAllegato;
import it.bologna.ausl.model.entities.scripta.Mezzo;

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
    DocRepository docRepository;

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

    @RequestMapping(value = "saveAllegato", method = RequestMethod.POST)
    public ResponseEntity<?> saveAllegato(
            HttpServletRequest request,
            @RequestParam("idDoc") Integer idDoc,
            @RequestParam("numeroProposta") String numeroProposta,
            @RequestParam("files") List<MultipartFile> files) throws MinIOWrapperException {
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

                DettaglioAllegato dettaglioAllegato = new DettaglioAllegato();

                //allegato.setConvertibilePdf(false);
                dettaglioAllegato.setEstensione(FilenameUtils.getExtension(file.getOriginalFilename()));
                dettaglioAllegato.setDimensioneByte(Math.toIntExact(file.getSize()));
                dettaglioAllegato.setIdRepository(savedFileOnRepository.getFileId());
                dettaglioAllegato.setMimeType(file.getContentType());

                savedFilesOnRepository.add(savedFileOnRepository);
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
            return ResponseEntity.ok(stream.map(a -> projectionFactory.createProjection(AllegatoWithPlainFields.class, a)).collect(Collectors.toList()));
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

    @RequestMapping(value = "createPE", method = RequestMethod.POST)
    public String createPE(
            @RequestParam("id_doc") Integer idDoc,
            HttpServletRequest request) throws HttpInternautaResponseException, Throwable {
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        Optional<Doc> docOp = docRepository.findById(idDoc);
        Doc doc;
        if (docOp.isPresent()) {
            doc = docOp.get();
        } else {
            return null;
        }
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente loggedUser = authenticatedUserProperties.getUser();

        // TODO: oggetto dei parametri poi tradotto in string. trasformare i json parametri in mappa
        Map<String, Object> parametersMap = new HashMap();
        parametersMap.put("azienda", doc.getIdAzienda().getCodiceRegione() + doc.getIdAzienda().getCodice());
        parametersMap.put("applicazione_chiamante", authenticatedUserProperties.getApplicazione().toString());
        //da cambiare quando ci saranno i campi sul db
        //visto che il sistema interno i campi origine sono i dati del documento
        parametersMap.put("numero_documento_origine", idDoc.toString());
        parametersMap.put("anno_documento_origine", doc.getDataCreazione().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-mm-dd");
        String dateFormat = doc.getDataCreazione().format(formatter);
        parametersMap.put("data_registrazione_origine", dateFormat);
//        parametersMap.put("fascicolo_origine", "scripta_1");

        parametersMap.put("oggetto", doc.getOggetto());
        //da decommentare quando ci saranno i campi sul db e bisogna mettere la data in stringa
        //parametersMap.put("data_arrivo_origine", doc.getMittenti().get(0).getSpedizioneList().get(0).getData());
        //da elimare quando ci saranno i campi sul db
        parametersMap.put("data_arrivo_origine", dateFormat);
        parametersMap.put("utente_protocollante", loggedUser.getIdPersona().getCodiceFiscale());
        //da mettere quando avremo le fascicolazioni
        //da decommentare quando avremo le tabelle della fascicolazione
        //parametersMap.put("fascicoli_babel", "fascicolo_origine_1");
        parametersMap.put("riservato", "no");
        parametersMap.put("visibilita_limitata", "no");
        parametersMap.put("mittente", buildMittente(projectionBeans.filterRelated(doc.getRelated(), "MITTENTE").get(0)));
        List<Related> related = new ArrayList();
        related.addAll(projectionBeans.filterRelated(doc.getRelated(), "A"));
        related.addAll(projectionBeans.filterRelated(doc.getRelated(), "CC"));

        parametersMap.put("destinatari", buildDestinarari(related));

        List<Allegato> allegati = doc.getAllegati();

        MultipartFile multipartPrincipale = null;
        List<MultipartFile> multipartList = new ArrayList();

        if (allegati != null && !allegati.isEmpty()) {
            for (Allegato allegato : allegati) {
                //TODO:prendo il primo o l'ultimo e lo setto come principale

                if (allegato.getPrincipale()) {

                    //devo prendere gli 'ORIGINALI' non figli
                    InputStream allegatoPrincipaleIS = minIOWrapper.getByFileId(allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getIdRepository());
                    multipartPrincipale = new MockMultipartFile(allegato.getNome() + "." + allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getEstensione(), allegato.getNome() + "." + allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getEstensione(), allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getMimeType(), allegatoPrincipaleIS);
                } else {
                    //devo prendere gli 'ORIGINALI' NON FIGLI
                    InputStream allegatoIS = minIOWrapper.getByFileId(allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getIdRepository());
                    MultipartFile multipart = new MockMultipartFile(allegato.getNome() + "." + allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getEstensione(), allegato.getNome() + "." + allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getEstensione(), allegato.getDettaglioByTipoDettaglioAllegato(TipoDettaglioAllegato.ORIGINALE).getMimeType(), allegatoIS);
                    multipartList.add(multipart);
                }
            }
        } else {
            // TODO: dai errore
        }
        Boolean minIOActive = false;
        List<ParametroAziende> mongoAndMinIOActive = parametriAziende.getParameters("mongoAndMinIOActive");
        if (mongoAndMinIOActive != null && !mongoAndMinIOActive.isEmpty()) {
            minIOActive = parametriAziende.getValue(mongoAndMinIOActive.get(0), Boolean.class);
        }
//        generatePE.init(
//                loggedUser.getIdPersona().getCodiceFiscale(),
//                parametersMap,
//                multipartPrincipale,
//                Optional.of(multipartList),
//                aziendeConnectionManager.getAziendeParametriJson(),
//                minIOActive,
//                aziendeConnectionManager.getMinIOConfig()
//        );

        String record = generatePE.create(null);

        return record;
    }

    @Transactional(rollbackFor = Throwable.class)
    private Allegato saveFileOnInternauta(Allegato allegato) {
        Allegato saved = allegatoRepository.save(allegato);
        return saved;

    }

    private Map<String, Object> buildMittente(Related mittenteDoc) throws JsonProcessingException {
        Map<String, Object> mittente = new HashMap();
        mittente.put("descrizione", mittenteDoc.getDescrizione());

        mittente.put("indirizzo_spedizione", mittenteDoc.getSpedizioneList().get(0).getIndirizzo().toString());
        Mezzo idMezzo = mittenteDoc.getSpedizioneList().get(0).getIdMezzo();
        mittente.put("mezzo_spedizione", mittenteDoc.getSpedizioneList().get(0).getIdMezzo().ottieniCodiceArgo());
        return mittente;
    }

    private List<Map<String, Object>> buildDestinarari(List<Related> destinarariDoc) {
        List<Map<String, Object>> destinarari = new ArrayList();

        for (Related destinatario : destinarariDoc) {
            Map<String, Object> AoCC = new HashMap();
            AoCC.put("tipo", destinatario.getTipo().toString());
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
}
