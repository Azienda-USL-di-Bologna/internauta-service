package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithAttributiStrutturaAndIdAzienda;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithDettaglioContattoListAndIdPersonaCreazioneAndIdStruttura;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomContattoList", types = Contatto.class)
public interface CustomContattoList extends ContattoWithDettaglioContattoListAndIdPersonaCreazioneAndIdStruttura {

    @Value("#{@rubricaProjectionsUtils.getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda(target)}")
    @Override
    public List<CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> getDettaglioContattoList();

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
    @Override
    public StrutturaWithAttributiStrutturaAndIdAzienda getIdStruttura();

    @Value("#{@permessiProjectionsUtils.getEntita(target)}")
    public Object getEntita();
}
