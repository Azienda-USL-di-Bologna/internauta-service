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
public class SqlSimilarityResult {

    private String field;
    private SqlSimilarityContact contact;
    private Double similarity;
    private Double distance_lev;
    private Integer idContact;
    private String foundValue;

    public SqlSimilarityResult() {
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public SqlSimilarityContact getContact() {
        return contact;
    }

    public void setContact(SqlSimilarityContact contact) {
        this.contact = contact;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public Double getDistance_lev() {
        return distance_lev;
    }

    public void setDistance_lev(Double distance_lev) {
        this.distance_lev = distance_lev;
    }

    public Integer getIdContact() {
        return idContact;
    }

    public void setIdContact(Integer idContact) {
        this.idContact = idContact;
    }

    public String getFoundValue() {
        return foundValue;
    }

    public void setFoundValue(String foundValue) {
        this.foundValue = foundValue;
    }

}
