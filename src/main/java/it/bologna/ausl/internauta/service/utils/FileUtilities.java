package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.estrattore.ExtractorCreator;
import it.bologna.ausl.estrattore.ExtractorResult;
import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilities.Detector;
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
            File tmp,
            String nomeFile) throws ExtractorException, IOException {

        ArrayList<ExtractorResult> extractAllResult = null;
        ExtractorCreator ec = new ExtractorCreator(tmp);

        if (ec.isExtractable()) {
            extractAllResult = ec.extractAll(folderToSave);
        }

        return extractAllResult;
    }

    public static String getHashFromFile(InputStream is, String algorithmName) throws FileNotFoundException, IOException, NoSuchAlgorithmException {

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

    /**
     * Torna l'hash in esadecimale del file, passato come bytes
     *
     * @param file i bytes del file
     * @param algorithm l'algoritmo da usare ES. SHA-256, MD5, ecc.
     * @return l'hash in esadecimale del file calcolato con l'algoritmo passato
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String getHashFromBytes(byte[] file, String algorithm) throws IOException, NoSuchAlgorithmException {

        MessageDigest mdigest = MessageDigest.getInstance(algorithm);

        // read the data from file and update that data in the message digest
        mdigest.update(file);

        // store the bytes returned by the digest() method
        byte[] hashBytes = mdigest.digest();

        // this array of bytes has bytes in decimal format so we need to convert it into hexadecimal format
        // for this we create an object of StringBuilder since it allows us to update the string i.e. its mutable
        StringBuilder sb = new StringBuilder();

        Formatter fmt = new Formatter();
        // loop through the bytes array
        for (int i = 0; i < hashBytes.length; i++) {

            // the following line converts the decimal into hexadecimal format and appends that to the StringBuilder object
            //sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
            fmt.format("%02X", hashBytes[i]);
        }

        // finally we return the complete hash
        return fmt.toString();
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

    public static String getMimeTypeFromPath(String path) throws IOException, UnsupportedEncodingException, MimeTypeException {
        Detector detector = new Detector();
        return detector.getMimeType(path);
    }

    public static String getMimeTypeFromInputStream(InputStream is) throws IOException, UnsupportedEncodingException, MimeTypeException {
        Detector detector = new Detector();
        return detector.getMimeType(is);
    }

}
