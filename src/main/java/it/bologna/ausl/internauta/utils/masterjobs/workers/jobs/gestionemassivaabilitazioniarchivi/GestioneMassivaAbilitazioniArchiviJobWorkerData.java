package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.gestionemassivaabilitazioniarchivi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.service.controllers.scripta.InfoAbilitazioniMassiveArchivi;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
public class GestioneMassivaAbilitazioniArchiviJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(GestioneMassivaAbilitazioniArchiviJobWorkerData.class);

    private Integer[] idsArchivi;
    private InfoAbilitazioniMassiveArchivi abilitazioniRichieste;
    private Integer idMassiveActionLog;
    private Integer idPersonaOperazione;
    private Integer idUtenteOperazione;
    private Integer idAzienda;

    public GestioneMassivaAbilitazioniArchiviJobWorkerData() {
    }

    public GestioneMassivaAbilitazioniArchiviJobWorkerData(Integer[] idsArchivi, InfoAbilitazioniMassiveArchivi abilitazioniRichieste, Integer idMassiveActionLog, Integer idPersonaOperazione, Integer idUtenteOperazione, Integer idAzienda) {
        this.idsArchivi = idsArchivi;
        this.abilitazioniRichieste = abilitazioniRichieste;
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

    public InfoAbilitazioniMassiveArchivi getAbilitazioniRichieste() {
        return abilitazioniRichieste;
    }

    public void setAbilitazioniRichieste(InfoAbilitazioniMassiveArchivi abilitazioniRichieste) {
        this.abilitazioniRichieste = abilitazioniRichieste;
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
