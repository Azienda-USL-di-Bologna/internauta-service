package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithAttributiStrutturaAndIdAziendaAndIdStrutturaPadre;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */                 
@Projection(name = "StrutturaWithAttributiStrutturaIdAziendaIdStrutturaPadreExpandedCustom", types = Struttura.class)
public interface StrutturaWithAttributiStrutturaIdAziendaIdStrutturaPadreExpandedCustom extends StrutturaWithAttributiStrutturaAndIdAziendaAndIdStrutturaPadre{
    
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getAttributiStruttura', 'AttributiStrutturaWithIdTipologiaStruttura')}")
    public Object getAttributiStruttura();
}
