package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.gestionemassivaabilitazioniarchivi;

import java.util.List;
import java.util.Map;

/**
 *
 * @author gusgus
 */
public class InfoArchivio {
    private String numerazioneGerarchica;
    private List<Integer> vicariAggiunti; 
    private List<Integer> vicariEliminati;
    private Map<String, List<Integer>> permessiPersonaAggiunti;   // {VISUALIZZA: [], MODIFICA: [], ELIMINA: []}
    private List<Integer> permessiPersonaRimossi;

    public InfoArchivio(String numerazioneGerarchica, List<Integer> vicariAggiunti, List<Integer> vicariEliminati, Map<String, List<Integer>> permessiPersonaAggiunti, List<Integer> permessiPersonaRimossi) {
        this.numerazioneGerarchica = numerazioneGerarchica;
        this.vicariAggiunti = vicariAggiunti;
        this.vicariEliminati = vicariEliminati;
        this.permessiPersonaAggiunti = permessiPersonaAggiunti;
        this.permessiPersonaRimossi = permessiPersonaRimossi;
    }

    public String getNumerazioneGerarchica() {
        return numerazioneGerarchica;
    }

    public void setNumerazioneGerarchica(String numerazioneGerarchica) {
        this.numerazioneGerarchica = numerazioneGerarchica;
    }

    public List<Integer> getVicariAggiunti() {
        return vicariAggiunti;
    }

    public void setVicariAggiunti(List<Integer> vicariAggiunti) {
        this.vicariAggiunti = vicariAggiunti;
    }

    public List<Integer> getVicariEliminati() {
        return vicariEliminati;
    }

    public void setVicariEliminati(List<Integer> vicariEliminati) {
        this.vicariEliminati = vicariEliminati;
    }

    public Map<String, List<Integer>> getPermessiPersonaAggiunti() {
        return permessiPersonaAggiunti;
    }

    public void setPermessiPersonaAggiunti(Map<String, List<Integer>> permessiPersonaAggiunti) {
        this.permessiPersonaAggiunti = permessiPersonaAggiunti;
    }

    public List<Integer> getPermessiPersonaRimossi() {
        return permessiPersonaRimossi;
    }

    public void setPermessiPersonaRimossi(List<Integer> permessiPersonaRimossi) {
        this.permessiPersonaRimossi = permessiPersonaRimossi;
    }
   
}
