package it.bologna.ausl.model.entities.shpeck.projections;
        
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageAddressWithIdAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageWithIdRecepitAndMessageAddressList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "CustomMessageWithAddressList", types = Azienda.class)
public interface CustomMessageWithAddressList extends MessageWithIdRecepitAndMessageAddressList {
    
    @Value("#{@projectionBeans.getMessageAddressListWithIdAddress(target)}")
    @Override
    public List<MessageAddressWithIdAddress> getMessageAddressList();
   
}