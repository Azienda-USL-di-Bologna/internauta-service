package it.bologna.ausl.internauta.service.scrivania.anteprima;

/**
 *
 * @author gdm
 */
public class BabelDownloaderRequestBody {
    public static enum Tipologia {FRONTESPIZIO, DOCUMENTO, STAMPA_UNICA, ALLEGATO, ALLEGATO_PU}
    
    private String guid;
    private Tipologia tipologia;
    private String cfUtente;

    public BabelDownloaderRequestBody() {
    }

    public BabelDownloaderRequestBody(String guid, Tipologia tipologia, String cfUtente) {
        this.guid = guid;
        this.tipologia = tipologia;
        this.cfUtente = cfUtente;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Tipologia getTipologia() {
        return tipologia;
    }

    public void setTipologia(Tipologia tipologia) {
        this.tipologia = tipologia;
    }

    public String getCfUtente() {
        return cfUtente;
    }

    public void setCfUtente(String cfUtente) {
        this.cfUtente = cfUtente;
    }

    @Override
    public String toString() {
        return this.guid + "_" + this.tipologia.toString() + "_" + this.cfUtente;
    }
}
