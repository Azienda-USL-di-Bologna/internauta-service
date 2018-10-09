package it.bologna.ausl.internauta.service.authorization;

import it.bologna.ausl.model.entities.baborg.Utente;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public class TokenBasedAuthentication extends AbstractAuthenticationToken {

    private String token;
    private final UserDetails user;
    private final UserDetails realUser;
    private Integer idSessionLog;

    public TokenBasedAuthentication(Utente user) {
        super(user.getAuthorities());
        this.user = user;
        this.realUser = user;
//        super.setDetails(userInfo);
    }

    public TokenBasedAuthentication(Utente user, Utente realUser) {
        super(user.getAuthorities());
        this.user = user;
        this.realUser = realUser;
//        super.setDetails(userInfo);
    }

    public TokenBasedAuthentication(Utente user, int idSessionLog) {
        super(user.getAuthorities());
        this.user = user;
        this.realUser = this.user;
        this.idSessionLog = idSessionLog;
    }

    public TokenBasedAuthentication(Utente user, Utente realUser, int idSessionLog) {
        super(user.getAuthorities());
        this.user = user;
        this.realUser = realUser;
        this.idSessionLog = idSessionLog;
//        super.setDetails(userInfo);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public UserDetails getPrincipal() {
        return user;
    }

    public UserDetails getRealUser() {
        return realUser;
    }

    public Integer getIdSessionLog() {
        return idSessionLog;
    }

    public void setIdSessionLog(Integer idSessionLog) {
        this.idSessionLog = idSessionLog;
    }
}
