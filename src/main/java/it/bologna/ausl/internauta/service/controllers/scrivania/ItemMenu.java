package it.bologna.ausl.internauta.service.controllers.scrivania;

import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.scrivania.Bmenu;
import java.util.List;

/**
 *
 * @author gusgus
 */
public class ItemMenu {
    private Integer id;
    private String descrizione;
    private String openCommand;
    private String icona;
    private List<ItemMenu> children;
    private Bmenu.CommandType commandType;
    private Applicazione.UrlsGenerationStrategy urlGenerationStrategy;
    private Boolean scomponiPerAzienda;
    private Boolean foglia;

    public ItemMenu(Integer id, String descrizione, String openCommand, String icona, List<ItemMenu> children, Bmenu.CommandType commandType, Applicazione.UrlsGenerationStrategy urlGenerationStrategy, Boolean scomponiPerAzienda, Boolean foglia) {
        this.id = id;
        this.descrizione = descrizione;
        this.openCommand = openCommand;
        this.icona = icona;
        this.children = children;
        this.commandType = commandType;
        this.urlGenerationStrategy = urlGenerationStrategy;
        this.scomponiPerAzienda = scomponiPerAzienda;
        this.foglia = foglia;
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

    public String getOpenCommand() {
        return openCommand;
    }

    public void setOpenCommand(String openCommand) {
        this.openCommand = openCommand;
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

    public Bmenu.CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(Bmenu.CommandType commandType) {
        this.commandType = commandType;
    }

    public Applicazione.UrlsGenerationStrategy getUrlGenerationStrategy() {
        return urlGenerationStrategy;
    }

    public void setUrlGenerationStrategy(Applicazione.UrlsGenerationStrategy urlGenerationStrategy) {
        this.urlGenerationStrategy = urlGenerationStrategy;
    }

    public Boolean getScomponiPerAzienda() {
        return scomponiPerAzienda;
    }

    public void setScomponiPerAzienda(Boolean scomponiPerAzienda) {
        this.scomponiPerAzienda = scomponiPerAzienda;
    }

    public Boolean getFoglia() {
        return foglia;
    }

    public void setFoglia(Boolean foglia) {
        this.foglia = foglia;
    }
 
}
