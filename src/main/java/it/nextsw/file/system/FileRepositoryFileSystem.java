package it.nextsw.file.system;

import it.nextsw.file.FileRepository;
import it.nextsw.file.exception.*;
import it.nextsw.file.utils.NameGenerationUtil;
import org.apache.commons.io.IOUtils;


import javax.annotation.PostConstruct;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by f.longhitano on 27/06/2017.
 */
public abstract class FileRepositoryFileSystem implements FileRepository {


    /**
     *
     * @return la directory root su cui opera il File repository
     */
    public abstract String getBaseUrl();


    @PostConstruct
    public void init() {
    }

    protected void createDirectoryBaseUrl(){
        File file = new File(getBaseUrl());
        try {
            if (!file.exists())
                file.mkdirs();
        } catch (Exception e) {
            throw new IllegalStateException("Base path for " + this.getClass().getName() + " not valid");
        }
    }

    public FileRepositoryFileSystem() {
        this.createDirectoryBaseUrl();
    }

    @Override
    public String createFile(String fileExtension) throws FileRepositoryCreateException {
        String generatedName = getGeneratedFileName() + "." + fileExtension;
        this.createFileWithName(generatedName, false);
        return generatedName;
    }

    @Override
    public void createFileWithName(String fileName) throws FileRepositoryCreateException {
        this.createFileWithName(fileName, false);
    }


    /**
     * Crea un file con un nome specifico e con un path specifico all'interno del repository
     *
     * @param path                       il nome del file comprensivo di eventuale path. example folder1/folder2/file.txt
     * @param createDirectoryIfNecessary se true il {@param fileName} contiene tutto il path completo (provvede a generare le cartelle mancanti)
     *                                   se false il {@param fileName} deve contente il solo nome del file che verra creato nella root
     * @throws FileRepositoryCreateException
     */
    protected void createFileWithName(String path, boolean createDirectoryIfNecessary) throws FileRepositoryCreateException {
        File file = new File(createPathFile(path));
        try {
            if (createDirectoryIfNecessary) {
                try {
                    SplittedPath splittedPath = splitPath(path);
                    mkDirs(splittedPath.pathDirs);
                } catch (Exception e) {
                    throw new FileRepositoryCreateException("Exception during create dir for new file " + file.getAbsolutePath(), e);
                }
            }
            if (file.createNewFile())
                return;
            else
                throw new FileRepositoryCreateException("File " + createPathFile(path) + " exist");
        } catch (FileRepositoryCreateException e) {
            throw e;
        } catch (Exception e) {
            throw new FileRepositoryCreateException("Exception during create file " + file.getAbsolutePath(), e);
        }
    }


    @Override
    public OutputStream writeFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException {
        File file = new File(createPathFile(fileName));
        checkFileExist(file);
        try {
            return new FileOutputStream(file);
        } catch (Exception e) {
            throw new FileRepositoryException("Exception during write file " + createPathFile(fileName), e);
        }
    }

    @Override
    public InputStream readFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException {
        File file = new File(createPathFile(fileName));
        checkFileExist(file);
        try {
            return new FileInputStream(file);
        } catch (Exception e) {
            throw new FileRepositoryException("Exception during read file " + createPathFile(fileName), e);
        }
    }

    @Override
    public void deleteFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException {
        File file = new File(createPathFile(fileName));
        checkFileExist(file);
        try {
            if (!file.delete())
                throw new FileRepositoryException("Exception during delete, file not deleted");
        } catch (Exception e) {
            throw new FileRepositoryException("Exception during delete file " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public void copyFile(String fileName, FileRepository targhetFileRepository) throws FileRepositoryFileNotFoundException, FileRepositoryException{
        File file = new File(createPathFile(fileName));
        checkFileExist(file);
        if(!isFile(fileName))
            throw new FileRepositoryException("Cannot copy a directory "+file.getAbsolutePath());
        try {
            targhetFileRepository.createFileWithName(fileName);
            OutputStream outputStream=targhetFileRepository.writeFile(fileName);
            InputStream inputStream=readFile(fileName);
            IOUtils.copy(inputStream,outputStream);
            IOUtils.closeQuietly(inputStream,outputStream);
        }catch (Exception e){
            throw new FileRepositoryException("Exception during copy file " + file.getAbsolutePath()+" to repository "+targhetFileRepository, e);
        }
    }

    @Override
    public void moveFile(String fileName, FileRepository targhetFileRepository) throws FileRepositoryFileNotFoundException, FileRepositoryException{
        this.copyFile(fileName,targhetFileRepository);
        try{
            this.deleteFile(fileName);
        } catch (Exception e){
            //quando va in eccezione se il file si trova in entrambi i repository lo cancello da quello targhet
            if(targhetFileRepository.isFileExist(fileName) && this.isFileExist(fileName))
                targhetFileRepository.deleteFile(fileName);
            throw new FileRepositoryException("Exception during copy file " + fileName+" to repository "+targhetFileRepository, e);
        }
    }

    @Override
    public List<String> listFiles() throws FileRepositoryException {
        File file = new File(getBaseUrl());
        try {
            return Arrays.stream(file.listFiles()).filter(file1 -> file1.isFile()).map(file1 -> file1.getName()).collect((Collectors.toList()));
//            String[] a=file.list();
//            return Arrays.asList(file.list());
        } catch (Exception e) {
            throw new FileRepositoryException("Exception during request files of dir " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public List<String> listDirs() throws FileRepositoryException{
        File file = new File(getBaseUrl());
        try {
            return Arrays.stream(file.listFiles()).filter(file1 -> file1.isDirectory()).map(file1 -> file1.getName()).collect((Collectors.toList()));

            //return Arrays.asList(file.list());
        } catch (Exception e) {
            throw new FileRepositoryException("Exception during request files of dir " + file.getAbsolutePath(), e);
        }
    }


    @Override
    public boolean isFile(String fileName) throws FileRepositoryFileNotFoundException {
        File file = new File(createPathFile(fileName));
        checkFileExist(file);
        return file.isFile();
    }

    @Override
    public boolean isDir(String dirName) throws FileRepositoryFileNotFoundException {
        File dir = new File(createPathFile(dirName));
        checkFileExist(dir);
        return dir.isDirectory();
    }

    @Override
    public boolean isFileExist(String fileName) throws FileRepositoryException{
        File file = new File(createPathFile(fileName));
        try {
            return file.exists();
        } catch (Exception e){
            throw new FileRepositoryException("Exception during check file exist "+file.getAbsolutePath(),e);
        }
    }


    protected void checkFileExist(File file) throws FileRepositoryFileNotFoundException {
        if (!file.exists())
            throw new FileRepositoryFileNotFoundException("file not found " + file.getAbsolutePath());
    }

    @Override
    public FileRepository exploreDir(String dirName) throws FileRepositoryException {
        String newrepoBaseUrl=createPathFile(dirName);
        return new FileRepositoryFileSystem() {
            @Override
            public String getBaseUrl() {
                return newrepoBaseUrl;
            }
        };

    }

    @Override
    public void mkDir(String dirName) throws FileRepositoryException {
        File file = new File(createPathFile(dirName));
        try {
            file.mkdir();
        } catch (Exception e) {
            throw new FileRepositoryException("Exception during create directory for path " + file.getAbsolutePath(), e);
        }
    }


    protected void mkDirs(String dirName) throws FileRepositoryException {
        File file = new File(createPathFile(dirName));
        try {
            file.mkdirs();
        } catch (Exception e) {
            throw new FileRepositoryException("Exception during create directory for path " + file.getAbsolutePath(), e);
        }
    }


    protected String getGeneratedFileName() {
        return NameGenerationUtil.generateName();
    }

    protected SplittedPath splitPath(String path) {
        return new SplittedPath(path);
    }

    protected String createPathFile(String fileName) {
        return getBaseUrl() + "/" + fileName;
    }

    protected class SplittedPath {
        protected final static String DEFAULT_DELIMITER = "/";

        //contiene l'ultimo valore, può essere file o directory a seconda del path
        private String fileName;
        private String pathDirs;

        public SplittedPath(String filePath) {
            this(filePath, DEFAULT_DELIMITER);
        }

        public SplittedPath(String filePath, String delimiter) {
            int index = filePath.lastIndexOf(delimiter);

            fileName = filePath.substring(index);
            pathDirs = filePath.substring(0, index);
        }

        public String getFileName() {
            return fileName;
        }

        public String getPathDirs() {
            return pathDirs;
        }
    }


//    @Override
//    public List<String> listFiles(String path) throws FileRepositoryException {
//        File file=new File(createPathFile(path));
//        try {
//            if(file.isDirectory())
//                return Arrays.asList(file.list());
//            else
//                throw new FileRepositoryException("The path "+file.getAbsolutePath()+" is not a directory");
//        } catch (FileRepositoryException e){
//            throw e;
//        } catch (Exception e){
//            throw new FileRepositoryException("Exception during request files of dir "+file.getAbsolutePath(),e);
//        }
//    }
}


