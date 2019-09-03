package it.bologna.ausl.internauta.service.krint;

import it.bologna.ausl.model.entities.logs.OperazioneKrint.CodiceOperazione;

/**
 *
 * @author gusgus & guiduzzo
 */
public class KrintError {
    private Integer idUtente;
    private Integer idRealUser;
    private Integer idOggetto;
    private String functionName;
    private CodiceOperazione codiceOperazione;

    public Integer getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(Integer idUtente) {
        this.idUtente = idUtente;
    }

    public Integer getIdRealUser() {
        return idRealUser;
    }

    public void setIdRealUser(Integer idRealUser) {
        this.idRealUser = idRealUser;
    }

    public Integer getIdOggetto() {
        return idOggetto;
    }

    public void setIdOggetto(Integer idOggetto) {
        this.idOggetto = idOggetto;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public CodiceOperazione getCodiceOperazione() {
        return codiceOperazione;
    }

    public void setCodiceOperazione(CodiceOperazione codiceOperazione) {
        this.codiceOperazione = codiceOperazione;
    }
    
}
