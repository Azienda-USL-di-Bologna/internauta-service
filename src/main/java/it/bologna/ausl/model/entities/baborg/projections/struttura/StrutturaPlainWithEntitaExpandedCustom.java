package it.bologna.ausl.model.entities.baborg.projections.struttura;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Mr. Sal
 */
@Projection(name = "StrutturaPlainWithEntitaExpandedCustom", types = Struttura.class)
public interface StrutturaPlainWithEntitaExpandedCustom extends StrutturaWithIdAzienda {

    @Value("#{@permessiProjectionsUtils.getEntita(target)}")
    public Object getEntita();
    
    @Value("#{@utenteStrutturaProjectionUtils.getCountUtentiStruttura(target)}")
    public String getAdditionalData();
}
