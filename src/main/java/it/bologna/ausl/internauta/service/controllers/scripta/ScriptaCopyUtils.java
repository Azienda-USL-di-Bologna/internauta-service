
package it.bologna.ausl.internauta.service.controllers.scripta;

import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Classe utile alla copia di un Doc
 * @author gusgus
 */
@Component
public class ScriptaCopyUtils {
    
    private static enum MotivazioneEsclusione {
        ARCHIVI_CHIUSI, ARCHIVI_ITER, SENZA_PERMESSO
    }
    
    @Autowired
    private ScriptaArchiviUtils scriptaArchiviUtils;
    
    @Autowired
    private ArchivioDocRepository archivioDocRepository;
    
    public Map<String, List<String>> copiaArchiviazioni(Doc docOrgine, Doc docDestinazione, Persona persona) {
        List<ArchivioDoc> archiviazioniOrigine = docOrgine.getArchiviDocList();
        Map<String, List<String>> infoArchiviNonCopiati = new HashMap();
        List<String> archiviChiusi = new ArrayList();
        List<String> archiviIter = new ArrayList();
        List<String> archiviSenzaPermesso = new ArrayList();
        for (ArchivioDoc archiviazioneOrgine : archiviazioniOrigine) {
            Archivio archivio = archiviazioneOrgine.getIdArchivio();
            // Se è eliminata faccio finta che non ci sia, non deve essere informato l'utente
            if (archiviazioneOrgine.getDataEliminazione() == null) {
                boolean personHasAtLeastThisPermissionOnTheArchive = scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), archivio.getId(), PermessoArchivio.DecimalePredicato.MODIFICA);
                if (personHasAtLeastThisPermissionOnTheArchive) {
                    if (archivio.getStato().equals(Archivio.StatoArchivio.APERTO)) {
                        if (archivio.getIdIter() == null) {
                            ArchivioDoc newArchivioDoc = new ArchivioDoc(archivio, docDestinazione, persona);
                            archivioDocRepository.save(newArchivioDoc);
                        } else {
                            archiviIter.add(scriptaArchiviUtils.getNomeCompletoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio));
                        }
                    } else {
                        archiviChiusi.add(scriptaArchiviUtils.getNomeCompletoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio));
                    }
                } else {
                    archiviSenzaPermesso.add(scriptaArchiviUtils.getNomeCompletoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio));
                }
            }
        }
        infoArchiviNonCopiati.put(MotivazioneEsclusione.ARCHIVI_ITER.toString(), archiviIter);
        infoArchiviNonCopiati.put(MotivazioneEsclusione.ARCHIVI_CHIUSI.toString(), archiviChiusi);
        infoArchiviNonCopiati.put(MotivazioneEsclusione.SENZA_PERMESSO.toString(), archiviSenzaPermesso);
        return infoArchiviNonCopiati;
    }
}
