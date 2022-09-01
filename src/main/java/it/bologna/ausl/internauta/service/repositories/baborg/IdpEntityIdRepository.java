package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.IdpEntityId;
import it.bologna.ausl.model.entities.baborg.QIdpEntityId;
import it.bologna.ausl.model.entities.baborg.projections.generated.IdpEntityIdWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/idpentityid", defaultProjection = IdpEntityIdWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "idpentityid", path = "idpentityid", exported = false, excerptProjection = IdpEntityIdWithPlainFields.class)
public interface IdpEntityIdRepository extends
        NextSdrQueryDslRepository<IdpEntityId, String, QIdpEntityId>,
        JpaRepository<IdpEntityId, String> {
}
