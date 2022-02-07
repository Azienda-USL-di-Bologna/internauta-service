package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.AttoreArchivioWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/attorearchivio", defaultProjection = AttoreArchivioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "attorearchivio", path = "attorearchivio", exported = false, excerptProjection = AttoreArchivioWithPlainFields.class)
public interface AttoreArchivioRepository extends
        NextSdrQueryDslRepository<AttoreArchivio, Integer, QAttoreArchivio>,
        JpaRepository<AttoreArchivio, Integer> {
}
