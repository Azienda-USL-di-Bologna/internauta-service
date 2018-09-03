package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.Pec;
import it.bologna.ausl.baborg.model.entities.QPec;
import it.bologna.ausl.baborg.model.entities.projections.generated.PecWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "pec", defaultProjection = PecWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pec", path = "pec", exported = false, excerptProjection = PecWithPlainFields.class)
public interface PecRepository extends
        NextSdrQueryDslRepository<Pec, Integer, QPec>,
        JpaRepository<Pec, Integer> {
}
