package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.projections.generated.StoricoRelazioneWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/storicorelazione", defaultProjection = StoricoRelazioneWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "storicorelazione", path = "storicorelazione", exported = false, excerptProjection = StoricoRelazioneWithPlainFields.class)
public interface StoricoRelazioneRepository extends
        NextSdrQueryDslRepository<StoricoRelazione, Integer, QStoricoRelazione>,
        JpaRepository<StoricoRelazione, Integer> {
}
