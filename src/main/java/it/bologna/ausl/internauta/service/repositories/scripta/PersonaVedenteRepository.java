package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.PersonaVedente;
import it.bologna.ausl.model.entities.scripta.QPersonaVedente;
import it.bologna.ausl.model.entities.scripta.projections.generated.PersonaVedenteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/personavedente", defaultProjection = PersonaVedenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "personavedente", path = "personavedente", exported = false, excerptProjection = PersonaVedenteWithPlainFields.class)
public interface PersonaVedenteRepository extends
        NextSdrQueryDslRepository<PersonaVedente, Long, QPersonaVedente>, 
        JpaRepository<PersonaVedente, Long> {

}
