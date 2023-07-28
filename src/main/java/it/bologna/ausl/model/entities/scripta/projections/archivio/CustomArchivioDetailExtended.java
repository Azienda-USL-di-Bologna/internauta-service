package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDetailWithIdAziendaAndIdMassimarioAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdTitolo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Salo
 */
@Projection(name = "CustomArchivioDetailExtended", types = it.bologna.ausl.model.entities.scripta.ArchivioDetail.class)
public interface CustomArchivioDetailExtended extends ArchivioDetailWithIdAziendaAndIdMassimarioAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdTitolo {

    @Value("#{@scriptaProjectionUtils.getDescrizionePersonaVicarioList(target)}")
    public List<String> getDescrizionePersonaVicarioList();

    @Value("#{@archivioProjectionUtils.getIsArchivioNero(target)}")
    public Boolean getIsArchivioNero();

}
