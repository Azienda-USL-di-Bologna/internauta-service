package it.bologna.ausl.internauta.service.repositories.tip;

import it.bologna.ausl.model.entities.tip.QImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.projections.generated.ImportazioneDocumentoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${tip.mapping.url.root}/importazionedocumento", defaultProjection = ImportazioneDocumentoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "importazionedocumento", path = "importazionedocumento", exported = false, excerptProjection = ImportazioneDocumentoWithPlainFields.class)
public interface ImportazioneDocumentoRepository extends
        NextSdrQueryDslRepository<ImportazioneDocumento, Long, QImportazioneDocumento>, 
        JpaRepository<ImportazioneDocumento, Long> {
};
