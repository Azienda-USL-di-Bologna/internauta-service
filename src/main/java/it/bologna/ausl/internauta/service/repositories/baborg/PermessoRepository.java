package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QPermesso;
import it.bologna.ausl.model.entities.baborg.Permesso;
import it.bologna.ausl.model.entities.baborg.projections.generated.PermessoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "permesso", baseUrl = "${baborg.mapping.url.root}", defaultProjection = PermessoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "permesso", path = "permesso", exported = false, excerptProjection = PermessoWithPlainFields.class)
public interface PermessoRepository extends
        NextSdrQueryDslRepository<Permesso, Integer, QPermesso>,
        JpaRepository<Permesso, Integer> {
}
