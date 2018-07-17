/**
 * Auto-Generated using the Jenesis Syntax API
 */
package it.bologna.ausl.baborg.model.entities.projections;

import com.fasterxml.jackson.annotation.JsonFormat;

import it.bologna.ausl.baborg.model.entities.Struttura;
import it.bologna.ausl.baborg.model.entities.Utente;
import it.bologna.ausl.baborg.model.entities.projections.generated.StrutturaWithPlainFields;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaCustom", types = it.bologna.ausl.baborg.model.entities.Struttura.class)
public interface StrutturaCustom extends StrutturaWithPlainFields{


    @Value("#{@projectionBeans.getUtenteStrutturaCustom(target.getId)}")
    public List<UtenteStrutturaCustom> getUtenteStrutturaSet();

}
