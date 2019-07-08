package it.bologna.ausl.internauta.service.authorization;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;

/**
 *
 * @author gdm
 */
public class AuthenticatedSessionData {
    private Utente user, realUser;
    private Persona person, realPerson;
    private Applicazione.Applicazioni applicazione;
    private int idSessionLog;

    public AuthenticatedSessionData() {
    }

    public AuthenticatedSessionData(Utente user, Utente realUser, Persona person, Persona realPerson, int idSessionLog, Applicazione.Applicazioni applicazione) {
        this.user = user;
        this.realUser = realUser;
        this.person = person;
        this.realPerson = realPerson;
        this.idSessionLog = idSessionLog;
        this.applicazione = applicazione;
    }

    public Utente getUser() {
        return user;
    }

    public void setUser(Utente user) {
        this.user = user;
    }

    public Utente getRealUser() {
        return realUser;
    }

    public void setRealUser(Utente realUser) {
        this.realUser = realUser;
    }

    public Persona getPerson() {
        return person;
    }

    public void setPerson(Persona person) {
        this.person = person;
    }

    public Persona getRealPerson() {
        return realPerson;
    }

    public void setRealPerson(Persona realPerson) {
        this.realPerson = realPerson;
    }

    public int getIdSessionLog() {
        return idSessionLog;
    }

    public void setIdSessionLog(int idSessionLog) {
        this.idSessionLog = idSessionLog;
    }

    public Applicazione.Applicazioni getApplicazione() {
        return applicazione;
    }

    public void setApplicazione(Applicazione.Applicazioni applicazione) {
        this.applicazione = applicazione;
    }
}
