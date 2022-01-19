package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithEmailListAndGruppiDelContattoListAndIdPersonaCreazioneAndIdUtenteCreazioneAndIndirizziListAndTelefonoList;
import it.bologna.ausl.model.entities.rubrica.projections.generated.EmailWithIdDettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdDettaglioContattoAndIdGruppo;
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
public interface CustomContattoEsternoDetail extends ContattoWithEmailListAndGruppiDelContattoListAndIdPersonaCreazioneAndIdUtenteCreazioneAndIndirizziListAndTelefonoList {

    @Value("#{@projectionBeans.getEmailWithIdDettaglioContatto(target)}")
    @Override
    public List<EmailWithIdDettaglioContatto> getEmailList();

    @Value("#{@projectionBeans.getIndirizzoWithIdDettaglioContatto(target)}")
    @Override
    public List<IndirizzoWithIdDettaglioContatto> getIndirizziList();

    @Value("#{@projectionBeans.getTelefonoWithIdDettaglioContatto(target)}")
    @Override
    public List<TelefonoWithIdDettaglioContatto> getTelefonoList();

    @Value("#{@projectionBeans.getGruppiContattiWithIdDettaglioContattoAndIdGruppo(target)}")
    @Override
    public List<GruppiContattiWithIdDettaglioContattoAndIdGruppo> getGruppiDelContattoList();

    @Value("#{@projectionBeans.getEntita(target)}")
    public Object getEntita();

    @Value("#{@projectionBeans.getPermessiContatto(target)}")
    public List<PermessoEntitaStoredProcedure> getPermessiContatto();

}
