package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.IdpEntityId;
import it.bologna.ausl.baborg.model.entities.QIdpEntityId;
import it.bologna.ausl.baborg.model.entities.projections.generated.IdpEntityIdWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "idpentityid", path = "idpentityid", exported = true, excerptProjection = IdpEntityIdWithPlainFields.class)
public interface IdpEntityIdRepository extends
        CustomQueryDslRepository<IdpEntityId, String, QIdpEntityId>,
        JpaRepository<IdpEntityId, String> {
}
