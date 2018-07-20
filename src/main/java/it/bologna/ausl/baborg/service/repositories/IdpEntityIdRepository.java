package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.IdpEntityId;
import it.bologna.ausl.baborg.model.entities.QIdpEntityId;
import it.bologna.ausl.baborg.model.entities.projections.generated.IdpEntityIdWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@RepositoryRestResource(collectionResourceRel = "idpentityid", path = "idpentityid", exported = false, excerptProjection = IdpEntityIdWithPlainFields.class)
public interface IdpEntityIdRepository extends
        CustomQueryDslRepository<IdpEntityId, String, QIdpEntityId>,
        JpaRepository<IdpEntityId, String> {
}
