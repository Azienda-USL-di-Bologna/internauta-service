package it.bologna.ausl.baborg.file.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.URLConnection;


/**
 * Classe di Utils per la gestione dei file
 */
public class NextFileUtils {

    private static final String EXTENSION_DELIMITER=".";

    public static String extractFileExtension(String fileName){
        Integer lastPointIndex=fileName.lastIndexOf(EXTENSION_DELIMITER);
        return fileName.substring(lastPointIndex+1);
    }

    /**
     *  Cerca di estrarre il mimeType a partire dal nome
     * @param fileName il nome del file
     * @return il mimeType del file se trovato, null altrimenti
     */
    public  static String extractMimeTye(String fileName){
        String mimeType=null;
        if(StringUtils.isNotBlank(fileName))
            mimeType=URLConnection.getFileNameMap().getContentTypeFor(fileName);
//        if(StringUtils.isNotBlank(mimeType))
//            Files.probeContentType(Paths.get())
        return mimeType;
    }
}
