package it.bologna.ausl.model.entities.baborg.projections;
        
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import it.bologna.ausl.model.entities.scrivania.projections.generated.AttivitaWithIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "TestGdm", types = Azienda.class)
public interface TestGdm extends AziendaWithPlainFields {
    
    @Value("#{@projectionBeans.getAttivitaWithIdPersona(target)}")
    public List<AttivitaWithIdPersona> getAttivitaListWithIdPersona();
   
}