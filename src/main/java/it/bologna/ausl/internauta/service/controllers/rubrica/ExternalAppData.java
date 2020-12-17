package it.bologna.ausl.internauta.service.controllers.rubrica;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.annotations.SerializedName;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import java.util.List;

/**
 *
 * @author gusgus
 */
public class ExternalAppData {

    private String mode;
    private String app; //todo fare enum
    private String guid;
    private String codiceAzienda;
    private String selectedContactsLists;
    private String cfUtenteOperazione;
    
    private List<Contatto> estemporaneiToAddToRubrica;
    private String glogParams;
//    private Map<String, List<SelectedContact>> selectedContactsLists;

    public String getMode() {
        return mode;
    }

    public List<Contatto> getEstemporaneiToAddToRubrica() {
        return estemporaneiToAddToRubrica;
    }

    public void setEstemporaneiToAddToRubrica(List<Contatto> estemporaneiToAddToRubrica) {
        this.estemporaneiToAddToRubrica = estemporaneiToAddToRubrica;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getSelectedContactsLists() {
        return selectedContactsLists;
    }

//    public Map<String, List<SelectedContact>> getSelectedContactsLists() {
//        return selectedContactsLists;
//    }
//
//    public void setSelectedContactsLists(Map<String, List<SelectedContact>> selectedContactsLists) {
//        this.selectedContactsLists = selectedContactsLists;
//    }
    public void setSelectedContactsLists(String selectedContactsLists) {
        this.selectedContactsLists = selectedContactsLists;
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }

    public String getCfUtenteOperazione() {
        return cfUtenteOperazione;
    }

    public void setCfUtenteOperazione(String cfUtenteOperazione) {
        this.cfUtenteOperazione = cfUtenteOperazione;
    }
    
    public String getGlogParams() {
        return glogParams;
    }

    public void setGlogParams(String glogParams) {
        this.glogParams = glogParams;
    }
}

class SelectedContactsLists {

    private List<SelectedContact> A;

    private List<SelectedContact> CC; //todo fare enum

    private List<SelectedContact> MITTENTE;

    public List<SelectedContact> getA() {
        return A;
    }

    @JsonProperty("A")
    public void setA(List<SelectedContact> A) {
        this.A = A;
    }

    public List<SelectedContact> getCC() {
        return CC;
    }

    @JsonProperty("CC")
    public void setCC(List<SelectedContact> CC) {
        this.CC = CC;
    }

    public List<SelectedContact> getMITTENTE() {
        return MITTENTE;
    }

    @JsonProperty("MITTENTE")
    public void setMITTENTE(List<SelectedContact> MITTENTE) {
        this.MITTENTE = MITTENTE;
    }

}
