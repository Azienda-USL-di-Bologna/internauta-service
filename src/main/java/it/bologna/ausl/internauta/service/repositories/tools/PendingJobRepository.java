package it.bologna.ausl.internauta.service.repositories.tools;

import it.bologna.ausl.model.entities.tools.QPendingJob;
import it.bologna.ausl.model.entities.tools.PendingJob;
import it.bologna.ausl.model.entities.tools.projections.generated.PendingJobWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.math.BigInteger;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${tools.mapping.url.root}/pendingjob", defaultProjection = PendingJobWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pendingjob", path = "pendingjob", exported = false, excerptProjection = PendingJobWithPlainFields.class)
public interface PendingJobRepository extends
        NextSdrQueryDslRepository<PendingJob, BigInteger, QPendingJob>,
        JpaRepository<PendingJob, BigInteger> {

}
