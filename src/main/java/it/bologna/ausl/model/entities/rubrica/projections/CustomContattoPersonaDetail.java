package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.baborg.projections.persona.PersonaWithUtentiAndStruttureAndAfferenzeCustom;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithDettaglioContattoListAndEmailListAndGruppiDelContattoListAndIdPersonaAndIdPersonaCreazioneAndIdUtenteCreazione;
import it.bologna.ausl.model.entities.rubrica.projections.generated.EmailWithIdDettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdDettaglioContattoAndIdGruppo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomContattoPersonaDetail", types = Contatto.class)
public interface CustomContattoPersonaDetail extends ContattoWithDettaglioContattoListAndEmailListAndGruppiDelContattoListAndIdPersonaAndIdPersonaCreazioneAndIdUtenteCreazione {
                
    @Value("#{@rubricaProjectionsUtils.getPersonaWithUtentiAndStruttureAndAfferenzeCustom(target)}")
    @Override
    public PersonaWithUtentiAndStruttureAndAfferenzeCustom getIdPersona();
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getGruppiDelContattoList', 'GruppiContattiWithIdDettaglioContattoAndIdGruppo')}")
    @Override
    public List<GruppiContattiWithIdDettaglioContattoAndIdGruppo> getGruppiDelContattoList();

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getEmailList', 'EmailWithIdDettaglioContatto')}")
    @Override
    public List<EmailWithIdDettaglioContatto> getEmailList();
}
