/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.raccolta;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import java.io.Serializable;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author Matteo Next
 */

//@Table(name = "raccolte", catalog = "argo908", schema = "gd")
//@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
//@Cacheable(false)
//@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id", scope = Raccolta.class)
public class Raccolta implements Serializable {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Basic(optional = false)
//    @Column(name = "id")
    private Long id;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "id_gddoc")
    private String idGddoc;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "codice")
    private String codice;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "applicazione_chiamante")
    private String applicazioneChiamante;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "additional_data")
    private String additionalData;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "creatore")
    private String creatore;
    
//    @Basic(optional = false)
//    @NotNull
//    @Column(name = "id_struttura_responsabile_internauta")
    private Integer idStrutturaResponsabileInternauta;
   
//    @Basic(optional = false)
//    @NotNull
//    @Column(name = "id_struttura_responsabile_argo")
    private Integer idStrutturaResponsabileArgo;
         
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "descrizione_struttura")
    private String descrizioneStruttura;
         
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "stato")
    private String stato;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "storico")
    private String storico;
    
    private String tipoDocumento;
    
    private String oggetto;

    private String fascicoli;
    
    private String documentoBabel;
    
    private List<Coinvolto> coinvolti = new ArrayList<>();
    
    private List<Sottodocumento> sottodocumenti = new ArrayList<>();

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
        this.sottodocumenti.add(s);
    }

    public void setCoinvolti(List<Coinvolto> coinvolti) {
        this.coinvolti = coinvolti;
    }

    public String getFascicoli() {
        return fascicoli;
    }
    
    public void addCoinvolto(Coinvolto e) {
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
    
//    @Version()
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private ZonedDateTime version;
    
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private Date createTime;
    
//    @Size(max = 2147483647)
//    @Column(name = "tscol", columnDefinition = "tsvector")
//    private String tscol;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
//    public String getTscol() {
//        return tscol;
//    }

//    public void setTscol(String tscol) {
//        this.tscol = tscol;
//    }
    
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

    public int getIdStrutturaResponsabileInternauta() {
        return idStrutturaResponsabileInternauta;
    }

    public void setIdStrutturaResponsabileInternauta(int idStrutturaResponsabileInternauta) {
        this.idStrutturaResponsabileInternauta = idStrutturaResponsabileInternauta;
    }

    public int getIdStrutturaResponsabileArgo() {
        return idStrutturaResponsabileArgo;
    }

    public void setIdStrutturaResponsabileArgo(int idStrutturaResponsabileArgo) {
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

//    public ZonedDateTime getVersion() {
//        return version;
//    }

//    public void setVersion(ZonedDateTime version) {
//        this.version = version;
//    }
    
    
    public static enum Stato{ 
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
