package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.projections.archivio.CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione;
import it.bologna.ausl.model.entities.scripta.views.DocDetailView;
import it.bologna.ausl.model.entities.scripta.views.projections.generated.DocDetailViewWithArchiviDocListAndIdApplicazioneAndIdAziendaAndIdPersonaRedattriceAndIdPersonaResponsabileProcedimentoAndIdStrutturaRegistrazione;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomDocDetailViewForDocList", types = DocDetailView.class)
public interface CustomDocDetailViewForDocList extends DocDetailViewWithArchiviDocListAndIdApplicazioneAndIdAziendaAndIdPersonaRedattriceAndIdPersonaResponsabileProcedimentoAndIdStrutturaRegistrazione {

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getArchiviDocList', 'CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione')}")
    public List<CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione> getArchiviDocList();
    
}
