package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sostizionemassivaresponsabilearchivi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
public class SostizioneMassivaResponsabileArchiviJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(SostizioneMassivaResponsabileArchiviJobWorkerData.class);

    private Integer[] idsArchivi;
    private Integer idPersonaNuovoResponsabile;
    private Integer idStrutturaNuovoResponsabile;
    private Integer idMassiveActionLog;
    private Integer idPersonaOperazione;
    private Integer idUtenteOperazione;
    private Integer idAzienda;

    public SostizioneMassivaResponsabileArchiviJobWorkerData() {
    }

    public SostizioneMassivaResponsabileArchiviJobWorkerData(Integer[] idsArchivi, Integer idPersonaNuovoResponsabile, Integer idStrutturaNuovoResponsabile, Integer idMassiveActionLog, Integer idPersonaOperazione, Integer idUtenteOperazione, Integer idAzienda) {
        this.idsArchivi = idsArchivi;
        this.idPersonaNuovoResponsabile = idPersonaNuovoResponsabile;
        this.idStrutturaNuovoResponsabile = idStrutturaNuovoResponsabile;
        this.idMassiveActionLog = idMassiveActionLog;
        this.idPersonaOperazione = idPersonaOperazione;
        this.idUtenteOperazione = idUtenteOperazione;
        this.idAzienda = idAzienda;
    }

    public Integer[] getIdsArchivi() {
        return idsArchivi;
    }

    public void setIdsArchivi(Integer[] idsArchivi) {
        this.idsArchivi = idsArchivi;
    }

    public Integer getIdPersonaNuovoResponsabile() {
        return idPersonaNuovoResponsabile;
    }

    public void setIdPersonaNuovoResponsabile(Integer idPersonaNuovoResponsabile) {
        this.idPersonaNuovoResponsabile = idPersonaNuovoResponsabile;
    }

    public Integer getIdStrutturaNuovoResponsabile() {
        return idStrutturaNuovoResponsabile;
    }

    public void setIdStrutturaNuovoResponsabile(Integer idStrutturaNuovoResponsabile) {
        this.idStrutturaNuovoResponsabile = idStrutturaNuovoResponsabile;
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
    
}
