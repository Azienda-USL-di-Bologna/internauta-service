/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.raccolta;


import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Matteo Next
 */

//@Table(name = "raccolte", catalog = "argo908", schema = "gd")
//@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
//@Cacheable(false)
//@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id", scope = Raccolta.class)
public class Raccolta  {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Basic(optional = false)
//    @Column(name = "id")
    private Long id;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "id_gddoc")
    private String idGddocAssociato;
    
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
    private String idStrutturaResponsabileArgo;
         
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
    
    private List<Coinvolto> coinvolti;
    
    private List<Sottodocumento> sottodocumenti;

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
        if(this.sottodocumenti == null)
            this.sottodocumenti = new ArrayList<>();
        this.sottodocumenti.add(s);
    }

    public void setCoinvolti(List<Coinvolto> coinvolti) {
        this.coinvolti = coinvolti;
    }

    public String getFascicoli() {
        return fascicoli;
    }
    
    public void addCoinvolto(Coinvolto e) {
        if(this.coinvolti == null)
            this.coinvolti = new ArrayList<>();
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
