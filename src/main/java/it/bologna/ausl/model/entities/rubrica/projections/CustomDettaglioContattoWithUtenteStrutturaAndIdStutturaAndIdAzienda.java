package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteStrutturaWithIdStrutturaAndIdAzienda;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithUtenteStruttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda", types = DettaglioContatto.class)
public interface CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda extends DettaglioContattoWithUtenteStruttura {

    @Value("#{@projectionBeans.getUtenteStrutturaWithIdStrutturaAndIdAzienda(target)}")
    @Override
    public CustomUtenteStrutturaWithIdStrutturaAndIdAzienda getUtenteStruttura();
}