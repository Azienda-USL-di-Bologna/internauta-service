package it.bologna.ausl.model.entities.baborg.projections.utentestruttura;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithAttributiStrutturaAndIdAzienda;
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
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
    public Object getIdStruttura();
}
