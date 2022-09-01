package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.PecProvider;
import it.bologna.ausl.model.entities.baborg.QPecProvider;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecProviderWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/pecprovider", defaultProjection = PecProviderWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pecprovider", path = "pecprovider", exported = false, excerptProjection = PecProviderWithPlainFields.class)
public interface PecProviderRepository extends
        NextSdrQueryDslRepository<PecProvider, Integer, QPecProvider>,
        JpaRepository<PecProvider, Integer> {
}
