package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.QRegistro;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.SequenzaRegistro;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/sequenzeragistro", defaultProjection = SequenzaRegistro.class)
@RepositoryRestResource(collectionResourceRel = "sequenzeregistri", path = "sequenzeregistri", exported = false, excerptProjection = SequenzaRegistro.class)
public interface SequenzaRegistroRepository extends
        NextSdrQueryDslRepository<SequenzaRegistro, Integer, QRegistro>,
        JpaRepository<Registro, Integer> {
}
