/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.controllers.scripta;

import it.bologna.ausl.documentgenerator.GeneratePE;
import it.bologna.ausl.documentgenerator.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    ReporitoryConnectionManager reporitoryConnectionManager;

    @Autowired
    AllegatoRepository allegatoRepository;

    @Autowired
    GeneratePE generatePE;

    @RequestMapping(value = "saveAllegato", method = RequestMethod.POST)
    public ResponseEntity<List<Allegato>> saveAllegato(
            HttpServletRequest request,
            @RequestParam("idDoc") Integer idDoc,
            @RequestParam("numeroProposta") String numeroProposta,
            @RequestParam("files") List<MultipartFile> files) throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = reporitoryConnectionManager.getMinIOWrapper();
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

        } catch (Exception e) {
            if (savedFilesOnRepository != null && !savedFilesOnRepository.isEmpty()) {
                for (MinIOWrapperFileInfo minIOWrapperFileInfo : savedFilesOnRepository) {
                    minIOWrapper.removeByFileId(minIOWrapperFileInfo.getFileId(), false);
                }
            }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok(savedFilesOnInternauta);

    }

    @RequestMapping(value = "createPE", method = RequestMethod.POST)
    public String createPE(
            @RequestParam("id_doc") Integer idDoc,
            HttpServletRequest request) throws HttpInternautaResponseException, Throwable {

        Optional<Doc> doc = docRepository.findById(idDoc);

        if (doc.isEmpty()) {
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente loggedUser = (Utente) authentication.getPrincipal();

        // TODO: oggetto dei parametri poi tradotto in string. trasformare i json parametri in mappa
        JSONObject jSONparametri = new JSONObject();

        // TODO: trasformare i file in MultipartFile e passarli
        // vedere: https://stackoverflow.com/questions/54712810/java-spring-best-way-to-convert-a-file-to-a-multipartfile
        generatePE.init(loggedUser.getIdPersona().getCodiceFiscale(), null, null, null);
        String record = generatePE.create();

        return record;
    }

    @Transactional(rollbackFor = Throwable.class)
    private Allegato saveFileOnInternauta(Allegato allegato) {
        Allegato saved = allegatoRepository.save(allegato);
        return saved;

    }
}
