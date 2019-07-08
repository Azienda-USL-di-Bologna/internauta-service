package it.bologna.ausl.internauta.service.repositories.ribaltoneutils;


import it.bologna.ausl.model.entities.ribaltoneutils.StoricoAttivazione;
import it.bologna.ausl.model.entities.ribaltoneutils.QStoricoAttivazione;
import it.bologna.ausl.model.entities.ribaltoneutils.projections.generated.StoricoAttivazioneWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${ribaltoneutils.mapping.url.root}/storicoattivazione", defaultProjection = StoricoAttivazioneWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "storicoattivazione", path = "storicoattivazione", exported = false, excerptProjection = StoricoAttivazioneWithPlainFields.class)
public interface StoricoAttivazioneRepository extends
        NextSdrQueryDslRepository<StoricoAttivazione, Integer, QStoricoAttivazione>,
        JpaRepository<StoricoAttivazione, Integer> {
}
