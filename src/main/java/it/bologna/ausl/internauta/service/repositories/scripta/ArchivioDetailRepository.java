package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDetailWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/archiviodetail", defaultProjection = ArchivioDetailWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "archiviodetail", path = "archiviodetail", exported = false, excerptProjection = ArchivioDetailWithPlainFields.class)
public interface ArchivioDetailRepository extends
        NextSdrQueryDslRepository<ArchivioDetail, Integer, QArchivioDetail>,
        JpaRepository<ArchivioDetail, Integer> {

}
