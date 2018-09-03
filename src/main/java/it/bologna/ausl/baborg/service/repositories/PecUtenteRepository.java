package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QPecUtente;
import it.bologna.ausl.baborg.model.entities.PecUtente;
import it.bologna.ausl.baborg.model.entities.projections.generated.PecUtenteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "pecutente", defaultProjection = PecUtenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pecutente", path = "pecutente", exported = false, excerptProjection = PecUtenteWithPlainFields.class)
public interface PecUtenteRepository extends
        NextSdrQueryDslRepository<PecUtente, Integer, QPecUtente>,
        JpaRepository<PecUtente, Integer> {
}
