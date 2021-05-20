package it.bologna.ausl.internauta.service.argo.raccolta;

import java.util.Date;

/**
 *
 * @author Matteo Next
 */
public class DocumentoBabel {

    private String guidGddoc;
    private String guidDocumento;
    private String codiceRegistro;
    private String numero;
    private Integer anno;
    private Date dataProtocollo;
    private Date dataDocumento;
    private String oggetto;

    public String getOggetto() {
        return oggetto;
    }

    public void setOggetto(String oggetto) {
        this.oggetto = oggetto;
    }

    public String getGuidDocumento() {
        return guidDocumento;
    }

    public void setGuidDocumento(String guidDocumento) {
        this.guidDocumento = guidDocumento;
    }

    public String getGuidGddoc() {
        return guidGddoc;
    }

    public void setGuidGddoc(String guidGddoc) {
        this.guidGddoc = guidGddoc;
    }

    public String getCodiceRegistro() {
        return codiceRegistro;
    }

    public void setCodiceRegistro(String codiceRegistro) {
        this.codiceRegistro = codiceRegistro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    public String getCodiceBabel() {

        return codiceRegistro + numero + "/" + anno.toString();
    }

    public Date getDataProtocollo() {
        return dataProtocollo;
    }

    public void setDataProtocollo(Date dataProtocollo) {
        this.dataProtocollo = dataProtocollo;
    }

    public Date getDataDocumento() {
        return dataDocumento;
    }

    public void setDataDocumento(Date dataDocumento) {
        this.dataDocumento = dataDocumento;
    }

}
