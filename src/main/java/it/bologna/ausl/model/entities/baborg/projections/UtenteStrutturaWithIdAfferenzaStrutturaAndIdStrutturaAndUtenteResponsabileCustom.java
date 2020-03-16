package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStrutturaAndIdStruttura;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabileCustom", types = UtenteStruttura.class)
public interface UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabileCustom extends UtenteStrutturaWithIdAfferenzaStrutturaAndIdStruttura {
    
    @Override
    @Value("#{@projectionBeans.getStrutturaWithUtenteReponsabile(target)}")
    public StrutturaWithUtenteResponsabileCustom getIdStruttura();

}
