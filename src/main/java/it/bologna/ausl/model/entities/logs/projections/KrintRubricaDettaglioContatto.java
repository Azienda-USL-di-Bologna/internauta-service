package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintRubricaDettaglioContatto", types = DettaglioContatto.class)
public interface KrintRubricaDettaglioContatto{
    
    Integer getId();
    String getDescrizione();
    Boolean getPrincipale();

    @Value("#{target.getTipo().name()}") 
    String getTipo();

    Boolean getEliminato();
}
