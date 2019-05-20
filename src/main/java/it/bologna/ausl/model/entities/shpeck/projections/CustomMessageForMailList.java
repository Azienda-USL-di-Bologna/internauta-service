package it.bologna.ausl.model.entities.shpeck.projections;
        
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageAddressWithIdAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageTagWithIdTag;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageWithMessageAddressListAndMessageFolderListAndMessageTagList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "CustomMessageForMailList", types = Message.class)
public interface CustomMessageForMailList extends MessageWithMessageAddressListAndMessageFolderListAndMessageTagList {
    
    @Value("#{@projectionBeans.getMessageAddressListWithIdAddress(target)}")
    @Override
    public List<MessageAddressWithIdAddress> getMessageAddressList();
    
    @Value("#{@projectionBeans.getMessageTagListWithIdTag(target)}")
    @Override
    public List<MessageTagWithIdTag> getMessageTagList();

    @Override
    @Value("#{@projectionBeans.getMessageFolderListWithIdFolder(target)}")
    public Object getMessageFolderList();
   
}