package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.QSequenzaRegistro;
import it.bologna.ausl.model.entities.scripta.SequenzaRegistro;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/sequenzaragistro", defaultProjection = SequenzaRegistro.class)
@RepositoryRestResource(collectionResourceRel = "sequenzaragistro", path = "sequenzaragistro", exported = false, excerptProjection = SequenzaRegistro.class)
public interface SequenzaRegistroRepository extends
        NextSdrQueryDslRepository<SequenzaRegistro, Integer, QSequenzaRegistro>,
        JpaRepository<SequenzaRegistro, Integer> {
}
