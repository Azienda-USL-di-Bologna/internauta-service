package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.QTipImportazioneDocumento;
import it.bologna.ausl.model.entities.scripta.TipImportazioneDocumento;
import it.bologna.ausl.model.entities.scripta.projections.generated.TipImportazioneDocumentoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/tipimportazionedocumento", defaultProjection = TipImportazioneDocumentoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "tipimportazionedocumento", path = "tipimportazionedocumento", exported = false, excerptProjection = TipImportazioneDocumentoWithPlainFields.class)
public interface TipImportazioneDocumentoRepository extends
        NextSdrQueryDslRepository<TipImportazioneDocumento, Long, QTipImportazioneDocumento>,
        JpaRepository<TipImportazioneDocumento, Long> {
};
