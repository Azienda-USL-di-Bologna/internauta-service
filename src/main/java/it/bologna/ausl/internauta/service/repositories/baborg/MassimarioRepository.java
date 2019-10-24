package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.Massimario;
import it.bologna.ausl.model.entities.baborg.QMassimario;
import it.bologna.ausl.model.entities.baborg.projections.generated.MassimarioWithTitoli;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/massimario", defaultProjection = MassimarioWithTitoli.class)
@RepositoryRestResource(collectionResourceRel = "massimario", path = "massimario", exported = false, excerptProjection = MassimarioWithTitoli.class)
public interface MassimarioRepository extends
        NextSdrQueryDslRepository<Massimario, Integer, QMassimario>,
        JpaRepository<Massimario, Integer> {
}
