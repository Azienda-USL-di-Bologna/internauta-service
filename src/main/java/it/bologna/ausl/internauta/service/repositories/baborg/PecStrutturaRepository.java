package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QPecStruttura;
import it.bologna.ausl.model.entities.baborg.PecStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecStrutturaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/pecstruttura", defaultProjection = PecStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pecstruttura", path = "pecstruttura", exported = false, excerptProjection = PecStrutturaWithPlainFields.class)
public interface PecStrutturaRepository extends
        NextSdrQueryDslRepository<PecStruttura, Integer, QPecStruttura>,
        JpaRepository<PecStruttura, Integer> {
}
