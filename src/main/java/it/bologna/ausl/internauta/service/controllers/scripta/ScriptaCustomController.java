/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.controllers.scripta;

import it.bologna.ausl.documentgenerator.GeneratePE;
import it.bologna.ausl.documentgenerator.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.service.utils.ProjectionBeans;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.StrutturaCustom;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.projections.generated.AllegatoWithPlainFields;
import java.io.FileInputStream;
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
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Top
 */
@RestController
@RequestMapping(value = "${scripta.mapping.url.root}")
public class ScriptaCustomController {

    MinIOWrapperFileInfo savedFileOnRepository = null;
    List<MinIOWrapperFileInfo> savedFilesOnRepository = new ArrayList();
    List<Allegato> savedFilesOnInternauta = new ArrayList();

    @Autowired
    DocRepository docRepository;

    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    AllegatoRepository allegatoRepository;

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    ProjectionBeans projectionBeans;

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    GeneratePE generatePE;

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

            for (MultipartFile file : files) {
                Integer numeroOrdine = null;
                List<Allegato> allegati = doc.getAllegati();
                if (allegati == null || allegati.isEmpty()) {
                    numeroOrdine = 0;
                } else {
                    numeroOrdine = doc.getAllegati().size() + 1;
                }
                DateTimeFormatter data = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSSSSS Z");
                String format = ZonedDateTime.now().format(data);

                savedFileOnRepository = minIOWrapper.put(file.getInputStream(), doc.getIdAzienda().getCodice(), numeroProposta, file.getOriginalFilename(), null, true);
                Allegato allegato = new Allegato();
                allegato.setConvertibilePdf(false);
                allegato.setEstensione(FilenameUtils.getExtension(file.getOriginalFilename()));
                allegato.setNome(FilenameUtils.getBaseName(file.getOriginalFilename()));
                allegato.setIdDoc(doc);
                allegato.setPrincipale(false);
                allegato.setTipo(Allegato.TipoAllegato.ALLEGATO);
                allegato.setDataInserimento(ZonedDateTime.now());
                allegato.setNumeroAllegato(numeroOrdine);
                allegato.setDimensioneByte(Math.toIntExact(file.getSize()));
                allegato.setIdRepository(savedFileOnRepository.getFileId());
                allegato.setMimeType(file.getContentType());

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
        parametersMap.put("applicazione_chiamante", authenticatedUserProperties.getApplicazione());
        //da cambiare quando ci saranno i campi sul db
//        parametersMap.put("numero_documento_origine", 3);
//        parametersMap.put("anno_documento_origine", 2021);
//        parametersMap.put("data_registrazione_origine", "2021-02-25");
//        parametersMap.put("fascicolo_origine", "fascicolo_origine_1");

        parametersMap.put("oggetto", doc.getOggetto());
        //da decommentare quando ci saranno i campi sul db e bisogna mettere la data in stringa
        //parametersMap.put("data_arrivo_origine", doc.getMittenti().get(0).getSpedizioneList().get(0).getData());
        //da elimare quando ci saranno i campi sul db
        parametersMap.put("data_arrivo_origine", "2021-02-25");
        parametersMap.put("utente_protocollante", loggedUser.getId());
        //da mettere quando avremo le fascicolazioni
        //da decommentare quando avremo le tabelle della fascicolazione
        //parametersMap.put("fascicoli_babel", "fascicolo_origine_1");
        parametersMap.put("riservato", "no");
        parametersMap.put("visibilita_limitata", "no");
        parametersMap.put("mittente", buildMittente(projectionBeans.filterRelated(doc.getRelated(), "MITTENTE").get(0)));
        parametersMap.put("destinatari", buildDestinarari(Stream.of(projectionBeans.filterRelated(doc.getRelated(), "A"),
                projectionBeans.filterRelated(doc.getRelated(), "CC"))
                .flatMap(x -> x.stream())
                .collect(Collectors.toList())));

        List<Allegato> allegati = doc.getAllegati();

        MultipartFile multipartPrincipale = null;
        List<MultipartFile> multipartList = new ArrayList();

        if (allegati != null && !allegati.isEmpty()) {
            for (Allegato allegato : allegati) {
                //TODO:prendo il primo o l'ultimo e lo setto come principale

                if (allegato.getPrincipale()) {
                    InputStream allegatoPrincipaleIS = minIOWrapper.getByFileId(allegato.getIdRepository());
                    multipartPrincipale = new MockMultipartFile(allegato.getNome(), allegato.getNome(), allegato.getMimeType(), allegatoPrincipaleIS);
                } else {
                    InputStream allegatoIS = minIOWrapper.getByFileId(allegato.getIdRepository());
                    MultipartFile multipart = new MockMultipartFile(allegato.getNome(), allegato.getNome(), allegato.getMimeType(), allegatoIS);
                    multipartList.add(multipart);
                }
            }
        } else {
            // TODO: dai errore
        }
        String parametersMapString = parametersMap.keySet().stream()
                .map(key -> key + "=" + parametersMap.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        generatePE.init(
                loggedUser.getIdPersona().getCodiceFiscale(),
                parametersMapString,
                multipartPrincipale,
                Optional.of(multipartList),
                aziendeConnectionManager.getAziendeParametriJson(),
                aziendeConnectionManager.getMinIOConfig());
        String record = generatePE.create(null);

        return record;
    }

    @Transactional(rollbackFor = Throwable.class)
    private Allegato saveFileOnInternauta(Allegato allegato) {
        Allegato saved = allegatoRepository.save(allegato);
        return saved;

    }

    private Map<String, Object> buildMittente(Related mittenteDoc) {
        Map<String, Object> mittente = new HashMap();
        mittente.put("descrizione", mittenteDoc.getDescrizione());

        mittente.put("indirizzo_spedizione", mittenteDoc.getSpedizioneList().get(0).getIndirizzo());
        mittente.put("mezzo_spedizione", mittenteDoc.getSpedizioneList().get(0).getIdMezzo());
        return mittente;
    }

    private List<Map<String, Object>> buildDestinarari(List<Related> destinarariDoc) {
        List<Map<String, Object>> destinarari = new ArrayList();
        Map<String, Object> AoCC = new HashMap();

        for (Related destinatario : destinarariDoc) {
            AoCC.put("tipo", destinatario.getTipo());
            //mettere gli assegnatari quando si avranno sul db 
            //dal contatto devo beccare la struttura
            //recuperare cf della persona dal contatto
            //da sistemare quando si potranno mettere in interfaccia
            //AoCC.put("assegnatari", destinatario.getIdContatto().getDettaglioContattoList().get(0).getIdContattoEsterno());
            //probabilmente da modificare con la spedizione
            AoCC.put("struttura", destinatario.getIdContatto().getIdEsterno());

            //da settare quando si avra il reponsabile del procedimento
            //AoCC.put("utente_responsabile", destinatario.getIdContatto().getIdEsterno());
            destinarari.add(AoCC);
        }
        return destinarari;
    }
}
