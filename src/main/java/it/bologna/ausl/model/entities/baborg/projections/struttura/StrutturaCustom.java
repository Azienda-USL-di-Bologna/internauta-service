package it.bologna.ausl.model.entities.baborg.projections.struttura;

import it.bologna.ausl.model.entities.baborg.projections.utentestruttura.UtenteStrutturaWithIdAfferenzaStrutturaCustom;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaCustom", types = Struttura.class)
public interface StrutturaCustom extends StrutturaWithPlainFields{

//TODO:DA FARE VEDERE A GDM
    @Value("#{@projectionBeans.getUtenteStrutturaCustom(target.getId)}")
    public List<UtenteStrutturaWithIdAfferenzaStrutturaCustom> getUtenteStrutturaSet();

}
