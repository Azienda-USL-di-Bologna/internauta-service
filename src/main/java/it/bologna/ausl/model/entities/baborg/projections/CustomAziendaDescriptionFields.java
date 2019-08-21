package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Azienda;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Projection(name = "CustomAziendaDescriptionFields", types = Azienda.class)
public interface CustomAziendaDescriptionFields {
    
    public Integer getId();
    
    public String getCodice();
        
    public String getNome();
    
    public String getDescrizione();   
}
