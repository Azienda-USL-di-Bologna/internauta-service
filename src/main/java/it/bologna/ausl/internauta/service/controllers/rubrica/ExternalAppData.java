package it.bologna.ausl.internauta.service.controllers.rubrica;

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
//    private Map<String, List<SelectedContact>> selectedContactsLists;

    public String getMode() {
        return mode;
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
}
