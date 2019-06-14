package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithImpostazioniApplicazioniList;
import it.bologna.ausl.model.entities.configuration.projections.generated.ImpostazioniApplicazioniWithPlainFields;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "CustomPersonaLogin", types = Persona.class)
public interface CustomPersonaLogin extends PersonaWithImpostazioniApplicazioniList {
    
    @Override
    @Value("#{@projectionBeans.getImpostazioniApplicazioniListWithPlainFields(target)}")
    public List<ImpostazioniApplicazioniWithPlainFields> getImpostazioniApplicazioniList();
    
    @Override
    @Value("#{@userInfoService.getPermessiPec(target)}")
    public Map<Integer, List<String>> getPermessiPec();
}
