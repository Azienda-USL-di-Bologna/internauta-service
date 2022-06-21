package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDocWithIdArchivioAndIdPersonaArchiviazione;
import it.bologna.ausl.model.entities.scripta.views.DocDetailView;
import it.bologna.ausl.model.entities.scripta.views.projections.generated.DocDetailViewWithIdApplicazioneAndIdAziendaAndIdPersonaRedattriceAndIdPersonaResponsabileProcedimentoAndIdStrutturaRegistrazione;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomDocDetailViewForDocList", types = DocDetailView.class)
public interface CustomDocDetailViewForDocList extends DocDetailViewWithIdApplicazioneAndIdAziendaAndIdPersonaRedattriceAndIdPersonaResponsabileProcedimentoAndIdStrutturaRegistrazione {

    @Value("#{@scriptaProjectionUtils.getArchiviDocList(target.getId())}")
    public List<ArchivioDocWithIdArchivioAndIdPersonaArchiviazione> getArchiviDocList();
    
}
