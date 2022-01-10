package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteStrutturaWithIdStrutturaAndIdAzienda;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithIndirizzoAndUtenteStruttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda", types = DettaglioContatto.class)
public interface CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda extends DettaglioContattoWithIndirizzoAndUtenteStruttura {

//    @Value("#{@projectionBeans.getUtenteStrutturaWithIdStrutturaAndIdAzienda(target)}")
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getUtenteStruttura', 'CustomUtenteStrutturaWithIdStrutturaAndIdAzienda')}")
    @Override
    public CustomUtenteStrutturaWithIdStrutturaAndIdAzienda getUtenteStruttura();
}