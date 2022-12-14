package it.bologna.ausl.model.entities.baborg.projections.struttura;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithAttributiStrutturaAndIdAzienda;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */                 
@Projection(name = "StrutturaWithIdAziendaAndPermessiCustom", types = Struttura.class)
public interface StrutturaWithIdAziendaAndPermessiCustom extends StrutturaWithAttributiStrutturaAndIdAzienda{
    public List<PermessoEntitaStoredProcedure> getPermessi();
}
