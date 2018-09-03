package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QStrutturaUnificata;
import it.bologna.ausl.baborg.model.entities.StrutturaUnificata;
import it.bologna.ausl.baborg.model.entities.projections.generated.StrutturaUnificataWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "strutturaunificata", defaultProjection = StrutturaUnificataWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "strutturaunificata", path = "strutturaunificata", exported = false, excerptProjection = StrutturaUnificataWithPlainFields.class)
public interface StrutturaUnificataRepository extends
        NextSdrQueryDslRepository<StrutturaUnificata, Integer, QStrutturaUnificata>,
        JpaRepository<StrutturaUnificata, Integer> {
}
