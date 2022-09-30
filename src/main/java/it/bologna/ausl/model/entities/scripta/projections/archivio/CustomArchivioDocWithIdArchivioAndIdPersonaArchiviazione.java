package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDocWithIdArchivioAndIdPersonaArchiviazione;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione", types = it.bologna.ausl.model.entities.scripta.ArchivioDoc.class)
public interface CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione extends ArchivioDocWithIdArchivioAndIdPersonaArchiviazione {

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdArchivio', 'CustomArchivioWithIdAziendaAndOggettoVisualizzazioneGenerica')}")
    public CustomArchivioWithIdAziendaAndOggettoVisualizzazioneGenerica getIdArchivio();
    
}
