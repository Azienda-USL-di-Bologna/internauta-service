package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QPecUtente;
import it.bologna.ausl.model.entities.baborg.PecUtente;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecUtenteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/pecutente", defaultProjection = PecUtenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pecutente", path = "pecutente", exported = false, excerptProjection = PecUtenteWithPlainFields.class)
public interface PecUtenteRepository extends
        NextSdrQueryDslRepository<PecUtente, Integer, QPecUtente>,
        JpaRepository<PecUtente, Integer> {
}
