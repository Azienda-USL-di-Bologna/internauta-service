/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.scripta.projections.archivio;

import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDetailWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStruttura;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Salo
 */
@Projection(name = "ArchivioDetailWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicari", types = it.bologna.ausl.model.entities.scripta.ArchivioDetail.class)
public interface ArchivioDetailWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStrutturaAndIdVicari extends ArchivioDetailWithIdAziendaAndIdPersonaCreazioneAndIdPersonaResponsabileAndIdStruttura{

    @Value("#{@scriptaProjectionUtils.getDescrizionePersonaVicarioList(target)}")
    public List<String> getDescrizionePersonaVicarioList();
    
}
