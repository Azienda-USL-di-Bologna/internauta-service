/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.rubrica.utils.similarity;

import java.util.List;

/**
 *
 * @author Top
 */
public class SqlSimilarityResults {

    private List<SqlSimilarityResult> emailList;
    private List<SqlSimilarityResult> indirizzo;
    private List<SqlSimilarityResult> codiceFiscale;
    private List<SqlSimilarityResult> cognomeAndNome;
    private List<SqlSimilarityResult> partitaIva;

    public SqlSimilarityResults() {
    }

    public List<SqlSimilarityResult> getEmailList() {
        return emailList;
    }

    public void setEmailList(List<SqlSimilarityResult> emailList) {
        this.emailList = emailList;
    }

    public List<SqlSimilarityResult> getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(List<SqlSimilarityResult> indirizzo) {
        this.indirizzo = indirizzo;
    }

    public List<SqlSimilarityResult> getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(List<SqlSimilarityResult> codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public List<SqlSimilarityResult> getCognomeAndNome() {
        return cognomeAndNome;
    }

    public void setCognomeAndNome(List<SqlSimilarityResult> cognomeAndNome) {
        this.cognomeAndNome = cognomeAndNome;
    }

    public List<SqlSimilarityResult> getPartitaIva() {
        return partitaIva;
    }

    public void setPartitaIva(List<SqlSimilarityResult> partitaIva) {
        this.partitaIva = partitaIva;
    }

}
