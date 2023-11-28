package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.QAttoreDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.AttoreDocWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/attoredoc", defaultProjection = AttoreDocWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "attoredoc", path = "attoredoc", exported = false, excerptProjection = AttoreDocWithPlainFields.class)
public interface AttoreDocRepository extends
        NextSdrQueryDslRepository<AttoreDoc, Integer, QAttoreDoc>,
        JpaRepository<AttoreDoc, Integer> {
}
