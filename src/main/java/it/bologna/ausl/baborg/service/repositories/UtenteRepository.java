package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QUtente;
import it.bologna.ausl.baborg.model.entities.Utente;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@RepositoryRestResource(collectionResourceRel = "utente", path = "utente", exported = false, excerptProjection = UtenteWithPlainFields.class)
public interface UtenteRepository extends
        CustomQueryDslRepository<Utente, Integer, QUtente>,
        JpaRepository<Utente, Integer> {
}
