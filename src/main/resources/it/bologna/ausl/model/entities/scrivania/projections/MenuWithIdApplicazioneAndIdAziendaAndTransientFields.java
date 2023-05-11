package it.bologna.ausl.model.entities.scrivania.projections;

import it.bologna.ausl.model.entities.scrivania.Menu;
import it.bologna.ausl.model.entities.scrivania.projections.generated.MenuWithIdApplicazioneAndIdAzienda;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "MenuWithIdApplicazioneAndIdAziendaAndTransientFields", types = Menu.class)
public interface MenuWithIdApplicazioneAndIdAziendaAndTransientFields extends MenuWithIdApplicazioneAndIdAzienda{

    @Override
    public String getCompiledUrl();
}
