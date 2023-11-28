package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.Profili;
import it.bologna.ausl.model.entities.baborg.QProfili;
import it.bologna.ausl.model.entities.baborg.projections.generated.ProfiliWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/profili", defaultProjection = ProfiliWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "profili", path = "profili", exported = false, excerptProjection = ProfiliWithPlainFields.class)
public interface ProfiliRepository extends
        NextSdrQueryDslRepository<Profili, String, QProfili>,
        JpaRepository<Profili, String> {
    
}
