package it.bologna.ausl.baborg.model.entities.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaUnificataWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaUnificataCustom", types = it.bologna.ausl.model.entities.baborg.StrutturaUnificata.class)
public interface StrutturaUnificataCustom extends StrutturaUnificataWithPlainFields{
        
    @Value("#{@projectionBeans.getStrutturaConAzienda(target.getIdStrutturaSorgente())}")
    public StrutturaWithIdAzienda getIdStrutturaSorgente();
        
    @Value("#{@projectionBeans.getStrutturaConAzienda(target.getIdStrutturaDestinazione())}")
    public StrutturaWithIdAzienda getIdStrutturaDestinazione();
}