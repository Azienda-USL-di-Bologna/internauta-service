package it.bologna.ausl.model.entities.baborg.projections.struttura;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaWithUtentiResponsabiliCustom", types = Struttura.class)
public interface StrutturaWithUtentiResponsabiliCustom extends StrutturaWithPlainFields {


    @Value("#{@strutturaProjectionUtils.getResposabiliStruttura(target)}")
    public List<UtenteWithIdPersona> getResponsabili();

}
