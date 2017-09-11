package it.nextsw.file;

import it.nextsw.file.exception.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * L'interfaccia di base per interagire con i file
 * Inserisce uno strato di astrazione che permette di avere trasparenza ai livelli superiori
 * su come e dove i file vangano salvati, letti e cancellati
 *
 * Created by f.longhitano on 27/06/2017.
 */
public interface FileRepository {

    /**
     * Crea un file nella root chimandolo con un nome random
     * @param fileExtension l'estensione del file da creare
     * @return il nome del file generato
     * @throws FileRepositoryCreateException
     */
    public String createFile(String fileExtension) throws FileRepositoryCreateException;

    /**
     * Crea un file con un nome specifico all'interno del repository
     * @param fileName il nome del file
     * @throws FileRepositoryCreateException
     */
    public void createFileWithName(String fileName) throws FileRepositoryCreateException;


    public OutputStream writeFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException;

    public InputStream readFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException;

    public void deleteFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException;

    public void copyFile(String fileName, FileRepository targhetFileRepository) throws FileRepositoryFileNotFoundException, FileRepositoryException;

    public void moveFile(String fileName, FileRepository targhetFileRepository) throws FileRepositoryFileNotFoundException, FileRepositoryException;


    public List<String> listFiles() throws FileRepositoryException;

    public List<String> listDirs() throws FileRepositoryException;


    //public void mkDirs(String pathDir) throws FileRepositoryException;

    public void mkDir(String dirName) throws FileRepositoryException;

    public FileRepository exploreDir(String dirName) throws FileRepositoryException;

    public boolean isFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException;

    public boolean isDir(String dirName) throws FileRepositoryFileNotFoundException, FileRepositoryException;

    public boolean isFileExist(String fileName) throws FileRepositoryException;

}
