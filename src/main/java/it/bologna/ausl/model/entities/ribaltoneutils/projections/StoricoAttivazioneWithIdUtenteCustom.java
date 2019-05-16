
package it.bologna.ausl.model.entities.ribaltoneutils.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.ribaltoneutils.StoricoAttivazione;
import it.bologna.ausl.model.entities.ribaltoneutils.projections.generated.StoricoAttivazioneWithIdUtente;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StoricoAttivazioneWithIdUtenteCustom", types = StoricoAttivazione.class)
public interface StoricoAttivazioneWithIdUtenteCustom extends StoricoAttivazioneWithIdUtente {
    
    @Value("#{@projectionBeans.getUtenteConPersona(target.getIdUtente())}")
    public UtenteWithIdPersona getIdUtente();
    
}
