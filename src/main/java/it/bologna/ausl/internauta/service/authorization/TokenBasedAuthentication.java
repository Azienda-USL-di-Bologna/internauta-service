package it.bologna.ausl.internauta.service.authorization;

import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione.Applicazioni;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public class TokenBasedAuthentication extends AbstractAuthenticationToken {

    private String token;
    private final UserDetails user;
    private final UserDetails realUser;
    private Integer idSessionLog;
    private Applicazioni applicazione;

    public TokenBasedAuthentication(Utente user, Applicazioni applicazione) {
        super(user.getAuthorities());
        this.user = user;
        this.realUser = null;
        this.applicazione = applicazione;
//        super.setDetails(userInfo);
    }

    public TokenBasedAuthentication(Utente user, Utente realUser, Applicazioni applicazione) {
        super(user.getAuthorities());
        this.user = user;
        this.realUser = realUser;
        this.applicazione = applicazione;
//        super.setDetails(userInfo);
    }

    public TokenBasedAuthentication(Utente user, int idSessionLog, Applicazioni applicazione) {
        super(user.getAuthorities());
        this.user = user;
        this.realUser = this.user;
        this.idSessionLog = idSessionLog;
        this.applicazione = applicazione;
    }

    public TokenBasedAuthentication(Utente user, Utente realUser, int idSessionLog, Applicazioni applicazione) {
        super(user.getAuthorities());
        this.user = user;
        this.realUser = realUser;
        this.idSessionLog = idSessionLog;
        this.applicazione = applicazione;
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

    public Applicazioni getApplicazione() {
        return applicazione;
    }

    public void setApplicazione(Applicazioni applicazione) {
        this.applicazione = applicazione;
    }
}
