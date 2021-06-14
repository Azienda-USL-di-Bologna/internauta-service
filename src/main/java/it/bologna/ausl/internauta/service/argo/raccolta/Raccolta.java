package it.bologna.ausl.internauta.service.argo.raccolta;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Matteo Next
 */
public class Raccolta {

    private Long id;
    private String idGddocAssociato;
    private String idGddoc;
    private String codice;
    private String applicazioneChiamante;
    private String additionalData;
    private String creatore;
    private Integer idStrutturaResponsabileInternauta;
    private String idStrutturaResponsabileArgo;
    private String descrizioneStruttura;
    private String stato;
    private String storico;
    private String tipoDocumento;
    private String oggetto;
    private String fascicoli;
    private String documentoBabel;
    private List<Coinvolto> coinvolti;
    private List<Sottodocumento> sottodocumenti;
    private Date createTime;

    public List<Sottodocumento> getSottodocumenti() {
        return sottodocumenti;
    }

    public void setSottodocumenti(List<Sottodocumento> sottodocumenti) {
        this.sottodocumenti = sottodocumenti;
    }

    public List<Coinvolto> getCoinvolti() {
        return coinvolti;
    }

    public void addSottodocumento(Sottodocumento s) {
        if (this.sottodocumenti == null) {
            this.sottodocumenti = new ArrayList<>();
        }
        this.sottodocumenti.add(s);
    }

    public void setCoinvolti(List<Coinvolto> coinvolti) {
        this.coinvolti = coinvolti;
    }

    public String getFascicoli() {
        return fascicoli;
    }

    public void addCoinvolto(Coinvolto e) {
        if (this.coinvolti == null) {
            this.coinvolti = new ArrayList<>();
        }
        this.coinvolti.add(e);
    }

    public void setFascicoli(String fascicoli) {
        this.fascicoli = fascicoli;
    }

    public String getDocumentoBabel() {
        return documentoBabel;
    }

    public void setDocumentoBabel(String documentoBabel) {
        this.documentoBabel = documentoBabel;
    }

    public String getIdGddocAssociato() {
        return idGddocAssociato;
    }

    public void setIdGddocAssociato(String idGddocAssociato) {
        this.idGddocAssociato = idGddocAssociato;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getIdGddoc() {
        return idGddoc;
    }

    public void setIdGddoc(String idGddoc) {
        this.idGddoc = idGddoc;
    }

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getApplicazioneChiamante() {
        return applicazioneChiamante;
    }

    public void setApplicazioneChiamante(String applicazioneChiamante) {
        this.applicazioneChiamante = applicazioneChiamante;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
    }

    public String getCreatore() {
        return creatore;
    }

    public void setCreatore(String creatore) {
        this.creatore = creatore;
    }

    public Integer getIdStrutturaResponsabileInternauta() {
        return idStrutturaResponsabileInternauta;
    }

    public void setIdStrutturaResponsabileInternauta(Integer idStrutturaResponsabileInternauta) {
        this.idStrutturaResponsabileInternauta = idStrutturaResponsabileInternauta;
    }

    public String getIdStrutturaResponsabileArgo() {
        return idStrutturaResponsabileArgo;
    }

    public void setIdStrutturaResponsabileArgo(String idStrutturaResponsabileArgo) {
        this.idStrutturaResponsabileArgo = idStrutturaResponsabileArgo;
    }

    public String getDescrizioneStruttura() {
        return descrizioneStruttura;
    }

    public void setDescrizioneStruttura(String descrizioneStruttura) {
        this.descrizioneStruttura = descrizioneStruttura;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public String getStorico() {
        return storico;
    }

    public void setStorico(String storico) {
        this.storico = storico;
    }

    public static enum Stato {
        ATTIVO,
        ANNULLATO
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOggetto() {
        return oggetto;
    }

    public void setOggetto(String oggetto) {
        this.oggetto = oggetto;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
}
