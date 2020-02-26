package it.bologna.ausl.internauta.service.rubrica.utils.similarity;

import java.util.List;

/**
 *
 * @author Top
 */
public class SqlSimilarityResults {

    private List<SqlSimilarityResult> emailList;
    // private List<SqlSimilarityResult> indirizzo;
    private List<SqlSimilarityResult> codiceFiscale;
    private List<SqlSimilarityResult> cognomeAndNome;
    private List<SqlSimilarityResult> partitaIva;
    private List<SqlSimilarityResult> ragioneSociale;

    public SqlSimilarityResults() {
    }

    public List<SqlSimilarityResult> getEmailList() {
        return emailList;
    }

    public void setEmailList(List<SqlSimilarityResult> emailList) {
        this.emailList = emailList;
    }

    public List<SqlSimilarityResult> getRagioneSociale() {
        return ragioneSociale;
    }

    public void setRagioneSociale(List<SqlSimilarityResult> ragioneSociale) {
        this.ragioneSociale = ragioneSociale;
    }

//    public List<SqlSimilarityResult> getIndirizzo() {
//        return indirizzo;
//    }
//
//    public void setIndirizzo(List<SqlSimilarityResult> indirizzo) {
//        this.indirizzo = indirizzo;
//    }

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
