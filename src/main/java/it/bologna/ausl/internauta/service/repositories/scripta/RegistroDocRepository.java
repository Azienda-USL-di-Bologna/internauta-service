package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.QRegistroDoc;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.RegistroDocWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/registrodoc", defaultProjection = RegistroDocWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "registrodoc", path = "registrodoc", exported = false, excerptProjection = RegistroDocWithPlainFields.class)
public interface RegistroDocRepository extends
        NextSdrQueryDslRepository<RegistroDoc, Integer, QRegistroDoc>,
        JpaRepository<RegistroDoc, Integer> {
}
