/**
 * Auto-Generated using the Jenesis Syntax API
 */
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaWithUtenteResponsabileCustom", types = Struttura.class)
public interface StrutturaWithUtenteResponsabileCustom extends StrutturaWithPlainFields {


    @Value("#{@projectionBeans.getResposabileStruttura(target)}")
    public UtenteWithIdPersona getResponsabile();

//    @Value("#{@projectionBeans.getAfferenza(target)}")
//    public AfferenzaStruttura getAfferenza();
}
