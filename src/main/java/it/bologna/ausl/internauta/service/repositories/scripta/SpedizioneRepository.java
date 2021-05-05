package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.scripta.QSpedizione;
import it.bologna.ausl.model.entities.scripta.projections.generated.SpedizioneWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/spedizione", defaultProjection = SpedizioneWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "spedizione", path = "spedizione", exported = false, excerptProjection = SpedizioneWithPlainFields.class)
public interface SpedizioneRepository extends
        NextSdrQueryDslRepository<Spedizione, Integer, QSpedizione>,
        JpaRepository<Spedizione, Integer> {
}
