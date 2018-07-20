package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QPersona;
import it.bologna.ausl.baborg.model.entities.Persona;
import it.bologna.ausl.baborg.model.entities.projections.generated.PersonaWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@RepositoryRestResource(collectionResourceRel = "persona", path = "persona", exported = false, excerptProjection = PersonaWithPlainFields.class)
public interface PersonaRepository extends
        CustomQueryDslRepository<Persona, Integer, QPersona>,
        JpaRepository<Persona, Integer> {
}
