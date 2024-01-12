package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.copiatrasferisciabilitazioniarchivi;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.model.entities.logs.MassiveActionLog;

/**
 *
 * @author gusgus
 */
public class CopiaTrasferisciAbilitazioniArchiviJobWorkerData extends JobWorkerData {
    private MassiveActionLog.OperationType operationType;
    private Integer idPersonaSorgente;
    private Integer idPersonaDestinazione;
    private Integer idStrutturaDestinazione;
    private Integer idMassiveActionLog;
    private Integer idPersonaOperazione;
    private Integer idUtenteOperazione;
    private Integer idAzienda;
    private Integer idUtenteDestinazione;

    public CopiaTrasferisciAbilitazioniArchiviJobWorkerData() {
    }

    public CopiaTrasferisciAbilitazioniArchiviJobWorkerData(MassiveActionLog.OperationType operationType, Integer idPersonaSorgente, Integer idPersonaDestinazione, Integer idStrutturaDestinazione, Integer idMassiveActionLog, Integer idPersonaOperazione, Integer idUtenteOperazione, Integer idAzienda, Integer idUtenteDestinazione) {
        this.operationType = operationType;
        this.idPersonaSorgente = idPersonaSorgente;
        this.idPersonaDestinazione = idPersonaDestinazione;
        this.idStrutturaDestinazione = idStrutturaDestinazione;
        this.idMassiveActionLog = idMassiveActionLog;
        this.idPersonaOperazione = idPersonaOperazione;
        this.idUtenteOperazione = idUtenteOperazione;
        this.idAzienda = idAzienda;
        this.idUtenteDestinazione = idUtenteDestinazione;
    }

    public MassiveActionLog.OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(MassiveActionLog.OperationType operationType) {
        this.operationType = operationType;
    }

    public Integer getIdPersonaSorgente() {
        return idPersonaSorgente;
    }

    public void setIdPersonaSorgente(Integer idPersonaSorgente) {
        this.idPersonaSorgente = idPersonaSorgente;
    }

    public Integer getIdPersonaDestinazione() {
        return idPersonaDestinazione;
    }

    public void setIdPersonaDestinazione(Integer idPersonaDestinazione) {
        this.idPersonaDestinazione = idPersonaDestinazione;
    }

    public Integer getIdStrutturaDestinazione() {
        return idStrutturaDestinazione;
    }

    public void setIdStrutturaDestinazione(Integer idStrutturaDestinazione) {
        this.idStrutturaDestinazione = idStrutturaDestinazione;
    }

    public Integer getIdMassiveActionLog() {
        return idMassiveActionLog;
    }

    public void setIdMassiveActionLog(Integer idMassiveActionLog) {
        this.idMassiveActionLog = idMassiveActionLog;
    }

    public Integer getIdPersonaOperazione() {
        return idPersonaOperazione;
    }

    public void setIdPersonaOperazione(Integer idPersonaOperazione) {
        this.idPersonaOperazione = idPersonaOperazione;
    }

    public Integer getIdUtenteOperazione() {
        return idUtenteOperazione;
    }

    public void setIdUtenteOperazione(Integer idUtenteOperazione) {
        this.idUtenteOperazione = idUtenteOperazione;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public Integer getIdUtenteDestinazione() {
        return idUtenteDestinazione;
    }

    public void setIdUtenteDestinazione(Integer idUtenteDestinazione) {
        this.idUtenteDestinazione = idUtenteDestinazione;
    }
    
}
