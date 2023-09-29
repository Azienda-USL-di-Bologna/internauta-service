package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.gestionemassivaabilitazioniarchivi;

import java.util.List;

/**
 *
 * @author gusgus
 */
public class InfoPersona {
    private String descrizione;
    private List<Integer> vicariatiOttenuti; // E' un elenco di id archivi
    private List<Integer> vicariatiPerduti; // E' un elenco di id archivi
    private String predicatoPermessoOttenuto; // TODO: Non so se serve salvarsi questo dato
    private List<Integer> permessiOttenuti; // E' un elenco di id archivi
    private List<Integer> permessiPerduti; // E' un elenco di id archivi

    public InfoPersona(String descrizione, List<Integer> vicariatiOttenuti, List<Integer> vicariatiPerduti, String predicatoPermessoOttenuto, List<Integer> permessiOttenuti, List<Integer> permessiPerduti) {
        this.descrizione = descrizione;
        this.vicariatiOttenuti = vicariatiOttenuti;
        this.vicariatiPerduti = vicariatiPerduti;
        this.predicatoPermessoOttenuto = predicatoPermessoOttenuto;
        this.permessiOttenuti = permessiOttenuti;
        this.permessiPerduti = permessiPerduti;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public List<Integer> getVicariatiOttenuti() {
        return vicariatiOttenuti;
    }

    public void setVicariatiOttenuti(List<Integer> vicariatiOttenuti) {
        this.vicariatiOttenuti = vicariatiOttenuti;
    }

    public List<Integer> getVicariatiPerduti() {
        return vicariatiPerduti;
    }

    public void setVicariatiPerduti(List<Integer> vicariatiPerduti) {
        this.vicariatiPerduti = vicariatiPerduti;
    }

    public String getPredicatoPermessoOttenuto() {
        return predicatoPermessoOttenuto;
    }

    public void setPredicatoPermessoOttenuto(String predicatoPermessoOttenuto) {
        this.predicatoPermessoOttenuto = predicatoPermessoOttenuto;
    }

    public List<Integer> getPermessiOttenuti() {
        return permessiOttenuti;
    }

    public void setPermessiOttenuti(List<Integer> permessiOttenuti) {
        this.permessiOttenuti = permessiOttenuti;
    }

    public List<Integer> getPermessiPerduti() {
        return permessiPerduti;
    }

    public void setPermessiPerduti(List<Integer> permessiPerduti) {
        this.permessiPerduti = permessiPerduti;
    }
   
}
