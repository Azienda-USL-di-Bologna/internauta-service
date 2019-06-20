package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/utente", defaultProjection = UtenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "utente", path = "utente", exported = false, excerptProjection = UtenteWithPlainFields.class)
public interface UtenteRepository extends
        NextSdrQueryDslRepository<Utente, Integer, QUtente>,
        JpaRepository<Utente, Integer> {
    
}
