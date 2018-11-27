package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QRuolo;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.projections.generated.RuoloWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/ruolo", defaultProjection = RuoloWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "ruolo", path = "ruolo", exported = false, excerptProjection = RuoloWithPlainFields.class)
public interface RuoloRepository extends
        NextSdrQueryDslRepository<Ruolo, Integer, QRuolo>,
        JpaRepository<Ruolo, Integer> {
}
