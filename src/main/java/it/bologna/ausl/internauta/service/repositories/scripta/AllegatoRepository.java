package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.projections.generated.AllegatoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/allegato", defaultProjection = AllegatoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "allegato", path = "allegato", exported = false, excerptProjection = AllegatoWithPlainFields.class)
public interface AllegatoRepository extends
        NextSdrQueryDslRepository<Allegato, Integer, QAllegato>,
        JpaRepository<Allegato, Integer> {
}
