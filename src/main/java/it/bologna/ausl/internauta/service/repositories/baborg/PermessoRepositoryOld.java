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
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/permessoold", defaultProjection = PermessoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "permessoold", path = "permessoold", exported = false, excerptProjection = PermessoWithPlainFields.class)
public interface PermessoRepositoryOld extends
        NextSdrQueryDslRepository<Permesso, Integer, QPermesso>,
        JpaRepository<Permesso, Integer> {
}
