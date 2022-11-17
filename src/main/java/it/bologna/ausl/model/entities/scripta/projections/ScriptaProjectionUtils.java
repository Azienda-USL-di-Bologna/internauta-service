package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.internauta.service.controllers.scripta.ScriptaArchiviUtils;
import it.bologna.ausl.internauta.service.repositories.versatore.VersamentoAllegatoRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetailInterface;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.versatore.QVersamentoAllegato;
import it.bologna.ausl.model.entities.versatore.VersamentoAllegato;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private VersamentoAllegatoRepository versamentoAllegatoRepository;
    
    @Autowired
    protected ProjectionFactory projectionFactory;
    
    @Autowired
    private ScriptaArchiviUtils scriptaArchiviUtils;
    

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
     * Restituisco l'oggetto dell'archivio per la visualizzazione generica.
     * In caso di aziendaparlante l'oggetto è stirnga vuota
     * Altrimenti è l'oggetto della radice
     * @return 
     */
    public String getOggettoArchivioPerVisualizzazioneDiSicurezzaClassica(Archivio archivio) {
        return scriptaArchiviUtils.getOggettoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio);
    }
    
    public VersamentoAllegato getVersamentoAllegatoByIdVersamento(Allegato allegato, Integer idVersamento) {
        if (allegato != null && idVersamento != null) {
            QVersamentoAllegato q = QVersamentoAllegato.versamentoAllegato;
            Optional<VersamentoAllegato> v = versamentoAllegatoRepository.findOne(q.idVersamento.id.eq(idVersamento).and(q.idAllegato.id.eq(allegato.getId())));
            if (v.isPresent())
                return v.get();
        }
        return null;
    }
}
