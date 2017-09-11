package it.nextsw.file.ftp;

import it.nextsw.file.FileRepository;
import it.nextsw.file.exception.FileRepositoryConnectionException;
import it.nextsw.file.exception.FileRepositoryCreateException;
import it.nextsw.file.exception.FileRepositoryException;
import it.nextsw.file.exception.FileRepositoryFileNotFoundException;
import it.nextsw.file.utils.NameGenerationUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanInitializationException;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class FileRepositoryFtp implements FileRepository {

    private static Logger logger=Logger.getLogger(FileRepositoryFtp.class);

    private FTPClient ftpClient;

    public abstract FtpConfig getFtpConfig();

    public FileRepositoryFtp() {
        ftpClient=new FTPClient();
        ftpClient.configure(getFtpConfig().getFtpClientConfig());
    }

    @PostConstruct
    public void init(){

    }

    protected void connect() throws FileRepositoryConnectionException {
        try {
            if (ftpClient.isConnected() && ftpClient.isAvailable()){
                ftpClient.completePendingCommand();
                return;
            }
            ftpClient.connect(getFtpConfig().getHost(),getFtpConfig().getPort());
            //mode active
            //ftpClient.enterLocalActiveMode();
            if (getFtpConfig().isAuthentication()){
                boolean successLogin = ftpClient.login(getFtpConfig().getUsername(),getFtpConfig().getPassword());
                if(!successLogin)
                    throw new FileRepositoryConnectionException("Exception on ftp authentication for "+getFtpConfig().getHost());
            }
            //tipo di file
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            //mi muovo sulla directory
            boolean changeDirectorySuccess=ftpClient.changeWorkingDirectory(getFtpConfig().getWorkingDirectory());
            if(!changeDirectorySuccess)
                throw new FileRepositoryException("Cannot move on directory "+getFtpConfig().getWorkingDirectory());

        } catch (Exception e) {
            throw new FileRepositoryConnectionException("Exception during connect to ftp server "+getFtpConfig(),e);
        }
    }

    protected void disconnect()throws FileRepositoryConnectionException {

            try {
                if (ftpClient.isConnected())
                ftpClient.disconnect();
            } catch (Exception e) {
                throw new FileRepositoryConnectionException("Exception during disconnect to ftp server "+getFtpConfig(),e);
            }
    }


    /**
     * Per l'implementazione su ftp il metodo in questione non ha senso per cui genererà soltanto un nome random
     *
     * @param fileExtension l'estensione del file da creare
     * @return
     * @throws FileRepositoryCreateException
     */
    @Override
    public String createFile(String fileExtension) throws FileRepositoryCreateException {
        String generatedName = NameGenerationUtil.generateName()+ "." + fileExtension;
        return generatedName;
    }

    /**
     * Per l'implementazione su ftp il metodo in questione non ha senso per cui non fa niente
     *
     * @param fileName il nome del file
     * @throws FileRepositoryCreateException
     */
    @Override
    public void createFileWithName(String fileName) throws FileRepositoryCreateException {
        try {
            connect();
            OutputStream outputStream=ftpClient.storeFileStream(fileName);
            IOUtils.closeQuietly(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Crea il file e ritorna l'output stream sul quale scrivere
     *
     * @param fileName
     * @return
     * @throws FileRepositoryFileNotFoundException
     * @throws FileRepositoryException
     */
    @Override
    public OutputStream writeFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException {
        try {
           // disconnect();
            connect();
            return ftpClient.storeFileStream(fileName);
        }catch (Exception e){
            throw new FileRepositoryException("Exception on writeFile "+fileName+" on "+getFtpConfig(),e);
        }
    }

    @Override
    public InputStream readFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException {
        try {
            connect();
            InputStream inputStream=ftpClient.retrieveFileStream(fileName);
            if (inputStream==null)
                throw new FileRepositoryFileNotFoundException("File "+fileName+" may not found on "+getFtpConfig());
            return inputStream;
        } catch (FileRepositoryFileNotFoundException e) {
            throw e;
        }catch (Exception e) {
            throw new FileRepositoryException("Exception on readFile "+fileName+" on "+getFtpConfig(),e);
        }

    }

    @Override
    public void deleteFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException {
        try {
            connect();
            boolean success=ftpClient.deleteFile(fileName);
            if (success)
                return;
            throw new FileRepositoryException("Exception on delete (not success) "+fileName+" on "+getFtpConfig());
        } catch (Exception e) {
            throw new FileRepositoryException("Exception on delete "+fileName+" on "+getFtpConfig(),e);
        }
    }

    @Override
    public void copyFile(String fileName, FileRepository targhetFileRepository) throws FileRepositoryFileNotFoundException, FileRepositoryException {

    }

    @Override
    public void moveFile(String fileName, FileRepository targhetFileRepository) throws FileRepositoryFileNotFoundException, FileRepositoryException {

    }

    @Override
    public List<String> listFiles() throws FileRepositoryException {
        try {
            connect();
            FTPFile[] files=ftpClient.listFiles();
            return Arrays.stream(files).filter(file -> file.isFile()).map(file -> file.getName()).collect(Collectors.toList());
        }catch (Exception e){
            throw new FileRepositoryException("Exception during list file",e);
        }
    }

    @Override
    public List<String> listDirs() throws FileRepositoryException {
        try {
            connect();
            FTPFile[] files=ftpClient.listFiles();
            return Arrays.stream(files).filter(file -> file.isDirectory()).map(file -> file.getName()).collect(Collectors.toList());
        }catch (Exception e){
            throw new FileRepositoryException("Exception during list directory",e);
        }
    }

    @Override
    public void mkDir(String dirName) throws FileRepositoryException {
        try {
            connect();
            boolean success=ftpClient.makeDirectory(dirName);
            if (success)
                return;
            throw new FileRepositoryException("Exception on mkdir (not success) "+dirName+" on "+getFtpConfig());
        } catch (Exception e) {
            throw new FileRepositoryException("Exception on mkdir "+dirName+" on "+getFtpConfig(),e);

        }
    }

    @Override
    public FileRepository exploreDir(String dirName) throws FileRepositoryException {
        try {
            FtpConfig ftpConfig = getFtpConfig().clone();
            ftpConfig.setWorkingDirectory(ftpConfig.getWorkingDirectory() + dirName + "/");
            return new FileRepositoryFtp() {
                @Override
                public FtpConfig getFtpConfig() {
                    return ftpConfig;
                }
            };
        } catch (Exception e) {
            throw new FileRepositoryException("Exception on exploreDir "+dirName+" on "+getFtpConfig(),e);

        }
    }

    @Override
    public boolean isFile(String fileName) throws FileRepositoryFileNotFoundException, FileRepositoryException {
        return listFiles().stream().anyMatch(file -> file.equals(fileName));
    }

    @Override
    public boolean isDir(String dirName) throws FileRepositoryFileNotFoundException, FileRepositoryException {
        return listDirs().stream().anyMatch(dir -> dir.equals(dirName));
    }

    @Override
    public boolean isFileExist(String fileName) throws FileRepositoryException {
        try {
            InputStream inputStream=ftpClient.retrieveFileStream(fileName);
            if (inputStream==null)
                return false;
            IOUtils.closeQuietly(inputStream);
            return true;
        } catch (Exception e){
            throw new FileRepositoryException("Exception on isFileExist "+fileName+" on "+getFtpConfig(),e);

        }

    }
}
