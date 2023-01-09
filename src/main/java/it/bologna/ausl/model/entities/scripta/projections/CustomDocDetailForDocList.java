package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.projections.archivio.CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocDetailWithArchiviDocListAndIdApplicazioneAndIdAziendaAndIdPersonaRedattriceAndIdPersonaResponsabileProcedimentoAndIdStrutturaRegistrazioneAndPersoneVedentiList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomDocDetailForDocList", types = DocDetail.class)
public interface CustomDocDetailForDocList extends DocDetailWithArchiviDocListAndIdApplicazioneAndIdAziendaAndIdPersonaRedattriceAndIdPersonaResponsabileProcedimentoAndIdStrutturaRegistrazioneAndPersoneVedentiList {

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getArchiviDocList', 'CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione')}")
    public List<CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione> getArchiviDocList();
    
}
