package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithAttributiStrutturaAndIdAzienda;
import it.bologna.ausl.model.entities.rubrica.views.ContattoConDettaglioPrincipale;
import it.bologna.ausl.model.entities.rubrica.views.projections.generated.ContattoConDettaglioPrincipaleWithIdPersonaCreazioneAndIdStruttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author mido
 */
@Projection(name = "CustomContattoListFast", types = ContattoConDettaglioPrincipale.class)
public interface CustomContattoListFast extends ContattoConDettaglioPrincipaleWithIdPersonaCreazioneAndIdStruttura {

//    @Value("#{@rubricaProjectionsUtils.getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda(target)}")
//    @Override
//    public List<CustomDe@Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
//    @Override
//    public StrutturaWithAttributiStrutturaAndIdAzienda getIdStruttura();
//
//    @Value("#{@permessiProjectionsUtils.getEntita(target)}")
//    public Object getEntita();ttaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> getDettaglioContattoList();

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
    @Override
    public StrutturaWithAttributiStrutturaAndIdAzienda getIdStruttura();
//
//    @Value("#{@permessiProjectionsUtils.getEntita(target)}")
//    public Object getEntita();
}