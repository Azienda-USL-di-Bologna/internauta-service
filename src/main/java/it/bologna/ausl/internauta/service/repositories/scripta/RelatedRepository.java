package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.QRelated;
import it.bologna.ausl.model.entities.scripta.projections.generated.RelatedWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/related", defaultProjection = RelatedWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "related", path = "related", exported = false, excerptProjection = RelatedWithPlainFields.class)
public interface RelatedRepository extends
        NextSdrQueryDslRepository<Related, Integer, QRelated>,
        JpaRepository<Related, Integer> {
}
