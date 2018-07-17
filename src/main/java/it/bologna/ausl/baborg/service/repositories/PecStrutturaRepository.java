package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QPecStruttura;
import it.bologna.ausl.baborg.model.entities.PecStruttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.PecStrutturaWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "pecstruttura", path = "pecstruttura", exported = true, excerptProjection = PecStrutturaWithPlainFields.class)
public interface PecStrutturaRepository extends
        CustomQueryDslRepository<PecStruttura, Integer, QPecStruttura>,
        JpaRepository<PecStruttura, Integer> {
}
