package it.bologna.ausl.model.entities.shpeck.projections;

import it.bologna.ausl.model.entities.shpeck.Draft;
import it.bologna.ausl.model.entities.shpeck.projections.generated.DraftWithIdPec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */

@Projection(name = "CustomDraftWithPlainFields", types = Draft.class)
public interface CustomDraftWithPlainFields extends DraftWithIdPec {
        
    @Value("#{null}")
    @Override
    public byte[] getEml();
}
