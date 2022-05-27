/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.scripta.ArchivioDetailInterface;
import it.bologna.ausl.model.entities.scripta.Related;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class ScriptaProjectionUtils {
    
    @Autowired
    private CachedEntities cachedEntities;

    @Autowired
    protected ProjectionFactory factory;

    public List<CustomRelatedWithSpedizioneList> filterRelatedWithSpedizioneList(List<Related> related, String tipo) {
        List<CustomRelatedWithSpedizioneList> res = null;
        if (related != null) {
            List<Related> relatedList = related.stream().filter(r -> r.getTipo().toString().equals(tipo)).collect(Collectors.toList());
            if (relatedList != null && !relatedList.isEmpty()) {
                res = relatedList.stream().map(r -> {
                    return factory.createProjection(CustomRelatedWithSpedizioneList.class, r);
                }).collect(Collectors.toList());
            }
        }
        return res;
    }

    public List<Related> filterRelated(List<Related> related, String tipo) {
        if (related != null) {
            return related.stream().filter(r -> r.getTipo().toString().equals(tipo)).collect(Collectors.toList());
        } else {
            return null;
        }
    }
    
    public List<String> getDescrizionePersonaVicarioList(ArchivioDetailInterface archivioDetail){
        List<String> descrizioneVicariList = new ArrayList<>();
        if(archivioDetail != null){
            Integer[] idVicari = archivioDetail.getIdVicari();
            descrizioneVicariList = Stream.of(idVicari).map((idPersonaVicario) -> {
                return cachedEntities.getPersona(idPersonaVicario).getDescrizione();
            }).collect(Collectors.toList());
        }
        return descrizioneVicariList;
    }
    
    
}
