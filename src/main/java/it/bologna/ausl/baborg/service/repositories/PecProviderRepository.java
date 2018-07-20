package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.PecProvider;
import it.bologna.ausl.baborg.model.entities.QPecProvider;
import it.bologna.ausl.baborg.model.entities.projections.generated.PecProviderWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@RepositoryRestResource(collectionResourceRel = "pecprovider", path = "pecprovider", exported = false, excerptProjection = PecProviderWithPlainFields.class)
public interface PecProviderRepository extends
        CustomQueryDslRepository<PecProvider, Integer, QPecProvider>,
        JpaRepository<PecProvider, Integer> {
}
