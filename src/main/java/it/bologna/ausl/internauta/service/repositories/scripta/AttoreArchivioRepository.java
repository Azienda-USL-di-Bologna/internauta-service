package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.AttoreArchivioWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                + " SELECT DISTINCT i.id_archivio, ?2, ?3, 'RESPONSABILE'\\:\\:scripta.ruolo_attore_archivio"
//                + " FROM responsabili_old i"
                + " FROM UNNEST(STRING_TO_ARRAY(?6, ',')\\:\\:integer[]) AS i(id_archivio)"
                + " WHERE NOT EXISTS (SELECT 1 FROM scripta.attori_archivi att WHERE att.id_archivio = i.id_archivio AND ruolo = 'RESPONSABILE'\\:\\:scripta.ruolo_attore_archivio)"
                + " RETURNING id_archivio, id as idAttoreArchivioNewResponsabile "
        + ") SELECT DISTINCT "
                + "n.id_archivio as \"idArchivio\", "
                + "o.id_persona as \"idPersonaOldResponsabile\", "
                + "COALESCE(o.descrizione, 'nessuno') as \"descrizioneOldResponsabile\", "
                + "?2 as \"idPersonaNewResponsabile\", "
                + "?3 as \"idStrutturaNewResponsabile\",  "
                + "?4 as \"descrizioneNewResponsabile\", "
                + "?5 as \"descrizioneStrutturaNewResponsabile\", "
                + "idAttoreArchivioNewResponsabile AS \"idAttoreArchivioNewResponsabile\", "
                + "a.numerazione_gerarchica as \"numerazioneGerarchica\""
            + " FROM responsabili_new n"
            + " LEFT JOIN responsabili_old o ON n.id_archivio = o.id_archivio"
            + " JOIN scripta.archivi a ON a.id = n.id_archivio", 
        nativeQuery = true)
    public List<Map<String, Object>> sostituisciResponsabile(
            Integer[] idsArchivi,
            Integer idPersonaNuovoResponsabile,
            Integer idStrutturaResponsabile,
            String descrizioneNewResponsabile,
            String descrizioneStrutturaNewResponsabile,
            String idsArchiviString
    );
    
    @Query(value = 
        " UPDATE scripta.attori_archivi aa"
        + " SET id_struttura = ?3"
        + " FROM scripta.archivi a, baborg.strutture s "
        + " WHERE aa.id_archivio in (?1) AND aa.id_persona = ?2 AND aa.id_struttura != ?3 AND aa.ruolo = 'RESPONSABILE'\\:\\:scripta.ruolo_attore_archivio"
        + " AND a.id = aa.id_archivio"
        + " AND aa.id_struttura = s.id"
        + " RETURNING "
                + " aa.id_archivio as \"idArchivio\", "
                + " a.numerazione_gerarchica as \"numerazioneGerarchica\", "
                + " s.id AS \"idStrutturaOldResponsabile\", "
                + " s.nome AS \"descrizioneStrutturaOldResponsabile\", "
                + " aa.id as \"idAttoreArchivioNewResponsabile\", "
                + " ?3 AS \"idStrutturaNewResponsabile\", "
                + " ?5 AS \"descrizioneStrutturaNewResponsabile\", "
                + " ?4 AS \"descrizioneNewResponsabile\" ",
        nativeQuery = true)
    public List<Map<String, Object>> aggiornaStrutturaResponsabile(
            Integer[] idsArchivi,
            Integer idPersonaResponsabile,
            Integer idStrutturaResponsabile,
            String descrizioneNewResponsabile,
            String descrizioneStrutturaNewResponsabile
    );
}
