/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.estrattore.ExtractorCreator;
import it.bologna.ausl.estrattore.ExtractorResult;
import it.bologna.ausl.estrattoremaven.exception.ExtractorException;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DettaglioAllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.DettaglioAllegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.shpeck.Message;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.mime.MimeTypeException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Salo
 */
@Component
public class ScriptaUtils {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    AllegatoRepository allegatoRepository;

    @Autowired
    DettaglioAllegatoRepository dettaglioAllegatoRepository;

    @Autowired
    private PecRepository pecRepository;

    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

    public Doc protocollaMessaggio(Doc doc, Message message) throws MinIOWrapperException, IOException, FileNotFoundException, NoSuchAlgorithmException, Throwable {
        List<Allegato> allegatiCreatiDalMessaggio = creaAndAllegaAllegatiByMessaggio(doc, message);
        doc.setAllegati(allegatiCreatiDalMessaggio);
        // qui ci va il codice per aggiungere DocMessage 
        // (tabella di cross che non esiste ancora)

        return doc;
    }

    public List<Allegato> creaAndAllegaAllegatiByMessaggio(Doc doc, Message message) throws MinIOWrapperException, IOException, FileNotFoundException, NoSuchAlgorithmException, Throwable {
        List<Allegato> allegatiDaTornare = new ArrayList<Allegato>();
        MinIOWrapper minIOWrapper = aziendeConnectionManager.
                getMinIOWrapper();
        InputStream file = minIOWrapper.getByUuid(message.getUuidRepository());
        if (file != null) {
            File folderToSave = FileUtilities.getCartellaTemporanea(
                    "EstrazioneTemp" + doc.getId().toString());
            try {
                allegatiDaTornare = estraiRicorsivamenteSalvaAndFaiUploadAllegatiDaInputStream(
                        file, message.getName(),
                        doc, folderToSave);
                FileUtilities.svuotaCartella(folderToSave.getAbsolutePath());

            } catch (Throwable ex) {
                FileUtilities.svuotaCartella(folderToSave.getAbsolutePath());
                throw ex;
            }

        } else {
            throw new FileNotFoundException("File non trovato sul repository " + message.getUuidRepository());
        }
        return allegatiDaTornare;
    }

    public MinIOWrapperFileInfo putFileOnMongo(File file,
            String codiceAzienda, String path, String fileName,
            Map<String, Object> metadata, boolean overWrite)
            throws MinIOWrapperException, IOException {

        MinIOWrapper minIOWrapper = aziendeConnectionManager.
                getMinIOWrapper();
        MinIOWrapperFileInfo savedFileOnRepositoryFileInfo = minIOWrapper.put(
                file, codiceAzienda,
                path, fileName, null, true);
        return savedFileOnRepositoryFileInfo;
    }

    public List<Allegato> estraiRicorsivamenteSalvaAndFaiUploadAllegatiDaInputStream(
            InputStream allegatoInputStream,
            String nomeDelFile,
            Doc doc,
            File folderToSave) throws ExtractorException, IOException, Throwable {
        List<Allegato> allegatiDaTornare = new ArrayList<Allegato>();
        ArrayList<ExtractorResult> extractionResultAll = FileUtilities.estraiTuttoDalFile(
                folderToSave,
                allegatoInputStream,
                nomeDelFile);
        if (extractionResultAll != null) {
            HashMap<String, Object> mappaHashAllegati = new HashMap();
            int numeroAllegato
                    = doc.getAllegati() != null
                    ? doc.getAllegati().size() : 0;
            for (ExtractorResult er : extractionResultAll) {
                numeroAllegato++;
                try {
                    File file = new File(er.getPath());
                    MinIOWrapperFileInfo putFileOnMongo = putFileOnMongo(file, doc.getIdAzienda().getCodice(), doc.getId().toString(),
                            er.getFileName(), null, true);
                    InputStream fileDaPassare = new FileInputStream(file);

                    Allegato nuovoAllegato = buildNewAllegato(doc, er.getFileName());
                    nuovoAllegato.setOrdinale(numeroAllegato);
                    Integer intSize = new Long(er.getSize()).intValue();
                    nuovoAllegato = allegatoRepository.save(nuovoAllegato);

                    DettaglioAllegato dettaglioAllegato = buildNewDettaglioAllegato(
                            nuovoAllegato, fileDaPassare,
                            nuovoAllegato.getNome(), er.getMimeType(),
                            intSize, putFileOnMongo);

                    dettaglioAllegato = dettaglioAllegatoRepository.save(dettaglioAllegato);
                    List<DettaglioAllegato> dettagliAllegatiList = new ArrayList();
                    dettagliAllegatiList.add(dettaglioAllegato);

                    mappaHashAllegati.put(er.getHash(), nuovoAllegato);
                    if (er.getPadre() != null) {
                        Allegato padre = (Allegato) mappaHashAllegati.get(er.getPadre());
                        nuovoAllegato.setIdAllegatoPadre(padre);
                    }
                    nuovoAllegato = allegatoRepository.save(nuovoAllegato);
                    allegatiDaTornare.add(nuovoAllegato);
                } catch (Throwable e) {
                    FileUtilities.svuotaCartella(folderToSave.getAbsolutePath());
                    throw e;
                }
            }
        }
        return allegatiDaTornare;
    }

    public Allegato buildNewAllegato(Doc doc, String originalFileName) {
        List<Allegato> allegati = doc.getAllegati();
        Integer numeroOrdine = !(allegati == null || allegati.isEmpty())
                ? allegati.size() : 0;
        numeroOrdine++;
        Allegato allegato = new Allegato();
        allegato.setNome(originalFileName);
        allegato.setIdDoc(doc);
        allegato.setPrincipale(false);
        allegato.setTipo(Allegato.TipoAllegato.ALLEGATO);
        allegato.setDataInserimento(ZonedDateTime.now());
        allegato.setOrdinale(numeroOrdine);
        allegato.setFirmato(false);
        return allegato;
    }

    public DettaglioAllegato buildNewDettaglioAllegato(Allegato allegato,
            InputStream fileInputStream,
            String fileNameWithExtension,
            String mimeType,
            Integer size, MinIOWrapperFileInfo minioFileInfo) throws IOException, FileNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException, MimeTypeException {
        DettaglioAllegato dettaglioAllegato = new DettaglioAllegato();
        dettaglioAllegato.setHashMd5(minioFileInfo.getMd5());

        dettaglioAllegato.setHashSha256(FileUtilities.getHashFromFile(
                fileInputStream, "SHA-256"));
        dettaglioAllegato.setNome(FilenameUtils.getBaseName(fileNameWithExtension));
        dettaglioAllegato.setIdAllegato(allegato);
        dettaglioAllegato.setEstensione(FilenameUtils.getExtension(fileNameWithExtension));
        dettaglioAllegato.setDimensioneByte(size);
        dettaglioAllegato.setIdRepository(
                minioFileInfo.getFileId());
        dettaglioAllegato.setCaratteristica(DettaglioAllegato.TipoDettaglioAllegato.ORIGINALE);
        dettaglioAllegato.setMimeType(mimeType);
        return dettaglioAllegato;
    }

    public Message getPecMittenteMessage(Doc doc) {
        Spedizione spedizioneMittenteFromDoc = getSpedizioneMittenteFromDoc(doc);
        Integer idMessage = spedizioneMittenteFromDoc.getIdMessage().getId();
        return messageRepository.getOne(idMessage);
    }

    public JSONObject getPecMittenteMessageJSONObjectByDoc(Doc doc) {
        JSONObject messageJsonObject = new JSONObject();
        Message message = getPecMittenteMessage(doc);
        messageJsonObject.put("idSorgentePec", message.getId());
        Related mittente = getMittentePE(doc);
        messageJsonObject.put("mittente", mittente.getDescrizione());
        messageJsonObject.put("subject", message.getSubject());
        messageJsonObject.put("dataArrivo", message.getReceiveTime());
        messageJsonObject.put("messageID", message.getUuidMessage());
        Pec pecDaCuiProtocollo = message.getIdPec();
        pecDaCuiProtocollo = pecRepository.getOne(pecDaCuiProtocollo.getId());
        messageJsonObject.put("indirizzoPecOrigine", pecDaCuiProtocollo.getIndirizzo());

        return messageJsonObject;
    }

    public Related getMittentePE(Doc doc) {
        Related mittente = null;
        List<Related> mittenti = doc.getMittenti();
        if (mittenti != null && !mittenti.isEmpty()) {
            mittente = mittenti.get(0);
        }
        return mittente;
    }

    public Spedizione getSpedizioneMittente(Related mittente) {
        Spedizione spedizioneMittente = new Spedizione();
        if (mittente != null) {
            List<Spedizione> spedizioneList = mittente.getSpedizioneList();
            if (spedizioneList != null && !spedizioneList.isEmpty()) {
                spedizioneMittente = spedizioneList.get(0);
            }
        }
        return spedizioneMittente;
    }

    public Spedizione getSpedizioneMittenteFromDoc(Doc doc) {
        Spedizione spedizioneMittente = new Spedizione();
        Related mittentePE = getMittentePE(doc);
        if (mittentePE != null) {
            spedizioneMittente = getSpedizioneMittente(mittentePE);
        }
        return spedizioneMittente;
    }

    public Allegato getAllegatoPrincipale(Doc doc) {
        Allegato allegatoDaTornare = null;
        List<Allegato> allegati = doc.getAllegati();
        for (Allegato allegato : allegati) {
            if (allegato.getPrincipale()) {
                allegatoDaTornare = allegato;
                break;
            }
        }
        return allegatoDaTornare;
    }

    public void getDettaglioAllegatoByTipo() {

    }

}
