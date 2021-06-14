package it.bologna.ausl.internauta.service.argo.raccolta;

/**
 *
 * @author Next
 */
public class RaccoltaNew {

    private String idGdDoc;
    private String guidGdDoc;
    private String applicazioneChiamante;
    private String codiceAzienda;
    private String oggetto;
    private String numeroRegistrazione;
    private Integer annoRegistrazione;
    private String idGddocAssociato;
    private String tipoDocumento;
    private String creatore;
    private String additionalData;
    private String descrizioneStruttura;
    private Integer idStrutturaResponsabileInternauta;
    private String idStrutturaResponsabileArgo;

    public RaccoltaNew() {
    }

    public RaccoltaNew(String idGdDoc, String guidGdDoc, String applicazioneChiamante, String oggetto, String tipoDocumento, String creatore, Integer idStrutturaResponsabileInternauta) {
        this.idGdDoc = idGdDoc;
        this.guidGdDoc = guidGdDoc;
        this.applicazioneChiamante = applicazioneChiamante;
        this.oggetto = oggetto;
        this.tipoDocumento = tipoDocumento;
        this.creatore = creatore;
        this.idStrutturaResponsabileInternauta = idStrutturaResponsabileInternauta;
    }

    public String getIdGdDoc() {
        return idGdDoc;
    }

    public void setIdGdDoc(String idGdDoc) {
        this.idGdDoc = idGdDoc;
    }

    public String getGuidGdDoc() {
        return guidGdDoc;
    }

    public void setGuidGdDoc(String guidGdDoc) {
        this.guidGdDoc = guidGdDoc;
    }

    public String getApplicazioneChiamante() {
        return applicazioneChiamante;
    }

    public void setApplicazioneChiamante(String applicazioneChiamante) {
        this.applicazioneChiamante = applicazioneChiamante;
    }

    public String getOggetto() {
        return oggetto;
    }

    public void setOggetto(String oggetto) {
        this.oggetto = oggetto;
    }

    public String getIdGddocAssociato() {
        return idGddocAssociato;
    }

    public void setIdGddocAssociato(String idGddocContenuto) {
        this.idGddocAssociato = idGddocContenuto;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getCreatore() {
        return creatore;
    }

    public void setCreatore(String creatore) {
        this.creatore = creatore;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
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

    public String getNumeroRegistrazione() {
        return numeroRegistrazione;
    }

    public void setNumeroRegistrazione(String numeroRegistrazione) {
        this.numeroRegistrazione = numeroRegistrazione;
    }

    public Integer getAnnoRegistrazione() {
        return annoRegistrazione;
    }

    public void setAnnoRegistrazione(Integer annoRegistrazione) {
        this.annoRegistrazione = annoRegistrazione;
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }

    public String getDescrizioneStruttura() {
        return descrizioneStruttura;
    }

    public void setDescrizioneStruttura(String descrizioneStruttura) {
        this.descrizioneStruttura = descrizioneStruttura;
    }
}
