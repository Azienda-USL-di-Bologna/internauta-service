/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.controllers.scripta;

import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVAnomaliaException;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Top
 */
public class ScriptaCustomController {

    MinIOWrapperFileInfo savedFileOnRepository = null;
    Allegato saveFileOnInternauta = null;

    @Autowired
    MinIOWrapper minIOWrapper;

    @Autowired
    DocRepository docRepository;

    @Autowired
    AllegatoRepository allegatoRepository;

    @RequestMapping(value = "saveAllegato", method = RequestMethod.POST)
    public ResponseEntity<Allegato> saveAllegato(
            HttpServletRequest request,
            @RequestParam("Allegato") Allegato allegato,
            @RequestParam("idAzienda") String idAzienda,
            @RequestParam("codiceAzienda") String codiceAzienda,
            @RequestParam("numeroProposta") String numeroProposta,
            @RequestParam("file") MultipartFile file) throws MinIOWrapperException {
        try {
            Optional<Doc> optionalDoc = docRepository.findById(allegato.getIdDoc().getId());
            Doc doc = null;
            if (!optionalDoc.isPresent()) {
                throw new Http500ResponseException("1", "documento non trovato");
            } else {
                doc = optionalDoc.get();
            }
            String mimeType = file.getContentType();

            savedFileOnRepository = minIOWrapper.put(file.getInputStream(), codiceAzienda, numeroProposta, file.getName(), null, true);
            saveFileOnInternauta = saveFileOnInternauta(savedFileOnRepository, doc, mimeType, allegato);
        } catch (Exception e) {
            minIOWrapper.removeByFileId(savedFileOnRepository.getFileId(), false);
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok(saveFileOnInternauta);

    }

    @Transactional(rollbackFor = Throwable.class)
    private Allegato saveFileOnInternauta(MinIOWrapperFileInfo savedFileOnRepository, Doc doc, String mimeType, Allegato allegato) {
        Integer numeroOrdine = null;
        List<Allegato> allegati = doc.getAllegati();
        if (allegati == null || allegati.isEmpty()) {
            numeroOrdine = 0;
        } else {
            numeroOrdine = doc.getAllegati().size() + 1;
        }
        LocalDateTime data = LocalDateTime.now();
        Allegato allegatoDaSalvare = new Allegato(doc, null, savedFileOnRepository.getFileName(), allegato.getEstensione(), allegato.getTipo(), savedFileOnRepository.getFileId(), false, numeroOrdine, true, savedFileOnRepository.getSize(), mimeType, data);
        Allegato saved = allegatoRepository.save(allegatoDaSalvare);
        return saved;

    }
}
