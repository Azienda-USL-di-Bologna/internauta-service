package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithEmailListAndIdPersonaCreazioneAndIdUtenteCreazioneAndIndirizziListAndTelefonoList;
import it.bologna.ausl.model.entities.rubrica.projections.generated.EmailWithIdDettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.IndirizzoWithIdDettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.TelefonoWithIdDettaglioContatto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Salo
 */
@Projection(name = "CustomContattoEsternoDetail", types = Contatto.class)
public interface CustomContattoEsternoDetail extends ContattoWithEmailListAndIdPersonaCreazioneAndIdUtenteCreazioneAndIndirizziListAndTelefonoList {

    @Value("#{@projectionBeans.getEmailWithIdDettaglioContatto(target)}")
    @Override
    public List<EmailWithIdDettaglioContatto> getEmailList();
    
    @Value("#{@projectionBeans.getIndirizzoWithIdDettaglioContatto(target)}")
    @Override
    public List<IndirizzoWithIdDettaglioContatto> getIndirizziList();
    
    @Value("#{@projectionBeans.getTelefonoWithIdDettaglioContatto(target)}")
    @Override
    public List<TelefonoWithIdDettaglioContatto> getTelefonoList();
    
//    @Value("#{@projectionBeans.getDettaglioContattoExpanded(target)}")
//    @Override
//    public List<DettaglioContattoWithEmailListAndGruppiDelDettaglioListAndIndirizzoListAndTelefonoList> getDettaglioContattoList();
}

//ContattoWithDettaglioContattoListAndEmailListAndIdPersonaCreazioneAndIdUtenteCreazioneAndIndirizziListAndTelefonoList

// ContattoWithDettaglioContattoListAndEmailListAndIdPersonaCreazioneAndIdUtenteCreazioneAndIndirizziListAndTelefonoList

// ContattoWithDettaglioContattoListAndIdPersonaCreazioneAndIdUtenteCreazione