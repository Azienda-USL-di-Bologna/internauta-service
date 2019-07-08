package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */                 
@Projection(name = "StrutturaWithIdAziendaAndPermessiCustom", types = Struttura.class)
public interface StrutturaWithIdAziendaAndPermessiCustom extends StrutturaWithIdAzienda{
    public List<PermessoEntitaStoredProcedure> getPermessi();
}
