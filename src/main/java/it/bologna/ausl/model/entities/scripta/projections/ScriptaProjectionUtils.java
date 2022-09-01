package it.bologna.ausl.model.entities.scripta.projections;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.scripta.ArchivioDetailInterface;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDocWithIdArchivioAndIdPersonaArchiviazione;
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
    
    @Autowired
    private ArchivioDocRepository archivioDocRepository;
    
    @Autowired
    protected ProjectionFactory projectionFactory;

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
    
    /**
     * A partire da un idDoc, restituisco la lista delle sue archiviazioni
     * @param idDoc
     * @return 
     */
    public List<ArchivioDocWithIdArchivioAndIdPersonaArchiviazione> getArchiviDocList(Integer idDoc){
        List<ArchivioDocWithIdArchivioAndIdPersonaArchiviazione> res = null;
        BooleanExpression filter = QArchivioDoc.archivioDoc.idDoc.id.eq(idDoc);
        Iterable<ArchivioDoc> archiviDocIterable = archivioDocRepository.findAll(filter);
        
        if (archiviDocIterable != null) {
            List<ArchivioDoc> archiviDoc = new ArrayList<>();
            archiviDocIterable.forEach(archiviDoc::add);
            
            if (archiviDoc != null && !archiviDoc.isEmpty()) {
                res = archiviDoc.stream().map(archivioDoc -> {
                    return projectionFactory.createProjection(ArchivioDocWithIdArchivioAndIdPersonaArchiviazione.class, archivioDoc);                
                }).collect(Collectors.toList());
            }
        }
        
        return res;
    }
}
