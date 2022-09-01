package it.bologna.ausl.model.entities.baborg.projections.utentestruttura;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.struttura.StrutturaWithUtentiResponsabiliCustom;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStrutturaAndIdDettaglioContattoAndIdStruttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom", types = UtenteStruttura.class)
public interface UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom extends UtenteStrutturaWithIdAfferenzaStrutturaAndIdDettaglioContattoAndIdStruttura {
    
    @Override
    @Value("#{@utenteStrutturaProjectionUtils.getStrutturaWithUtentiReponsabili(target)}")
    public StrutturaWithUtentiResponsabiliCustom getIdStruttura();

}
