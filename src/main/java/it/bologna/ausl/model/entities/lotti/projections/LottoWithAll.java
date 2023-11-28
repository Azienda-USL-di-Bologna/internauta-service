package it.bologna.ausl.model.entities.lotti.projections;

import it.bologna.ausl.model.entities.lotti.Lotto;
import it.bologna.ausl.model.entities.lotti.projections.generated.LottoWithIdContraenteAndIdDocAndIdTipologia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author QB
 */
@Projection(name = "LottoWithAll", types = Lotto.class)
public interface LottoWithAll extends LottoWithIdContraenteAndIdDocAndIdTipologia{
    
    @Value("#{@lottiProjectionUtils.filterGruppoLottoPerTipo(target.getGruppiList(), 'PARTECIPANTE')}")
    public Object getGruppiPartecipanti();
    
    @Value("#{@lottiProjectionUtils.filterGruppoLottoPerTipo(target.getGruppiList(), 'AGGIUDICATARIO')}")
    public Object getGruppiAggiudicatari();
}
