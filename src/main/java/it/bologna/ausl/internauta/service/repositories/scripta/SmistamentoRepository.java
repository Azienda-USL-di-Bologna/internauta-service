package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Smistamento;
import it.bologna.ausl.model.entities.scripta.QSmistamento;
import it.bologna.ausl.model.entities.scripta.projections.generated.SmistamentoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/smistamento", defaultProjection = SmistamentoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "smistamento", path = "smistamento", exported = false, excerptProjection = SmistamentoWithPlainFields.class)
public interface SmistamentoRepository extends
        NextSdrQueryDslRepository<Smistamento, Integer, QSmistamento>,
        JpaRepository<Smistamento, Integer> {
}
