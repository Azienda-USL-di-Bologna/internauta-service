package it.bologna.ausl.model.entities.shpeck.projections;

import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecWithFolderListAndPecAziendaListAndTagList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */

@Projection(name = "CustomPecWithFolderListAndPecAziendaListAndTagList", types = Pec.class)
public interface CustomPecWithFolderListAndPecAziendaListAndTagList extends PecWithFolderListAndPecAziendaListAndTagList {
        
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getTagList', @projectionsInterceptorLauncher.buildSort('asc', 'type', 'description'))}")
    @Override
    public Object getTagList();

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getFolderList', @projectionsInterceptorLauncher.buildSort('asc', 'order', 'description'))}")
    @Override
    public Object getFolderList();
    
    
}
