package it.bologna.ausl.baborg.file.impl;

import it.bologna.ausl.baborg.file.ftp.FileRepositoryFtp;
import it.bologna.ausl.baborg.file.ftp.FtpConfig;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.springframework.stereotype.Service;


@Service
public class FileRepositoryFtpImpl extends FileRepositoryFtp {

    public FtpConfig getFtpConfig(){
        FtpConfig ftpConfig=new FtpConfig();
        FTPClientConfig ftpClientConfig= new FTPClientConfig();
        ftpConfig.setHost("localhost");
        ftpConfig.setPort(21);
        ftpConfig.setAuthentication(true);
        ftpConfig.setUsername("prova");
        ftpConfig.setPassword("prova");

        return ftpConfig;
    }
}
