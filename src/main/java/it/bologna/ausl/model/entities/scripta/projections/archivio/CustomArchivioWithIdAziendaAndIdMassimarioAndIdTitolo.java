/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioWithIdAziendaAndIdMassimarioAndIdTitolo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Matteo Next
 */
@Projection(name = "CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo", types = it.bologna.ausl.model.entities.scripta.Archivio.class)
public interface CustomArchivioWithIdAziendaAndIdMassimarioAndIdTitolo extends ArchivioWithIdAziendaAndIdMassimarioAndIdTitolo {

    @Value("#{@archivioProjectionUtils.getIsArchivioNero(target)}")
    public Boolean getIsArchivioNero();
}
