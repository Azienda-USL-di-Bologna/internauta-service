package it.bologna.ausl.internauta.service.argo.raccolta;

/**
 *
 * @author Matteo Next
 */
public class Fascicolo {

    private String guidFascicolo;
    private String idLivelloFascicolo;
    private String numerazioneGerarchica;
    private String nomeFascicoloInterfaccia;
    private String numeroFascicolo;
    private String nomeFascicolo;
    private Integer annoFascicolo;
    private String idUtenteCreazione;
    private String idUtenteResponsabile;
    private String titolo;

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getNomeFascicoloInterfaccia() {
        return nomeFascicoloInterfaccia;
    }

    public void setNomeFascicoloInterfaccia(String nomeFascicoloInterfaccia) {
        this.nomeFascicoloInterfaccia = nomeFascicoloInterfaccia;
    }

    public String getNumerazioneGerarchica() {
        return numerazioneGerarchica;
    }

    public void setNumerazioneGerarchica(String numerazioneGerarchica) {
        this.numerazioneGerarchica = numerazioneGerarchica;
    }

    public String getGuidFascicolo() {
        return guidFascicolo;
    }

    public void setGuidFascicolo(String guidFascicolo) {
        this.guidFascicolo = guidFascicolo;
    }

    public String getIdLivelloFascicolo() {
        return idLivelloFascicolo;
    }

    public void setIdLivelloFascicolo(String idLivelloFascicolo) {
        this.idLivelloFascicolo = idLivelloFascicolo;
    }

    public String getNumeroFascicolo() {
        return numeroFascicolo;
    }

    public void setNumeroFascicolo(String numeroFascicolo) {
        this.numeroFascicolo = numeroFascicolo;
    }

    public String getNomeFascicolo() {
        return nomeFascicolo;
    }

    public void setNomeFascicolo(String nomeFascicolo) {
        this.nomeFascicolo = nomeFascicolo;
    }

    public Integer getAnnoFascicolo() {
        return annoFascicolo;
    }

    public void setAnnoFascicolo(Integer annoFascicolo) {
        this.annoFascicolo = annoFascicolo;
    }

    public String getIdUtenteCreazione() {
        return idUtenteCreazione;
    }

    public void setIdUtenteCreazione(String idUtenteCreazione) {
        this.idUtenteCreazione = idUtenteCreazione;
    }

    public String getIdUtenteResponsabile() {
        return idUtenteResponsabile;
    }

    public void setIdUtenteResponsabile(String idUtenteResponsabile) {
        this.idUtenteResponsabile = idUtenteResponsabile;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Fascicolo) {
            return (((Fascicolo) obj).getNumerazioneGerarchica() == null ? this.numerazioneGerarchica == null : ((Fascicolo) obj).getNumerazioneGerarchica().equals(this.numerazioneGerarchica));
        }
        return false;
    }
}
