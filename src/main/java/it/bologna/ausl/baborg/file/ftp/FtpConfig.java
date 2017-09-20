package it.bologna.ausl.baborg.file.ftp;

import org.apache.commons.net.ftp.FTPClientConfig;

public class FtpConfig implements Cloneable{

    public static final int DEFAULT_PORT=21;
    public static final String DEFAULT_WORKING_DIRECTORY="/";

    private FTPClientConfig ftpClientConfig;
    private String host;
    private Integer port;
    private boolean authentication;
    private String username;
    private String password;

    private String workingDirectory;

    public FtpConfig() {
        port=DEFAULT_PORT;
        workingDirectory=DEFAULT_WORKING_DIRECTORY;
    }

    public FTPClientConfig getFtpClientConfig() {
        return ftpClientConfig;
    }

    public void setFtpClientConfig(FTPClientConfig ftpClientConfig) {
        this.ftpClientConfig = ftpClientConfig;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public boolean isAuthentication() {
        return authentication;
    }

    public void setAuthentication(boolean authentication) {
        this.authentication = authentication;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String toString() {
        return getHost()+":"+getPort()+getWorkingDirectory();
    }

    public FtpConfig clone(){
        try {
            return (FtpConfig) super.clone();
        } catch (CloneNotSupportedException e){
            throw new RuntimeException("Exception on clone FtpConfig",e);
        }
    }
}
