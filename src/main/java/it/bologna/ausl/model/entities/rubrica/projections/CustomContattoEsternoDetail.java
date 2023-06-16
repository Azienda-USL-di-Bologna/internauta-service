package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
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

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getEmailList', 'EmailWithIdDettaglioContatto')}")
    @Override
    public List<EmailWithIdDettaglioContatto> getEmailList();

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getIndirizziList', 'IndirizzoWithIdDettaglioContatto')}")
    @Override
    public List<IndirizzoWithIdDettaglioContatto> getIndirizziList();

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getTelefonoList', 'TelefonoWithIdDettaglioContatto')}")
    @Override
    public List<TelefonoWithIdDettaglioContatto> getTelefonoList();

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getGruppiDelContattoList', 'GruppiContattiWithIdDettaglioContattoAndIdGruppo')}")
    @Override
    public List<GruppiContattiWithIdDettaglioContattoAndIdGruppo> getGruppiDelContattoList();

    @Value("#{@permessiProjectionsUtils.getEntita(target)}")
    public Object getEntita();

    @Value("#{@rubricaProjectionsUtils.getPermessiContatto(target)}")
    public List<PermessoEntitaStoredProcedure> getPermessiContatto();

}
