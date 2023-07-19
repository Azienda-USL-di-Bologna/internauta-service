package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.views.projections.generated.ArchivioDetailViewWithIdAziendaAndIdMassimarioAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdTitolo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Salo
 */
@Projection(name = "CustomArchivioDetailViewExtended", types = it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView.class)
public interface CustomArchivioDetailViewExtended extends ArchivioDetailViewWithIdAziendaAndIdMassimarioAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdTitolo {

    @Value("#{@scriptaProjectionUtils.getDescrizionePersonaVicarioList(target)}")
    public List<String> getDescrizionePersonaVicarioList();

    @Value("#{@archivioProjectionUtils.getIsArchivioNeroView(target)}")
    public Boolean getIsArchivioNeroView();
}
