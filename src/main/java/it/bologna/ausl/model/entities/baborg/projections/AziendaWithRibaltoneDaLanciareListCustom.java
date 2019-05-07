
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithRibaltoneDaLanciareList;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "AziendaWithRibaltoneDaLanciareListCustom", types = Azienda.class)
public interface AziendaWithRibaltoneDaLanciareListCustom extends AziendaWithRibaltoneDaLanciareList{
    
    // metto l'utente con la persona
//    @Value("#{@projectionBeans.getUtenteConPersona(target.getRibaltoneDaLanciare().getIdUtente())}")
//    public UtenteWithIdPersona();
    
    @Value("#{@projectionBeans.getRibaltoneDaLanciareListWithIdUtente(target)}")
    public Object getRibaltoneDaLanciareList();
    
//    @Value("#{@projectionBeans.getMessageAddressListWithIdAddress(target)}")
//    @Override
//    public List<MessageAddressWithIdAddress> getMessageAddressList();
}
