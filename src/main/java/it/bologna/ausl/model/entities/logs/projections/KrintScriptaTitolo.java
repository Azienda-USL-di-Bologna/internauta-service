package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.scripta.Titolo;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author j.vjerdha
 */
@Projection(name = "KrintScriptaTitolo", types = Titolo.class)
public interface KrintScriptaTitolo {
   
   String getNome();
   
   String getClassificazione();
   
}
