package it.bologna.ausl.model.entities.shpeck.projections;
        
import it.bologna.ausl.model.entities.shpeck.views.MessageWithTagView;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageAddressWithIdAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageFolderWithIdFolder;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageTagWithIdTag;
import it.bologna.ausl.model.entities.shpeck.views.projections.generated.MessageWithTagViewWithMessageAddressListAndMessageFolderListAndMessageTagList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "CustomMessageWithTagViewForMailList", types = MessageWithTagView.class)
public interface CustomMessageWithTagViewForMailList extends MessageWithTagViewWithMessageAddressListAndMessageFolderListAndMessageTagList {
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getMessageAddressList', 'MessageAddressWithIdAddress')}")
    @Override
    public List<MessageAddressWithIdAddress> getMessageAddressList();

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getMessageTagList', 'MessageTagWithIdTag')}")
    @Override
    public List<MessageTagWithIdTag> getMessageTagList();

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getMessageFolderList', 'MessageFolderWithIdFolder')}")
    public List<MessageFolderWithIdFolder> getMessageFolderList();
}