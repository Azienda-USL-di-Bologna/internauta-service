package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.scripta.Massimario;
import org.springframework.data.rest.core.config.Projection;

/**
 * 
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Projection(name = "KrintScriptaMassimario", types = Massimario.class)
public interface KrintScriptaMassimario {

    Integer getId();
    String getNome();    
}
