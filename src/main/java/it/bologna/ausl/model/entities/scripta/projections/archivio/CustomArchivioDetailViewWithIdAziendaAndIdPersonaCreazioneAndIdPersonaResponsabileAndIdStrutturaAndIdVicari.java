/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.views.projections.generated.ArchivioDetailViewWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStruttura;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Salo
 */
@Projection(name = "CustomArchivioDetailViewWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicari", types = it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView.class)
public interface CustomArchivioDetailViewWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicari extends ArchivioDetailViewWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStruttura {

    @Value("#{@scriptaProjectionUtils.getDescrizionePersonaVicarioList(target)}")
    public List<String> getDescrizionePersonaVicarioList();

    @Value("#{@archivioProjectionUtils.getIsArchivioNeroView(target)}")
    public Boolean getIsArchivioNeroView();
}
