package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.QRegistro;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.projections.generated.RegistroWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/registro", defaultProjection = RegistroWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "registro", path = "registro", exported = false, excerptProjection = RegistroWithPlainFields.class)
public interface RegistroRepository extends
        NextSdrQueryDslRepository<Registro, Integer, QRegistro>,
        JpaRepository<Registro, Integer> {
}
