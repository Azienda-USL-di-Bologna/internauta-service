package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QRuolo;
import it.bologna.ausl.baborg.model.entities.Ruolo;
import it.bologna.ausl.baborg.model.entities.projections.generated.RuoloWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@RepositoryRestResource(collectionResourceRel = "ruolo", path = "ruolo", exported = false, excerptProjection = RuoloWithPlainFields.class)
public interface RuoloRepository extends
        CustomQueryDslRepository<Ruolo, Integer, QRuolo>,
        JpaRepository<Ruolo, Integer> {
}
