package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioRecenteWithIdArchivio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Matteo Next
 */
@Projection(name = "CustomArchivioRecenteWithIdArchivioDetail", types = it.bologna.ausl.model.entities.scripta.ArchivioRecente.class)
public interface CustomArchivioRecenteWithIdArchivioDetail extends ArchivioRecenteWithIdArchivio {

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdArchivio', 'CustomArchivioDetailWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicari')}")
    public CustomArchivioDetailWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicari getIdArchivio();
}
