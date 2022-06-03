package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioWithIdArchivioRadice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomArchivioWithIdArchivioRadice", types = it.bologna.ausl.model.entities.scripta.Archivio.class)
public interface CustomArchivioWithIdArchivioRadice extends ArchivioWithIdArchivioRadice {

    @Value("#{@archivioProjectionUtils.getIsArchivioNero(target)}")
    public Boolean getIsArchivioNero();
}
