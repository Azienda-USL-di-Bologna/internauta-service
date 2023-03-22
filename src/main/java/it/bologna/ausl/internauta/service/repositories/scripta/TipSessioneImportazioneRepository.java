package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.QTipSessioneImportazione;
import it.bologna.ausl.model.entities.scripta.TipSessioneImportazione;
import it.bologna.ausl.model.entities.scripta.projections.generated.TipSessioneImportazioneWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/tipsessioneimportazione", defaultProjection = TipSessioneImportazioneWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "tipsessioneimportazione", path = "tipsessioneimportazione", exported = false, excerptProjection = TipSessioneImportazioneWithPlainFields.class)
public interface TipSessioneImportazioneRepository extends
        NextSdrQueryDslRepository<TipSessioneImportazione, Long, QTipSessioneImportazione>,
        JpaRepository<TipSessioneImportazione, Long> {
};
