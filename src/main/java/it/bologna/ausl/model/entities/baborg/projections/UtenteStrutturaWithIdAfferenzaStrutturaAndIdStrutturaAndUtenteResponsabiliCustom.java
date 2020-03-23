package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStrutturaAndIdStruttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom", types = UtenteStruttura.class)
public interface UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom extends UtenteStrutturaWithIdAfferenzaStrutturaAndIdStruttura {
    
    @Override
    @Value("#{@projectionBeans.getStrutturaWithUtentiReponsabili(target)}")
    public StrutturaWithUtentiResponsabiliCustom getIdStruttura();

}
