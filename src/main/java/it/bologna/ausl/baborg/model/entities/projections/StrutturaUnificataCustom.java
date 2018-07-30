

package it.bologna.ausl.baborg.model.entities.projections;

import it.bologna.ausl.baborg.model.entities.projections.generated.StrutturaUnificataWithPlainFields;
import it.bologna.ausl.baborg.model.entities.projections.generated.StrutturaWithIdAzienda;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaUnificataCustom", types = it.bologna.ausl.baborg.model.entities.StrutturaUnificata.class)
public interface StrutturaUnificataCustom extends StrutturaUnificataWithPlainFields{
        
    @Value("#{@projectionBeans.getStrutturaConAzienda(target.getIdStrutturaSorgente())}")
    public StrutturaWithIdAzienda getIdStrutturaSorgente();
        
    @Value("#{@projectionBeans.getStrutturaConAzienda(target.getIdStrutturaDestinazione())}")
    public StrutturaWithIdAzienda getIdStrutturaDestinazione();
}