package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDocWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/archiviodoc", defaultProjection = ArchivioDocWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "archiviodoc", path = "archiviodoc", exported = false, excerptProjection = ArchivioDocWithPlainFields.class)
public interface ArchivioDocRepository extends
        NextSdrQueryDslRepository<ArchivioDoc, Integer, QArchivioDoc>,
        JpaRepository<ArchivioDoc, Integer> {
    
    @Query(value = "SELECT DISTINCT ad.id_doc " +
                    " FROM scripta.archivi_docs ad " +
                    " JOIN scripta.archivi a ON a.id = ad.id_archivio" +
                    " WHERE a.id_archivio_radice = ANY (?1)",
            nativeQuery = true)
    public List<Integer> getIdDocsDaArchiviRadice(Set<Integer> idArchiviRadice);
}
