package it.bologna.ausl.internauta.service.repositories.logs;

import it.bologna.ausl.model.entities.logs.MassiveActionLog;
import it.bologna.ausl.model.entities.logs.QMassiveActionLog;
import it.bologna.ausl.model.entities.logs.projections.generated.MassiveActionLogWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${logs.mapping.url.root}/massiveactionlog", defaultProjection = MassiveActionLogWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "massiveactionlog", path =  "massiveactionlog", exported = false, excerptProjection = MassiveActionLogWithPlainFields.class)
public interface MassiveActionLogRepository extends
        NextSdrQueryDslRepository<MassiveActionLog, Integer, QMassiveActionLog>,
        JpaRepository<MassiveActionLog, Integer> {
}