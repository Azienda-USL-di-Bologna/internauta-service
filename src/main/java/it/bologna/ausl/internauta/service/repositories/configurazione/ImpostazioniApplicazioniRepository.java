package it.bologna.ausl.internauta.service.repositories.configurazione;

import it.bologna.ausl.model.entities.configuration.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configuration.QImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configuration.projections.generated.ImpostazioniApplicazioniWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author gdm
 */
@NextSdrRepository(repositoryPath = "${configurazione.mapping.url.root}/impostazioniapplicazioni", defaultProjection = ImpostazioniApplicazioniWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "impostazioniapplicazioni", path = "impostazioniapplicazioni", exported = false, excerptProjection = ImpostazioniApplicazioniWithPlainFields.class)
public interface ImpostazioniApplicazioniRepository extends
        NextSdrQueryDslRepository<ImpostazioniApplicazioni, String, QImpostazioniApplicazioni>,
        JpaRepository<ImpostazioniApplicazioni, String> {
}
