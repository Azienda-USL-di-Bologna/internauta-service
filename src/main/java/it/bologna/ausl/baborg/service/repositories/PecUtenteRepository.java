package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QPecUtente;
import it.bologna.ausl.baborg.model.entities.PecUtente;
import it.bologna.ausl.baborg.model.entities.projections.generated.PecUtenteWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "pecutente", path = "pecutente", exported = true, excerptProjection = PecUtenteWithPlainFields.class)
public interface PecUtenteRepository extends
        CustomQueryDslRepository<PecUtente, Integer, QPecUtente>,
        JpaRepository<PecUtente, Integer> {
}
