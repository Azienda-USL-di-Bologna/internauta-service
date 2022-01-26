package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDocWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/archiviodoc", defaultProjection = ArchivioDocWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "archiviodoc", path = "archiviodoc", exported = false, excerptProjection = ArchivioDocWithPlainFields.class)
public interface ArchivioDocRepository extends
        NextSdrQueryDslRepository<ArchivioDoc, Integer, QArchivioDoc>,
        JpaRepository<ArchivioDoc, Integer> {
}
