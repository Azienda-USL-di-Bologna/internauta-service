package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QStrutturaUnificata;
import it.bologna.ausl.baborg.model.entities.StrutturaUnificata;
import it.bologna.ausl.baborg.model.entities.projections.generated.StrutturaUnificataWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "strutturaunificata", path = "strutturaunificata", exported = true, excerptProjection = StrutturaUnificataWithPlainFields.class)
public interface StrutturaUnificataRepository extends
        CustomQueryDslRepository<StrutturaUnificata, Integer, QStrutturaUnificata>,
        JpaRepository<StrutturaUnificata, Integer> {
}
