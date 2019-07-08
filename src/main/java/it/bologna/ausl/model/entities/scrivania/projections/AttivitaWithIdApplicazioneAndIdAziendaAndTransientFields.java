package it.bologna.ausl.model.entities.scrivania.projections;

import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.projections.generated.AttivitaFattaWithIdApplicazioneAndIdAzienda;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "AttivitaWithIdApplicazioneAndIdAziendaAndTransientFields", types = Attivita.class)
public interface AttivitaWithIdApplicazioneAndIdAziendaAndTransientFields extends AttivitaFattaWithIdApplicazioneAndIdAzienda{

    public String getCompiledUrls();
}
