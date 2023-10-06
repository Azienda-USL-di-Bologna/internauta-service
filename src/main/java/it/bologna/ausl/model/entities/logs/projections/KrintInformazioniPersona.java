package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;


@Projection(name = "KrintInformazioniPersona", types = Persona.class)
public interface KrintInformazioniPersona{
     
    @Value("#{target.getId()}") 
    Integer getId();
    
    @Value("#{target.getBitRuoli()}")
    Integer getBitRuoli();
    
    @Value("#{target.getCodiceFiscale()}") 
    String getCodiceFiscale();
    
    @Value("#{target.getDescrizione()}") 
    String getDescrizione();
    
    @Value("#{target.getIdAziendaDefault().getId()}") 
    Integer getIdAziendaDefault();
}
