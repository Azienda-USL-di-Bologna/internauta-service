package it.bologna.ausl.internauta.service.repositories.messaggero;

import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QAmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.projections.generated.AmministrazioneMessaggioWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@NextSdrRepository(repositoryPath = "${messaggero.mapping.url.root}/amministrazionemessaggio", defaultProjection = AmministrazioneMessaggioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "amministrazionemessaggio", path = "amministrazionemessaggio", exported = false, excerptProjection = AmministrazioneMessaggioWithPlainFields.class)
public interface AmministrazioneMessaggioRepository extends
        NextSdrQueryDslRepository<AmministrazioneMessaggio, Integer, QAmministrazioneMessaggio>, 
        JpaRepository<AmministrazioneMessaggio, Integer> {
}