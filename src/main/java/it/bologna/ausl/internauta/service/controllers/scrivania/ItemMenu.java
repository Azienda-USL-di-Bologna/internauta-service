package it.bologna.ausl.internauta.service.controllers.scrivania;

import java.util.List;

/**
 *
 * @author gusgus
 */
public class ItemMenu {
    private Integer id;
    private String descrizione;
    private String urlCommand;
    private String icona;
    private List<ItemMenu> children;

    public ItemMenu(Integer id, String descrizione, String urlCommand, String icona, List<ItemMenu> children) {
        this.id = id;
        this.descrizione = descrizione;
        this.urlCommand = urlCommand;
        this.icona = icona;
        this.children = children;
    }
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getUrlCommand() {
        return urlCommand;
    }

    public void setUrlCommand(String urlCommand) {
        this.urlCommand = urlCommand;
    }

    public String getIcona() {
        return icona;
    }

    public void setIcona(String icona) {
        this.icona = icona;
    }

    public List<ItemMenu> getChildren() {
        return children;
    }

    public void setChildren(List<ItemMenu> children) {
        this.children = children;
    }
    
    
}
