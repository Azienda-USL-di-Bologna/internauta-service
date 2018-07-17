/**
 * Auto-Generated using the Jenesis Syntax API
 */
package it.bologna.ausl.baborg.model.entities.projections;

import com.fasterxml.jackson.annotation.JsonFormat;
import it.bologna.ausl.baborg.model.entities.Persona;

import it.bologna.ausl.baborg.model.entities.Struttura;
import it.bologna.ausl.baborg.model.entities.Utente;
import it.bologna.ausl.baborg.model.entities.projections.generated.StrutturaWithPlainFields;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteStrutturaWithIdAfferenzaStruttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteStrutturaWithIdAfferenzaStrutturaAndIdUtente;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteStrutturaWithPlainFields;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteWithIdPersona;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaCustom", types = it.bologna.ausl.baborg.model.entities.UtenteStruttura.class)
public interface UtenteStrutturaCustom extends UtenteStrutturaWithIdAfferenzaStruttura{
    
    @Value("#{@projectionBeans.getUtenteConPersona(target.getIdUtente())}")
    public UtenteWithIdPersona getIdUtente();
    
}
