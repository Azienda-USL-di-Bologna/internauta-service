package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithGruppiDelContattoListAndIdPersonaCreazioneAndIdStrutturaAndIdUtenteCreazione;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdDettaglioContattoAndIdGruppo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomContattoStrutturaDetail", types = Contatto.class)
public interface CustomContattoStrutturaDetail extends ContattoWithGruppiDelContattoListAndIdPersonaCreazioneAndIdStrutturaAndIdUtenteCreazione {
  
    @Value("#{@projectionBeans.getGruppiContattiWithIdDettaglioContattoAndIdGruppo(target)}")
    @Override
    public List<GruppiContattiWithIdDettaglioContattoAndIdGruppo> getGruppiDelContattoList();
    
}
