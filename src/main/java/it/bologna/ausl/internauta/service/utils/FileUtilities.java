/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.estrattore.ExtractorCreator;
import it.bologna.ausl.estrattore.ExtractorResult;
import it.bologna.ausl.estrattoremaven.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilitymaven.Detector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Salo
 */
public class FileUtilities {

    public static File getCartellaTemporanea(String nomeTemp) {
        return new File(System.getProperty("java.io.tmpdir")
                + nomeTemp
                + System.getProperty("file.separator"));
    }

    public static ArrayList<ExtractorResult> estraiTuttoDalFile(File folderToSave,
            InputStream fileInputStream,
            String nomeFile) throws ExtractorException, IOException {
        String separatoreDiSiStema = System.getProperty("file.separator");
        CharSequence daRimpiazzare = separatoreDiSiStema;
        CharSequence sostituto = "\\" + separatoreDiSiStema;
        nomeFile = nomeFile.replace(daRimpiazzare, sostituto);

        ArrayList<ExtractorResult> extractAllResult = null;
        File tmp = new File(folderToSave.getAbsolutePath()
                + separatoreDiSiStema + nomeFile);
        FileUtils.copyInputStreamToFile(fileInputStream, tmp);
        ExtractorCreator ec = new ExtractorCreator(tmp);
        if (ec.isExtractable()) {
            extractAllResult = ec.extractAll(folderToSave);
        }
        return extractAllResult;
    }

    public static String getHashFromFile(InputStream is,
            String algorithmName) throws FileNotFoundException,
            IOException, NoSuchAlgorithmException {

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
        return hashString;
    }

    public static void svuotaCartella(String dirDaSvuotareAbsolutePath) {
        File directory = new File(dirDaSvuotareAbsolutePath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    public static boolean isSupportedMimeType(MultipartFile file) throws Exception {
        return ExtractorCreator.isSupportedMimyType(file.getContentType());
    }

    public static boolean isExtractableFile(MultipartFile file) throws Exception {
        return ExtractorCreator.isSupportedMimyType(file.getContentType());
    }

    public static String getMimeTypeFromMultipart(MultipartFile multipart) throws IOException, UnsupportedEncodingException, MimeTypeException {
        return getMimeTypeFromInputStream(multipart.getInputStream());
    }

    public static String getMimeTypeFromInputStream(InputStream is) throws IOException, UnsupportedEncodingException, MimeTypeException {
        Detector detector = new Detector();
        return detector.getMimeType(is);
    }

}
