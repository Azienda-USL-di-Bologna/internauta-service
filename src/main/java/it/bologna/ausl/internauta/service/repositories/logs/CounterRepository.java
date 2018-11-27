package it.bologna.ausl.internauta.service.repositories.logs;

import it.bologna.ausl.model.entities.logs.Counter;
import it.bologna.ausl.model.entities.logs.QCounter;
import it.bologna.ausl.model.entities.logs.projections.generated.CounterWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${logs.mapping.url.root}/counter", defaultProjection = CounterWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "counter", path = "counter", exported = false, excerptProjection = CounterWithPlainFields.class)
public interface CounterRepository extends
        NextSdrQueryDslRepository<Counter, Integer, QCounter>,
        JpaRepository<Counter, Integer> {
}
