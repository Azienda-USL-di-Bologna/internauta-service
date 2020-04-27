package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.baborg.projections.PersonaWithUtentiAndStruttureAndAfferenzeCustom;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithGruppiDelContattoListAndIdPersonaAndIdPersonaCreazioneAndIdUtenteCreazione;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdDettaglioContattoAndIdGruppo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomContattoPersonaDetail", types = Contatto.class)
public interface CustomContattoPersonaDetail extends ContattoWithGruppiDelContattoListAndIdPersonaAndIdPersonaCreazioneAndIdUtenteCreazione {

    @Value("#{@projectionBeans.getPersonaWithUtentiAndStruttureAndAfferenzeCustom(target)}")
    @Override
    public PersonaWithUtentiAndStruttureAndAfferenzeCustom getIdPersona();
    
    @Value("#{@projectionBeans.getGruppiContattiWithIdDettaglioContattoAndIdGruppo(target)}")
    @Override
    public List<GruppiContattiWithIdDettaglioContattoAndIdGruppo> getGruppiDelContattoList();
    
}
