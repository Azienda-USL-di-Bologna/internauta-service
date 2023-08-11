package it.bologna.ausl.internauta.service.repositories.tip;

import it.bologna.ausl.model.entities.tip.QSessioneImportazione;
import it.bologna.ausl.model.entities.tip.projections.generated.SessioneImportazioneWithPlainFields;
import it.bologna.ausl.model.entities.tip.SessioneImportazione;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${tip.mapping.url.root}/sessioneimportazione", defaultProjection = SessioneImportazioneWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "sessioneimportazione", path = "sessioneimportazione", exported = false, excerptProjection = SessioneImportazioneWithPlainFields.class)
public interface SessioneImportazioneRepository extends
        NextSdrQueryDslRepository<SessioneImportazione, Long, QSessioneImportazione>, 
        JpaRepository<SessioneImportazione, Long> {
};
