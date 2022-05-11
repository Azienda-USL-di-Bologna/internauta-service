/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Salo
 */
@Projection(name = "CustomArchivioDetailViewWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicariAndPermessi", types = it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView.class)
public interface CustomArchivioDetailViewWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicariAndPermessi extends CustomArchivioDetailViewWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicari {

    @Value("#{@archivioProjectionUtils.getPermessi(target)}")
    public List<PermessoEntitaStoredProcedure> getPermessi();
}
