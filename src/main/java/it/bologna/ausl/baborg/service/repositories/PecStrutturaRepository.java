package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QPecStruttura;
import it.bologna.ausl.baborg.model.entities.PecStruttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.PecStrutturaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "pecstruttura", defaultProjection = PecStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pecstruttura", path = "pecstruttura", exported = false, excerptProjection = PecStrutturaWithPlainFields.class)
public interface PecStrutturaRepository extends
        NextSdrQueryDslRepository<PecStruttura, Integer, QPecStruttura>,
        JpaRepository<PecStruttura, Integer> {
}
