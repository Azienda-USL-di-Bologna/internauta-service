package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.estrattore.ExtractorResult;
import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.scripta.AllegatoException;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.scripta.Allegato;
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
import java.lang.reflect.InvocationTargetException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class ScriptaUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptaUtils.class);
    
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    AllegatoRepository allegatoRepository;

    @Autowired
    private PecRepository pecRepository;

    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

    public Doc protocollaMessaggio(Doc doc, Message message) throws MinIOWrapperException, IOException, FileNotFoundException, NoSuchAlgorithmException, Throwable {
        MinIOWrapper minIOWrapper = aziendeConnectionManager.
                getMinIOWrapper();
        InputStream file = minIOWrapper.getByUuid(message.getUuidRepository());
        if (file != null) {
            List<Allegato> allegatiCreatiDalMessaggio = creaAndAllegaAllegati(doc, file, message.getName(), false);
            doc.setAllegati(allegatiCreatiDalMessaggio);

        } else {
            throw new FileNotFoundException("File non trovato sul repository " + message.getUuidRepository());
        }
        // qui ci va il codice per aggiungere DocMessage 
        // (tabella di cross che non esiste ancora)
        return doc;
    }

    
    public List<Allegato> creaAndAllegaAllegati(Doc doc, InputStream allegatoInputStream, String fileName, boolean fileEstrattiSuTemp) 
            throws MinIOWrapperException, IOException, FileNotFoundException, NoSuchAlgorithmException, ExtractorException, UnsupportedEncodingException, MimeTypeException, AllegatoException {
        return creaAndAllegaAllegati(doc, allegatoInputStream, fileName, fileEstrattiSuTemp, false, null, false, null);
    }
    
    /**
     * 
     * @param doc
     * @param allegatoInputStream
     * @param fileName
     * @param fileEstrattiSuTemp
     * @param skipInsertEstraibileInMinio
     * @param fileUuid
     * @param doUpdate parametro usato qualora gli allegati già esistono ma ne va aggiornato l'idRepository, parametro usato per allegati dentro un contenitore
     * @param mappaAllegatiToUpdate  chiave: md5, valore: allegato da aggiornare col nuovo idRepository sul dettaglio originale
     * @return
     * @throws MinIOWrapperException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException 
     * @throws it.bologna.ausl.estrattore.exception.ExtractorException 
     * @throws org.apache.tika.mime.MimeTypeException 
     */
    public List<Allegato> creaAndAllegaAllegati(
            Doc doc, 
            InputStream allegatoInputStream, 
            String fileName,
            boolean fileEstrattiSuTemp,
            boolean skipInsertEstraibileInMinio,
            String fileUuid,
            boolean doUpdate,
            HashMap<String, Allegato> mappaAllegatiToUpdate
    ) throws MinIOWrapperException, IOException, FileNotFoundException, 
            NoSuchAlgorithmException, ExtractorException, UnsupportedEncodingException, MimeTypeException, AllegatoException {
        if (allegatoInputStream == null) {
            throw new FileNotFoundException("Passato file null ");
        }
        
        // Del file passato creerò uno o più allegati che poi tornerò al chiamante
        List<Allegato> allegatiDaTornare = new ArrayList<>();
        File folderToSave = FileUtilities.getCartellaTemporanea("EstrazioneTemp_" + doc.getId().toString() + "_" + UUID.randomUUID().toString());
        
        try {
            String separatoreDiSiStema = System.getProperty("file.separator");
            CharSequence daRimpiazzare = separatoreDiSiStema;
            CharSequence sostituto = "\\" + separatoreDiSiStema;
            fileName = fileName.replace(daRimpiazzare, sostituto);

            File tmp = File.createTempFile("Allegato_", "." + FilenameUtils.getExtension(fileName));
            FileUtils.copyInputStreamToFile(allegatoInputStream, tmp);

            ArrayList<ExtractorResult> extractionResultAll = FileUtilities.estraiTuttoDalFile(folderToSave, tmp, fileName);

            HashMap<String, Object> mappaHashAllegati = new HashMap();
            //HashMap<String, Object> mappaMd5Allegati = new HashMap();
            int numeroAllegato  = doc.getAllegati() != null ? doc.getAllegati().size() : 0;

            if (extractionResultAll != null) {
                /* L'allegato è un contenitore. Devo quindi salvare sia lui che tutti i vari figli e nipoti etc
                   Se skipInsertEstraibileInMinio è true allora il contenitore non va salavato su minio (salvo bug il motivo è che c'è già)
                   Se doUpdate è true, allora non devo fare la insert degli allegati, in teoria già ci sono. Devo aggiornarli (in paritoclare l'idRepository)*/
                String codiceAzienda = doc.getIdAzienda().getCodice();
                int index = 0;
                for (ExtractorResult er : extractionResultAll) {
                    numeroAllegato++;
                    index++;
                    File file = new File(er.getPath());
                    Allegato padre = null;
                    if (er.getPadre() != null) {
                        padre = (Allegato) mappaHashAllegati.get(er.getPadre());
                    }
                    MinIOWrapperFileInfo fileSuMinioInfo = null;
                    boolean fileAlreadyPresent = false;
                    if (skipInsertEstraibileInMinio && index == 1) {
                        // Il contenitore è già su minio, mi prendo i suoi dati
                        fileSuMinioInfo = getFileFromMinIO(fileUuid);
                    } else {
                        // Controllo se un file identico è già stato salavato (cioè il file contenitore contiene più volte lo stesso file). Se si non lo salvo di nuovo.
                        if (mappaHashAllegati.get(er.getHash()) == null) {
                            // Inserisco il file su minio
                            fileSuMinioInfo = putFileOnMinIO(
                                file,
                                codiceAzienda + (fileEstrattiSuTemp && index > 1 ? "t": ""), 
                                doc.getId().toString(), 
                                er.getFileName(), 
                                null, 
                                true
                            );
                        } else {
                            fileAlreadyPresent = true;
                        }
                    }
                    
                    if (!fileAlreadyPresent) {
                        Allegato nuovoAllegato = null;
                    
                        if (doUpdate) {
                            // In caso di doUpdate il contenitore lo scarto, inoltre non inserisco un 
                            // nuovo allegato, ma aggiorno quello che trovo nella mappaAllegatiToUpdate tramite md5
                            if (index > 1) {
                                Allegato allegatoToUpdate = mappaAllegatiToUpdate.get(er.getMd5());
                                Allegato.DettaglioAllegato originale = allegatoToUpdate.getDettagli().getOriginale();
                                originale.setIdRepository(fileSuMinioInfo.getFileId());
                                originale.setBucket(fileSuMinioInfo.getBucketName());
                                nuovoAllegato = allegatoRepository.save(allegatoToUpdate);
                            }
                        } else {
                            nuovoAllegato = buildAndSaveAllegato(
                                er.getFileName(),
                                doc,
                                file,
                                numeroAllegato,
                                padre,
                                er.getIsExtractable(),
                                fileSuMinioInfo
                            );
                        }
                        if (nuovoAllegato != null) {
                            mappaHashAllegati.put(er.getHash(), nuovoAllegato);
                            allegatiDaTornare.add(nuovoAllegato);
                        }
                    }
                }
            } else {
                // Non è un allegato contenitore. Lo salvo semplicemente.
                MinIOWrapperFileInfo fileSuMinioInfo = putFileOnMinIO(
                    tmp,
                    doc.getIdAzienda().getCodice(), 
                    doc.getId().toString(), 
                    fileName, 
                    null, 
                    true
                );
                Allegato nuovoAllegato = buildAndSaveAllegato(
                    fileName,
                    doc,
                    tmp,
                    numeroAllegato + 1,
                    null,
                    false,
                    fileSuMinioInfo
                );
                allegatiDaTornare.add(nuovoAllegato);
            }
            tmp.delete(); // TODO: chiedi a gdm, queste due righe vanno nel finally?
            allegatoInputStream.close();
        } finally {
            FileUtilities.svuotaCartella(folderToSave.getAbsolutePath());
        }
        
        return allegatiDaTornare;
    }

    /**
     * Salvo il file su minio
     * @param file
     * @param codiceAzienda
     * @param path
     * @param fileName
     * @param metadata
     * @param overWrite
     * @return
     * @throws MinIOWrapperException
     * @throws IOException 
     */
    public MinIOWrapperFileInfo putFileOnMinIO(
            File file,
            String codiceAzienda, 
            String path, 
            String fileName,
            Map<String, Object> metadata, 
            boolean overWrite
    ) throws MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        MinIOWrapperFileInfo savedFileOnRepositoryFileInfo = minIOWrapper.put(
                file, codiceAzienda,
                path, fileName, null, false);
        return savedFileOnRepositoryFileInfo;
    }
    
    public MinIOWrapperFileInfo getFileFromMinIO(
            String fileUuid
    ) throws MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        MinIOWrapperFileInfo savedFileOnRepositoryFileInfo = minIOWrapper.getFileInfoByUuid(fileUuid);
        return savedFileOnRepositoryFileInfo;
    }

    
    /**
     * Questa funzione si occupa di creare un nuovo allegato con il suo 
     * dettaglio originale, lo mette su minio e lo salva sul db.
     * @param nomeDelFile
     * @param doc
     * @param file
     * @param numeroAllegato
     * @param allegatoPadre
     * @return
     * @throws MinIOWrapperException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws MimeTypeException
     * @throws Http500ResponseException
     * @throws AllegatoException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException 
     */
    private Allegato buildAndSaveAllegato(
            String nomeDelFile,
            Doc doc,
            File file,
            Integer numeroAllegato,
            Allegato allegatoPadre,
            boolean isEstraibile,
            MinIOWrapperFileInfo fileSuMinioInfo
    ) throws MinIOWrapperException, IOException, FileNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException, MimeTypeException, AllegatoException {
        // Creo l'allegato
        Allegato nuovoAllegato = buildNewAllegato(doc, nomeDelFile);
        nuovoAllegato.setOrdinale(numeroAllegato);
        nuovoAllegato.setEstraibile(isEstraibile);
        Integer intSize = Long.valueOf(Files.size(file.toPath())).intValue();
        // Creo il dettaglio allegato
        Allegato.DettaglioAllegato dettaglioAllegato = buildNewDettaglioAllegato(
                nuovoAllegato,
                file,
                nomeDelFile,
                FileUtilities.getMimeTypeFromPath(file.getAbsolutePath()),
                intSize,
                fileSuMinioInfo
        );
        Allegato.DettagliAllegato dettagliAllegato = new Allegato.DettagliAllegato();
        nuovoAllegato.setDettagli(dettagliAllegato);
        addDettaglioAllegato(dettagliAllegato, dettaglioAllegato, Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE);
        nuovoAllegato.setIdAllegatoPadre(allegatoPadre);
        return allegatoRepository.save(nuovoAllegato);
    }

    public Allegato buildNewAllegato(Doc doc, String originalFileName) {
        List<Allegato> allegati = doc.getAllegati();
        Integer numeroOrdine = !(allegati == null || allegati.isEmpty()) ? allegati.size() : 0;
        numeroOrdine++;
        Allegato allegato = new Allegato();
        allegato.setNome(FilenameUtils.getBaseName(originalFileName));
        allegato.setIdDoc(doc);
        allegato.setPrincipale(false);
        
        switch (doc.getTipologia()) {
            case DOCUMENT_UTENTE:
            case DOCUMENT_PEC:
                allegato.setTipo(Allegato.TipoAllegato.FASCICOLATO);
                break;
            default:
                allegato.setTipo(Allegato.TipoAllegato.ALLEGATO);
        }
        
        allegato.setDataInserimento(ZonedDateTime.now());
        allegato.setOrdinale(numeroOrdine);
        allegato.setFirmato(false);

        return allegato;
    }

    
    /**
     * Aggiunge ai DettagliAllegato il DettaglioAllegato passato. 
     * Se la chiave (il tipo di dettaglio allegato) esiste già lancia eccezione
     * @param dettagli
     * @param dettaglioAllegato
     * @param tipoDettaglioAllegato
     * @throws it.bologna.ausl.internauta.service.exceptions.scripta.AllegatoException
     */
    public void addDettaglioAllegato(
            Allegato.DettagliAllegato dettagli, 
            Allegato.DettaglioAllegato dettaglioAllegato,
            Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato) throws AllegatoException {
        Allegato.DettaglioAllegato dettaglioAllegatoTemp;
        try {
            dettaglioAllegatoTemp = dettagli.getDettaglioAllegato(tipoDettaglioAllegato);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.info("errore nel recuperare il dettaglio allegato", ex);
            throw new AllegatoException("errore nel recuperare il dettaglio allegato", ex);
        }
        if (dettaglioAllegatoTemp != null) {
            LOG.info("Il dettaglio allegato che si vuole aggiungere è già presente");
            throw new AllegatoException("Il dettaglio allegato che si vuole aggiungere è già presente");
        }
        try {
            dettagli.setDettaglioAllegato(dettaglioAllegato, tipoDettaglioAllegato);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.info("errore nel settare il dettaglio allegato", ex);
            throw new AllegatoException("errore nel settare il dettaglio allegato", ex);
        }
    }
    
    public Allegato.DettaglioAllegato buildNewDettaglioAllegato(
            Allegato allegato,
            File file,
            String fileNameWithExtension,
            String mimeType,
            Integer size, 
            MinIOWrapperFileInfo minioFileInfo
    ) throws IOException, FileNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException, MimeTypeException {
        try ( InputStream is = new FileInputStream(file)) {
            return buildNewDettaglioAllegato(
                allegato,
                is,
                fileNameWithExtension,
                mimeType,
                size, 
                minioFileInfo
            );
        }
    }
    
    public Allegato.DettaglioAllegato buildNewDettaglioAllegato(
            Allegato allegato,
            InputStream file,
            String fileNameWithExtension,
            String mimeType,
            Integer size, 
            MinIOWrapperFileInfo minioFileInfo
    ) throws IOException, FileNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException, MimeTypeException {
        Allegato.DettaglioAllegato dettaglioAllegato = new Allegato.DettaglioAllegato();
        dettaglioAllegato.setHashMd5(minioFileInfo.getMd5());
        dettaglioAllegato.setHashSha256(org.apache.commons.codec.digest.DigestUtils.sha256Hex(file));
        dettaglioAllegato.setNome(FilenameUtils.getBaseName(fileNameWithExtension));
        dettaglioAllegato.setEstensione(FilenameUtils.getExtension(fileNameWithExtension));
        dettaglioAllegato.setDimensioneByte(size);
        dettaglioAllegato.setIdRepository(minioFileInfo.getFileId());
        dettaglioAllegato.setMimeType(mimeType);
        dettaglioAllegato.setBucket(minioFileInfo.getBucketName());
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

}
