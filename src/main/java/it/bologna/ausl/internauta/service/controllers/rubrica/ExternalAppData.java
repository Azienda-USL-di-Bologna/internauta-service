package it.bologna.ausl.internauta.service.controllers.rubrica;

import java.util.List;
import java.util.Map;

/**
 *
 * @author gusgus
 */
public class ExternalAppData {
    private String mode;
    private String app; //todo fare enum
    private String guid;
    private String codiceAzienda;
    private Map<String, List<SelectedContact>> selectedContactsLists;

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

    public Map<String, List<SelectedContact>> getSelectedContactsLists() {
        return selectedContactsLists;
    }

    public void setSelectedContactsLists(Map<String, List<SelectedContact>> selectedContactsLists) {
        this.selectedContactsLists = selectedContactsLists;
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }
}
