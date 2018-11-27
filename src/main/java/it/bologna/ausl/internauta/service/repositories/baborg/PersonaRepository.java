package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/persona", defaultProjection = PersonaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "persona", path = "persona", exported = false, excerptProjection = PersonaWithPlainFields.class)
public interface PersonaRepository extends
        NextSdrQueryDslRepository<Persona, Integer, QPersona>,
        JpaRepository<Persona, Integer> {
}
