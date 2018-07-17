package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QTipoPermesso;
import it.bologna.ausl.baborg.model.entities.TipoPermesso;
import it.bologna.ausl.baborg.model.entities.projections.generated.TipoPermessoWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "tipopermesso", path = "tipopermesso", exported = true, excerptProjection = TipoPermessoWithPlainFields.class)
public interface TipoPermessoRepository extends
        CustomQueryDslRepository<TipoPermesso, Integer, QTipoPermesso>,
        JpaRepository<TipoPermesso, Integer> {
}
