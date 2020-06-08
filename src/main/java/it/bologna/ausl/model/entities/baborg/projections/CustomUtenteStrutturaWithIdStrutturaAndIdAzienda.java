package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdStruttura;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomUtenteStrutturaWithIdStrutturaAndIdAzienda", types = UtenteStruttura.class)
public interface CustomUtenteStrutturaWithIdStrutturaAndIdAzienda extends UtenteStrutturaWithIdStruttura {
    
    // metto la struttura con l'azienda
    @Value("#{@projectionBeans.getStrutturaConAzienda(target.getIdStruttura())}")
    public StrutturaWithIdAzienda getIdStruttura();
}
