package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.estrattore.ExtractorResult;
import it.bologna.ausl.estrattore.exception.ExtractorException;
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
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.mime.MimeTypeException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        MinIOWrapper minIOWrapper = aziendeConnectionManager.
                getMinIOWrapper();
        InputStream file = minIOWrapper.getByUuid(message.getUuidRepository());
        if (file != null) {
            List<Allegato> allegatiCreatiDalMessaggio = creaAndAllegaAllegati(doc, file, message.getName());
            doc.setAllegati(allegatiCreatiDalMessaggio);

        } else {
            throw new FileNotFoundException("File non trovato sul repository " + message.getUuidRepository());
        }
        // qui ci va il codice per aggiungere DocMessage 
        // (tabella di cross che non esiste ancora)
        return doc;
    }

    public List<Allegato> creaAndAllegaAllegati(Doc doc, InputStream fileIS, String fileName) throws MinIOWrapperException, IOException, FileNotFoundException, NoSuchAlgorithmException, Throwable {
        List<Allegato> allegatiDaTornare = new ArrayList<Allegato>();
        if (fileIS != null) {
            File folderToSave = FileUtilities.getCartellaTemporanea(
                    "EstrazioneTemp" + doc.getId().toString() + "_" + UUID.randomUUID().toString());
            try {
                allegatiDaTornare = estraiRicorsivamenteSalvaAndFaiUploadAllegatiDaInputStream(
                        fileIS, fileName,
                        doc, folderToSave);
                FileUtilities.svuotaCartella(folderToSave.getAbsolutePath());

            } catch (Throwable ex) {
                FileUtilities.svuotaCartella(folderToSave.getAbsolutePath());
                throw ex;
            }

        } else {
            throw new FileNotFoundException("Passato file null ");
        }

        return allegatiDaTornare;
    }

    public MinIOWrapperFileInfo putFileOnMinIO(File file,
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
        String separatoreDiSiStema = System.getProperty("file.separator");
        CharSequence daRimpiazzare = separatoreDiSiStema;
        CharSequence sostituto = "\\" + separatoreDiSiStema;
        nomeDelFile = nomeDelFile.replace(daRimpiazzare, sostituto);

        //TODO: GENERARE CON LIBRERIA JAVA
        File tmp = new File(folderToSave.getAbsolutePath()
                + separatoreDiSiStema + nomeDelFile);
        FileUtils.copyInputStreamToFile(allegatoInputStream, tmp);

        ArrayList<ExtractorResult> extractionResultAll = FileUtilities.estraiTuttoDalFile(
                folderToSave,
                tmp,
                nomeDelFile);

        HashMap<String, Object> mappaHashAllegati = new HashMap();
        int numeroAllegato
                = doc.getAllegati() != null
                ? doc.getAllegati().size() : 0;

        if (extractionResultAll != null) {
            for (ExtractorResult er : extractionResultAll) {
                numeroAllegato++;
                try {
                    File file = new File(er.getPath());
                    MinIOWrapperFileInfo putFileOnMongo = putFileOnMinIO(file, doc.getIdAzienda().getCodice(), doc.getId().toString(),
                            er.getFileName(), null, true);
                    Allegato nuovoAllegato = buildNewAllegato(doc, er.getFileName());
                    nuovoAllegato.setOrdinale(numeroAllegato);
                    Integer intSize = new Long(er.getSize()).intValue();
                    nuovoAllegato = allegatoRepository.save(nuovoAllegato);

                    DettaglioAllegato dettaglioAllegato = buildNewDettaglioAllegato(
                            nuovoAllegato,
                            file,
                            er.getFileName(),
                            er.getMimeType(),
                            intSize,
                            putFileOnMongo);

                    dettaglioAllegato = dettaglioAllegatoRepository.save(dettaglioAllegato);
                    List<DettaglioAllegato> dettagliAllegatiList = new ArrayList();
                    dettagliAllegatiList.add(dettaglioAllegato);
                    nuovoAllegato.setDettagliAllegatiList(dettagliAllegatiList);

                    if (er.getPadre() != null) {
                        Allegato padre = (Allegato) mappaHashAllegati.get(er.getPadre());
                        nuovoAllegato.setIdAllegatoPadre(padre);
                    }
                    nuovoAllegato = allegatoRepository.save(nuovoAllegato);

                    mappaHashAllegati.put(er.getHash(), nuovoAllegato);

                    allegatiDaTornare.add(nuovoAllegato);
                } catch (Throwable e) {
                    FileUtilities.svuotaCartella(folderToSave.getAbsolutePath());
                    throw e;
                }
            }
        } else {
            try {
                MinIOWrapperFileInfo putFileOnMinIO = putFileOnMinIO(tmp, doc.getIdAzienda().getCodice(), doc.getId().toString(),
                        nomeDelFile, null, true);
                Allegato nuovoAllegato = buildNewAllegato(doc, nomeDelFile);
                numeroAllegato = doc.getAllegati() != null ? doc.getAllegati().size() + 1 : 1;
                nuovoAllegato.setOrdinale(numeroAllegato);
                Integer intSize = new Long(Files.size(tmp.toPath())).intValue();
                nuovoAllegato = allegatoRepository.save(nuovoAllegato);

                DettaglioAllegato dettaglioAllegato = buildNewDettaglioAllegato(
                        nuovoAllegato,
                        tmp,
                        nomeDelFile,
                        FileUtilities.getMimeTypeFromPath(tmp.getAbsolutePath()),
                        intSize,
                        putFileOnMinIO);

                dettaglioAllegato = dettaglioAllegatoRepository.save(dettaglioAllegato);
                List<DettaglioAllegato> dettagliAllegatiList = new ArrayList();
                dettagliAllegatiList.add(dettaglioAllegato);
                nuovoAllegato.setDettagliAllegatiList(dettagliAllegatiList);
                nuovoAllegato = allegatoRepository.save(nuovoAllegato);
                mappaHashAllegati.put(putFileOnMinIO.getMd5(), nuovoAllegato);
                allegatiDaTornare.add(nuovoAllegato);
                doc.getAllegati().addAll(allegatiDaTornare);
            } catch (Throwable e) {
                FileUtilities.svuotaCartella(folderToSave.getAbsolutePath());
                throw e;
            }
        }

        tmp.delete();
        allegatoInputStream.close();
        return allegatiDaTornare;
    }

    public Allegato buildNewAllegato(Doc doc, String originalFileName) {
        List<Allegato> allegati = doc.getAllegati();
        Integer numeroOrdine = !(allegati == null || allegati.isEmpty())
                ? allegati.size() : 0;
        numeroOrdine++;
        Allegato allegato = new Allegato();
        allegato.setNome(FilenameUtils.getBaseName(originalFileName));
        allegato.setIdDoc(doc);
        allegato.setPrincipale(false);
        allegato.setTipo(Allegato.TipoAllegato.ALLEGATO);
        allegato.setDataInserimento(ZonedDateTime.now());
        allegato.setOrdinale(numeroOrdine);
        allegato.setFirmato(false);

        return allegato;
    }

    public DettaglioAllegato buildNewDettaglioAllegato(Allegato allegato,
            File file,
            String fileNameWithExtension,
            String mimeType,
            Integer size, MinIOWrapperFileInfo minioFileInfo) throws IOException, FileNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException, MimeTypeException {

        DettaglioAllegato dettaglioAllegato = new DettaglioAllegato();
        dettaglioAllegato.setHashMd5(minioFileInfo.getMd5());

//        dettaglioAllegato.setHashSha256(FileUtilities.getHashFromFile(fileInputStream, "SHA-256"));
        try ( InputStream is = new FileInputStream(file)) {
            dettaglioAllegato.setHashSha256(org.apache.commons.codec.digest.DigestUtils.sha256Hex(is));
        }
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
