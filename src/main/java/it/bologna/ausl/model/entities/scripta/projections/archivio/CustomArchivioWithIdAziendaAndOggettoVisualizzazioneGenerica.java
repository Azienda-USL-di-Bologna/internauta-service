package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioWithIdAzienda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomArchivioWithIdAziendaAndOggettoVisualizzazioneGenerica", types = it.bologna.ausl.model.entities.scripta.Archivio.class)
public interface CustomArchivioWithIdAziendaAndOggettoVisualizzazioneGenerica extends ArchivioWithIdAzienda {

    @Value("#{@scriptaProjectionUtils.getOggettoArchivioPerVisualizzazioneDiSicurezzaClassica(target)}")
    public String getOggetto();
    
}
