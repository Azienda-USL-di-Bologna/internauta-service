package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioWithAttoriListAndIdAziendaAndIdMassimarioAndIdTitolo;
import it.bologna.ausl.model.entities.scripta.projections.generated.AttoreArchivioWithIdPersonaAndIdStruttura;
import it.bologna.ausl.model.entities.scripta.projections.generated.PermessoArchivioWithPlainFields;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Matteo Next
 */
@Projection(name = "CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo", types = it.bologna.ausl.model.entities.scripta.Archivio.class)
public interface CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo extends ArchivioWithAttoriListAndIdAziendaAndIdMassimarioAndIdTitolo {

    @Value("#{@archivioProjectionUtils.getIsArchivioNero(target)}")
    public Boolean getIsArchivioNero();
    
    @Value("#{@archivioProjectionUtils.getPermessi(target)}")
    public List<PermessoEntitaStoredProcedure> getPermessi();
    
    @Value("#{@archivioProjectionUtils.getPermessiEspliciti(target)}")
    public List<PermessoArchivioWithPlainFields> getPermessiEspliciti();
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getAttoriList', 'AttoreArchivioWithIdPersonaAndIdStruttura')}")
    @Override
    public List<AttoreArchivioWithIdPersonaAndIdStruttura> getAttoriList();
}
