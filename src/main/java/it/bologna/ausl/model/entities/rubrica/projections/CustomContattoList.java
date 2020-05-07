package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithDettaglioContattoListAndIdPersonaCreazioneAndIdStruttura;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithUtenteStruttura;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomContattoList", types = Contatto.class)
public interface CustomContattoList extends ContattoWithDettaglioContattoListAndIdPersonaCreazioneAndIdStruttura {

    @Value("#{@projectionBeans.getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda(target)}")
    @Override
    public List<CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> getDettaglioContattoList();
 
    @Value("#{@projectionBeans.getStrutturaWithIdAzienda(target)}")
    @Override
    public StrutturaWithIdAzienda getIdStruttura();
}