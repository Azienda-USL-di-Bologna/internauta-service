/**
 * Auto-Generated using the Jenesis Syntax API
 */
package it.bologna.ausl.model.entities.baborg.projections.struttura;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithStruttureFiglieList;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaCustom", types = Struttura.class)
public interface StrutturaWithStruttureFiglieListCustom extends StrutturaWithStruttureFiglieList{

    @Override
    public Object getStruttureFiglieList();

}
