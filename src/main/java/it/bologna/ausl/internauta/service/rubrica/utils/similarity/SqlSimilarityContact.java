/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.rubrica.utils.similarity;

/**
 *
 * @author Top
 */
public class SqlSimilarityContact {

    private Integer id;
    private String nome;
    private String cognome;
    private String categoria;
    private String descrizione;
    private String partita_iva;
    private String codice_fiscale;
    private String email;
    private String descrizione_indirizzo;
    private boolean riservato;

    public SqlSimilarityContact() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getPartita_iva() {
        return partita_iva;
    }

    public void setPartita_iva(String partita_iva) {
        this.partita_iva = partita_iva;
    }

    public String getCodice_fiscale() {
        return codice_fiscale;
    }

    public void setCodice_fiscale(String codice_fiscale) {
        this.codice_fiscale = codice_fiscale;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescrizione_indirizzo() {
        return descrizione_indirizzo;
    }

    public boolean isRiservato() {
        return riservato;
    }

    public void setRiservato(boolean riservato) {
        this.riservato = riservato;
    }

    public void setDescrizione_indirizzo(String descrizione_indirizzo) {
        this.descrizione_indirizzo = descrizione_indirizzo;
    }

}
