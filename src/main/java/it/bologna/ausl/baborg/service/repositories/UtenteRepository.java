package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QUtente;
import it.bologna.ausl.baborg.model.entities.Utente;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "utente", defaultProjection = UtenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "utente", path = "utente", exported = false, excerptProjection = UtenteWithPlainFields.class)
public interface UtenteRepository extends
        NextSdrQueryDslRepository<Utente, Integer, QUtente>,
        JpaRepository<Utente, Integer> {
}
