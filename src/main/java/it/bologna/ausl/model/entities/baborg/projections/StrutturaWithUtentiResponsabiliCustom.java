package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaWithUtentiResponsabiliCustom", types = Struttura.class)
public interface StrutturaWithUtentiResponsabiliCustom extends StrutturaWithPlainFields {


    @Value("#{@projectionBeans.getResposabiliStruttura(target)}")
    public List<UtenteWithIdPersona> getResponsabili();

//    @Value("#{@projectionBeans.getAfferenza(target)}")
//    public AfferenzaStruttura getAfferenza();
}
