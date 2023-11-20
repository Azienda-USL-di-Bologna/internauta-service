package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.FrequenzaUtilizzoArchivio;
import it.bologna.ausl.model.entities.scripta.QFrequenzaUtilizzoArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.FrequenzaUtilizzoArchivioWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/frequenzautilizzoarchivio", defaultProjection = FrequenzaUtilizzoArchivioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "frequenzautilizzoarchivio", path = "frequenzautilizzoarchivio", exported = false, excerptProjection = FrequenzaUtilizzoArchivioWithPlainFields.class)
public interface FrequenzaUtilizzoArchivioRepository extends
        NextSdrQueryDslRepository<FrequenzaUtilizzoArchivio, Integer, QFrequenzaUtilizzoArchivio>,
        JpaRepository<FrequenzaUtilizzoArchivio, Integer> {
}
