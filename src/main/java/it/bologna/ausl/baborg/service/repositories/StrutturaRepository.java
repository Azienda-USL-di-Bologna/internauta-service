package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QStruttura;
import it.bologna.ausl.baborg.model.entities.Struttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.StrutturaWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "struttura", path = "struttura", exported = true, excerptProjection = StrutturaWithPlainFields.class)
public interface StrutturaRepository extends
        CustomQueryDslRepository<Struttura, Integer, QStruttura>,
        JpaRepository<Struttura, Integer> {
}
