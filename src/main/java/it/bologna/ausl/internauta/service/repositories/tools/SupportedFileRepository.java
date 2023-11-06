package it.bologna.ausl.internauta.service.repositories.tools;

import it.bologna.ausl.model.entities.tools.QSupportedFile;
import it.bologna.ausl.model.entities.tools.SupportedFile;
import it.bologna.ausl.model.entities.tools.projections.generated.SupportedFileWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.math.BigInteger;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${tools.mapping.url.root}/supportedfile", defaultProjection = SupportedFileWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "supportedfile", path = "supportedfile", exported = false, excerptProjection = SupportedFileWithPlainFields.class)
public interface SupportedFileRepository extends
        NextSdrQueryDslRepository<SupportedFile, BigInteger, QSupportedFile>,
        JpaRepository<SupportedFile, BigInteger> {

}
