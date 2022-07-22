package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDocWithIdArchivioAndIdPersonaArchiviazione;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocDetailWithIdApplicazioneAndIdAziendaAndIdPersonaRedattriceAndIdPersonaResponsabileProcedimentoAndIdStrutturaRegistrazione;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomDocDetailForDocList", types = DocDetail.class)
public interface CustomDocDetailForDocList extends DocDetailWithIdApplicazioneAndIdAziendaAndIdPersonaRedattriceAndIdPersonaResponsabileProcedimentoAndIdStrutturaRegistrazione {

    @Value("#{@scriptaProjectionUtils.getArchiviDocList(target.getId())}")
    public List<ArchivioDocWithIdArchivioAndIdPersonaArchiviazione> getArchiviDocList();
    
}
