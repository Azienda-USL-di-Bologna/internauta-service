package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.ArchivioDiInteresse;
import it.bologna.ausl.model.entities.scripta.ArchivioRecente;
import it.bologna.ausl.model.entities.scripta.QArchivioDiInteresse;
import it.bologna.ausl.model.entities.scripta.QArchivioRecente;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioRecenteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 *
 * @author Matteo Next
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/archivirecenti", defaultProjection = ArchivioRecenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "archiviorecente", path = "archiviorecente", exported = false, excerptProjection = ArchivioRecenteWithPlainFields.class)
public interface ArchiviRecentiRepository extends
        NextSdrQueryDslRepository<ArchivioRecente, Integer, QArchivioRecente>,
        JpaRepository<ArchivioRecente, Integer> {

    @Query(value = "SELECT * FROM scripta.archivi_recenti a WHERE id_archivio = ?1 and id_persona = ?2 ", nativeQuery = true)
    public Optional<ArchivioRecente> getArchivioFromPersonaAndArchivio(
            Integer idArchivio,
            Integer idPersona
    );

    @Query(value = "select id_archivio from scripta.archivi_recenti ar where id_persona = ?1 order by data_recentezza desc", nativeQuery = true)
    public Optional<Integer[]> getArchiviFromPersona(
            Integer idPersona);

}
