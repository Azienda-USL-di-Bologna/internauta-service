
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithRibaltoneDaLanciareList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "AziendaWithRibaltoneDaLanciareListCustom", types = Azienda.class)
public interface AziendaWithRibaltoneDaLanciareListCustom extends AziendaWithRibaltoneDaLanciareList{
    
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getRibaltoneDaLanciareList', 'AziendaWithRibaltoneDaLanciareListCustom')}")
    @Override
    public List<AziendaWithRibaltoneDaLanciareListCustom> getRibaltoneDaLanciareList();
    
}
