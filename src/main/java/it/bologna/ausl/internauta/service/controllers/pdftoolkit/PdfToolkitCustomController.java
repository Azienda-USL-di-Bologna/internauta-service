package it.bologna.ausl.internauta.service.controllers.pdftoolkit;

import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.utils.FileUtilities;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
@RestResource
@RequestMapping(value = "${pdftoolkit.mapping.url.root}")
public class PdfToolkitCustomController {
    private static final Logger log = LoggerFactory.getLogger(PdfToolkitCustomController.class);
    @Autowired
    private ReporitoryConnectionManager aziendeConnectionManager;
    
    /**
    * Restituisce una lista di file in un percorso specificato su MinIO.
    *
    * @param codiceAzienda Il codice aziendale associato ai file.
    * @param path Il percorso dei file da ottenere.
    * @return Una lista di oggetti MinIOWrapperFileInfo che rappresentano i file nel percorso specificato.
    *
    * Questo metodo riceve come parametri il codice aziendale e il percorso dei file da ottenere su MinIO.
    * Utilizza l'oggetto MinIOWrapper per ottenere la lista dei file nel percorso specificato.
    * Restituisce la lista di oggetti MinIOWrapperFileInfo che rappresentano i file nel percorso specificato.
    *
    * @throws MinIOWrapperException Se si verifica un errore durante l'interazione con MinIO.
    */
    @RequestMapping(value = "getFilesInPath", method = RequestMethod.GET)
    public List<MinIOWrapperFileInfo> getFilesInPath(@RequestParam("codiceAzienda") String codiceAzienda, @RequestParam("path") String path) throws MinIOWrapperException {
        
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        List<MinIOWrapperFileInfo> filesInPath = minIOWrapper.getFilesInPath(path, true, true, codiceAzienda);
        return filesInPath;
    }
    
    /**
     * Elimina i file in un percorso specificato su MinIO.
     *
     * @param relativePath Il percorso relativo dei resources del reporter da eliminare.
     * @return Un messaggio di stringa che indica lo stato dell'operazione di eliminazione.
     *
     * Questo metodo riceve come parametro il percorso relativo dei file da eliminare su MinIO del reporter.
     * Utilizza l'oggetto MinIOWrapper per ottenere la lista dei file nel percorso specificato.
     * Successivamente, itera su ogni file nella lista e lo rimuove utilizzando il metodo removeByFileId di MinIOWrapper.
     * Dopo l'eliminazione di ogni file, viene registrato un messaggio di log.
     * Infine, viene restituito un messaggio di completamento dell'operazione.
     *
     * @throws MinIOWrapperException Se si verifica un errore durante l'interazione con MinIO.
     */
    @RequestMapping(value = "deleteResources", method = RequestMethod.POST)
    public String deleteBucketPdfToolkit(@RequestParam(required = false) String relativePath) throws MinIOWrapperException {
        
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        List<MinIOWrapperFileInfo> filesInPath = minIOWrapper.getFilesInPath("/resources/reporter/" + relativePath, true, true, "pdftoolkit");
        for (MinIOWrapperFileInfo minIOFileInfo : filesInPath) {
            minIOWrapper.removeByFileId(minIOFileInfo.getFileId(), false);
            log.info("File: {} removed.", minIOFileInfo.getFileId());
        }
        return "Operazione completata.";
    }   
    
    /**
     * Gestisce la richiesta HTTP POST per il caricamento dei file.
     *
     * @param bucket Il nome del bucket dove il file verrà caricato.
     * @param codiceAzienda Il codice aziendale associato al file.
     * @param file Il file da caricare.
     * @return Un messaggio di stringa che indica lo stato dell'operazione di caricamento.
     * <br>
     * Questo metodo riceve un file, un nome di bucket e un codice aziendale come parametri. 
     * Crea un file temporaneo e una cartella temporanea, poi copia il file di input nel file temporaneo.
     * I contenuti del file temporaneo vengono quindi estratti nella cartella temporanea.
     * I contenuti della cartella temporanea vengono quindi caricati nel bucket specificato nello storage MinIO.
     * Se si verifica un errore durante queste operazioni, viene restituito un messaggio di errore appropriato.
     * Indipendentemente dall'esito, il file temporaneo e la cartella vengono eliminati prima che il metodo ritorni.
     */
    @RequestMapping(value = "/uploadFiles", method = RequestMethod.POST)
    public String uploadPdfToolkitResources(
            @RequestParam("bucket") String bucket, 
            @RequestParam("codiceAzienda") String codiceAzienda, 
            @RequestParam("file") MultipartFile file) {
        
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        File folderToSave = null;
        File fileTempToUpload = null;
        String responseMessage = "Operazione completata.";
        String fileName = file.getOriginalFilename();
        String extension = ".tmp";
        int pos = fileName.lastIndexOf(".");
        if (pos > 0 && pos < (fileName.length() - 1)) {
            extension = fileName.substring(pos);
            fileName = fileName.substring(0, pos);
        }
        try (InputStream stream = file.getInputStream()) {
            
            folderToSave = FileUtilities.getCartellaTemporanea(fileName);
            fileTempToUpload = File.createTempFile(fileName, extension);
            folderToSave.deleteOnExit();
            fileTempToUpload.deleteOnExit();
            
            FileUtils.copyInputStreamToFile(stream, fileTempToUpload);
            FileUtilities.estraiTuttoDalFile(folderToSave, fileTempToUpload, fileTempToUpload.getName());
            
            uploadToMinIO(folderToSave, folderToSave, minIOWrapper, codiceAzienda, bucket);
            
            File[] listFiles = folderToSave.listFiles();
            for (File listFile : listFiles) {
                Files.move(listFile.toPath(), Paths.get(System.getProperty("java.io.tmpdir") + listFile.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (ExtractorException ex) {
            responseMessage = "Errore durante l'estrazione del file zip.";
            log.error(responseMessage);
        } catch (MinIOWrapperException ex) {
            responseMessage = "Errore durante l'upload su minIo.";
            log.error(responseMessage);
        } catch (IOException ex) {
            responseMessage = "Errore durante la creazione dei file temporanei.";
            log.error(responseMessage);
        } finally {
            if (folderToSave != null && folderToSave.exists())
                folderToSave.delete();
            if (fileTempToUpload != null && fileTempToUpload.exists())
                fileTempToUpload.delete();
        }   
        return responseMessage;
    }

    /**
     * Carica un file o una directory di file su MinIO.
     *
     * @param fileToUpload Il file o la directory da caricare.
     * @param folderToSave La cartella dove salvare i file.
     * @param minIOWrapper L'oggetto MinIOWrapper per interagire con MinIO.
     * @param codiceAzienda Il codice aziendale associato al file.
     * @param bucket Il nome del bucket dove il file verrà caricato.
     *
     * Questo metodo verifica se il file da caricare è una directory. 
     * Se è una directory, itera su tutti i file nella directory e li carica ricorsivamente.
     * Se non è una directory, carica il file su MinIO utilizzando il metodo putWithBucket di MinIOWrapper.
     * Il percorso del file viene calcolato come il percorso relativo dal folderToSave al fileToUpload.
     * Dopo il caricamento di ogni file, viene registrato un messaggio di log.
     *
     * @throws MinIOWrapperException Se si verifica un errore durante il caricamento su MinIO.
     * @throws IOException Se si verifica un errore durante la lettura del file o della directory.
     */
    private void uploadToMinIO(File fileToUpload, File folderToSave, MinIOWrapper minIOWrapper, String codiceAzienda, String bucket) throws MinIOWrapperException, IOException {
        if (fileToUpload.isDirectory()) {
            File[] listFiles = fileToUpload.listFiles();
            for (File fileList : listFiles) {
                uploadToMinIO(fileList, folderToSave, minIOWrapper, codiceAzienda, bucket);
            }
        } else {
            String path = "/" + folderToSave.toURI().relativize(fileToUpload.toURI()).getPath();
            minIOWrapper.putWithBucket(fileToUpload, codiceAzienda, path, fileToUpload.getName(), null, true, bucket);
            log.info("File uploaded: {} ok", path);
        }
    }
}
