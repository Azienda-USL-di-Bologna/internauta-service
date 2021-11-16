package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecAziendaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithPlainFields;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "PecPlainWithStruttureAndGestoriCustom", types = Pec.class)
public interface PecPlainWithStruttureAndGestoriCustom extends PecWithPecProviderAndAziendaCustom {

    //public List<PermessoEntitaStoredProcedure> getPermessi();
    @Override
    //@Value("#{@projectionBeans.getPecAziendaListWithIdAzienda(target.getPecAziendaList())}")
    public List<PecAziendaWithIdAzienda> getPecAziendaList();

    public List<PersonaWithPlainFields> getGestori();
}
