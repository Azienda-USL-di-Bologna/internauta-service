/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.raccolta;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Matteo Next
 */
//@Entity
//@Table(name = "coinvolti", catalog = "argo908", schema = "gd")
//@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
//@Cacheable(false)
//@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id", scope = Coinvolto.class)
public class Coinvolto implements Serializable {

    
//    public static enum Tipologia {
//        FISICA,
//        GIUDIZIARIA 
//    }
    
    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Basic(optional = false)
//    @Column(name = "id")
    private Long id;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "nome")
    private String nome;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "cognome")
    private String cognome; 
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "ragione_sociale")
    private String ragioneSociale;
    
    private String descrizione;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "cf")
    private String cf;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "partitaIva")
    private String partitaIva;
    
    
    
//    @Basic(optional = false)
//    @NotNull
//    @Column(name = "id_contatto_internauta")
    private Long idContattoInternauta;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "tipo")
    private String tipo;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "mail")
    private String mail;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "telefono")
    private String telefono;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "via")
    private String via;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "civico")
    private String civico;
       
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "civico")
    private String cap;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "comune")
    private String comune;
      
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "provincia")
    private String provincia;
       
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 2147483647)
//    @Column(name = "nazione")
    private String nazione;
    
    public void setDescrizione(String d) {
        this.descrizione = d;
    }
    
    public String getDescrizione() {
        return this.descrizione;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getRagioneSociale() {
        return ragioneSociale;
    }

    public void setRagioneSociale(String ragioneSociale) {
        this.ragioneSociale = ragioneSociale;
    }

    public String getCf() {
        return cf;
    }

    public void setCf(String cf) {
        this.cf = cf;
    }

    public String getPartitaIva() {
        return partitaIva;
    }

    public void setPartitaIva(String partitaIva) {
        this.partitaIva = partitaIva;
    }

    public Long getIdContattoInternauta() {
        return idContattoInternauta;
    }

    public void setIdContattoInternauta(Long idContattoInternauta) {
        this.idContattoInternauta = idContattoInternauta;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
            this.tipo = tipo;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getCivico() {
        return civico;
    }

    public void setCivico(String civico) {
        this.civico = civico;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public String getComune() {
        return comune;
    }

    public void setComune(String comune) {
        this.comune = comune;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getNazione() {
        return nazione;
    }

    public void setNazione(String nazione) {
        this.nazione = nazione;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    
    
}
