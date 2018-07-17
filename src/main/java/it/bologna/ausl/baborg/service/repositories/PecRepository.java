package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.Pec;
import it.bologna.ausl.baborg.model.entities.QPec;
import it.bologna.ausl.baborg.model.entities.projections.generated.PecWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "pec", path = "pec", exported = true, excerptProjection = PecWithPlainFields.class)
public interface PecRepository extends
        CustomQueryDslRepository<Pec, Integer, QPec>,
        JpaRepository<Pec, Integer> {
}
