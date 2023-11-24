package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.copiatrasferisciabilitazioniarchivi;

import java.util.List;

/**
 *
 * @author gusgus
 */
public class InfoArchivio {
    private String numerazioneGerarchica;
    private List<String> abilitazioniAggiunte; // [VISUALIZZA, MODIFICA, ELIMINA, RESPONSABILE, VICARIO}
    private Integer idArchivioRadice;

    public InfoArchivio(String numerazioneGerarchica, List<String> abilitazioniAggiunte, Integer idArchivioRadice) {
        this.numerazioneGerarchica = numerazioneGerarchica;
        this.abilitazioniAggiunte = abilitazioniAggiunte;
        this.idArchivioRadice = idArchivioRadice;
    }

    public String getNumerazioneGerarchica() {
        return numerazioneGerarchica;
    }

    public void setNumerazioneGerarchica(String numerazioneGerarchica) {
        this.numerazioneGerarchica = numerazioneGerarchica;
    }

    public List<String> getAbilitazioniAggiunte() {
        return abilitazioniAggiunte;
    }

    public void setAbilitazioniAggiunte(List<String> abilitazioniAggiunte) {
        this.abilitazioniAggiunte = abilitazioniAggiunte;
    }

    public Integer getIdArchivioRadice() {
        return idArchivioRadice;
    }

    public void setIdArchivioRadice(Integer idArchivioRadice) {
        this.idArchivioRadice = idArchivioRadice;
    }

}
