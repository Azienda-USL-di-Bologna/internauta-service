package it.nextsw.security.login.bean;

/**
 * Created by f.longhitano on 14/07/2017.
 */
public class LoginRequestDTO {

    private String username;
    private String password;

    /**
     * Se true il token di sessione ha durata maggiore
     */
    private boolean remainConnected;

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

    public boolean isRemainConnected() {
        return remainConnected;
    }

    public void setRemainConnected(boolean remainConnected) {
        this.remainConnected = remainConnected;
    }
}
