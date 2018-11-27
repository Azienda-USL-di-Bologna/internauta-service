package it.bologna.ausl.internauta.service.repositories.configurazione;

import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.configuration.QApplicazione;
import it.bologna.ausl.model.entities.configuration.projections.generated.ApplicazioneWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${configurazione.mapping.url.root}/applicazione", defaultProjection = ApplicazioneWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "applicazione", path = "applicazione", exported = false, excerptProjection = ApplicazioneWithPlainFields.class)
public interface ApplicazioneRepository extends
        NextSdrQueryDslRepository<Applicazione, String, QApplicazione>,
        JpaRepository<Applicazione, String> {
}
