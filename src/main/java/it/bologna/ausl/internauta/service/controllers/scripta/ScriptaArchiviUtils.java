
package it.bologna.ausl.internauta.service.controllers.scripta;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ScriptaArchiviUtils {
    
    @Autowired
    private PermessoArchivioRepository permessoArchivioRepository;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    
    /**
     * Restituisce true se la persona ha ALMENO il permesso sull'archivio
     */
    public boolean personHasAtLeastThisPermissionOnTheArchive(Integer idPersona, Integer idArchivio, PermessoArchivio.DecimalePredicato permesso) {
        QPermessoArchivio permessoArchivio = QPermessoArchivio.permessoArchivio;
        BooleanExpression filterUserhasPermission = permessoArchivio.idArchivioDetail.id.eq(idArchivio).and(
                permessoArchivio.idPersona.id.eq(idPersona).and(
                permessoArchivio.bit.goe(permesso.getValue()))
        );
        Optional<PermessoArchivio> findOne = permessoArchivioRepository.findOne(filterUserhasPermission);
        return findOne.isPresent();
    }
    
    /**
     * Torna il nome dell'archivio così composto: [numerazione gerarchica] oggetto dell'archivio radice.In caso di azienda con facicoliParlanti il nome sarà solo: [numerazione gerarchica]
     * @param archivio
     * @return 
     */
    public String getNomeCompletoArchivioPerVisualizzazioneDiSicurezzaClassica(Archivio archivio) {
        return "[" + archivio.getNumerazioneGerarchica() + "] " + getOggettoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio);
    }
    
    /**
     * Torna l'oggetto dell'archivio radice. Torna solo null se azienda con fascioli parlanti
     * @param archivio
     * @return 
     */
    public String getOggettoArchivioPerVisualizzazioneDiSicurezzaClassica(Archivio archivio) {
        List<ParametroAziende> fascicoliParlanti = cachedEntities.getParameters("fascicoliParlanti", archivio.getIdAzienda().getId());
        if (fascicoliParlanti != null && !fascicoliParlanti.isEmpty() && parametriAziende.getValue(fascicoliParlanti.get(0), Boolean.class)) {
            return "";
        } else {
            Archivio archivioRadice = null;
            if (archivio.getLivello().equals(1)) {
                archivioRadice = archivio;
            } else {
                archivioRadice = archivio.getIdArchivioRadice();
            }
            return archivioRadice.getOggetto();
        }
    }
}
