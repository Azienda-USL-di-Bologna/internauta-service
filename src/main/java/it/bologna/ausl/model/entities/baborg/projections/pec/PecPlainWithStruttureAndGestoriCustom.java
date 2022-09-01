package it.bologna.ausl.model.entities.baborg.projections.pec;

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

    @Override
    public List<PecAziendaWithIdAzienda> getPecAziendaList();

    public List<PersonaWithPlainFields> getGestori();
}
