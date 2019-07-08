package it.bologna.ausl.internauta.service.repositories.scrivania;

import it.bologna.ausl.model.entities.scrivania.AttivitaFatta;
import it.bologna.ausl.model.entities.scrivania.QAttivitaFatta;
import it.bologna.ausl.model.entities.scrivania.projections.generated.AttivitaFattaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scrivania.mapping.url.root}/attivitafatta", defaultProjection = AttivitaFattaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "attivitafatta", path = "attivitafatta", exported = false, excerptProjection = AttivitaFattaWithPlainFields.class)
public interface AttivitaFatteRepository extends
        NextSdrQueryDslRepository<AttivitaFatta, Integer, QAttivitaFatta>,
        JpaRepository<AttivitaFatta, Integer> {
}
