package it.bologna.ausl.model.entities.scrivania.projections;

import it.bologna.ausl.model.entities.scrivania.AttivitaFatta;
import it.bologna.ausl.model.entities.scrivania.projections.generated.AttivitaFattaWithIdApplicazioneAndIdAzienda;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "AttivitaFattaWithIdApplicazioneAndIdAziendaAndTransientFields", types = AttivitaFatta.class)
public interface AttivitaFattaWithIdApplicazioneAndIdAziendaAndTransientFields extends AttivitaFattaWithIdApplicazioneAndIdAzienda{

    @Override
    public String getCompiledUrls();
}
