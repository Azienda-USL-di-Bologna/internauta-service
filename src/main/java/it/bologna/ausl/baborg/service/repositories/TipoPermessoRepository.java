package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QTipoPermesso;
import it.bologna.ausl.baborg.model.entities.TipoPermesso;
import it.bologna.ausl.baborg.model.entities.projections.generated.TipoPermessoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "tipopermesso", defaultProjection = TipoPermessoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "tipopermesso", path = "tipopermesso", exported = false, excerptProjection = TipoPermessoWithPlainFields.class)
public interface TipoPermessoRepository extends
        NextSdrQueryDslRepository<TipoPermesso, Integer, QTipoPermesso>,
        JpaRepository<TipoPermesso, Integer> {
}
