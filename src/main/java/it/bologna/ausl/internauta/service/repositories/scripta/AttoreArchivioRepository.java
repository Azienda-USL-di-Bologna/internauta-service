package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sostizionemassivaresponsabilearchivi.SostizioneMassivaResponsabileInfo;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.AttoreArchivioWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/attorearchivio", defaultProjection = AttoreArchivioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "attorearchivio", path = "attorearchivio", exported = false, excerptProjection = AttoreArchivioWithPlainFields.class)
public interface AttoreArchivioRepository extends
        NextSdrQueryDslRepository<AttoreArchivio, Integer, QAttoreArchivio>,
        JpaRepository<AttoreArchivio, Integer> {
    
    
    @Query(value = 
        " WITH responsabili_old AS ("
                + " DELETE FROM scripta.attori_archivi aa"
                + " USING baborg.persone p"
                + " WHERE aa.id_archivio in (?1) AND aa.id_persona != ?2 AND aa.ruolo = 'RESPONSABILE'\\:\\:scripta.ruolo_attore_archivio AND p.id = aa.id_persona"
                + " RETURNING aa.id_archivio, aa.id_persona, p.descrizione"
        + "), responsabili_new AS ("
                + " INSERT INTO scripta.attori_archivi (id_archivio, id_persona, id_struttura, ruolo)"
                + " SELECT DISTINCT i.id_archivio, ?2, ?3, 'RESPONSABILE'\\:\\:scripta.ruolo_attore_archivio FROM responsabili_old i"
        + ") SELECT DISTINCT id_archivio, id_persona as id_persona_old_responsabile, descrizione as descrizione_old_responsabile FROM responsabili_old", nativeQuery = true)
    public List<Map<String, Object>> sostituisciResponsabile(
            Integer[] idsArchivi,
            Integer idPersonaNuovoResponsabile,
            Integer idStrutturaResponsabile
    );
    
    @Query(value = 
        " UPDATE scripta.attori_archivi SET id_struttura = ?3" +
        " WHERE id_archivio in (?1) AND id_persona = ?2 AND id_struttura != ?3 AND ruolo = 'RESPONSABILE'\\:\\:scripta.ruolo_attore_archivio RETURNING id_archivio", nativeQuery = true)
    public Set<Integer> aggiornaStrutturaResponsabile(
            Integer[] idsArchivi,
            Integer idPersonaResponsabile,
            Integer idStrutturaResponsabile
    );
}
