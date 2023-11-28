package it.bologna.ausl.internauta.service.repositories.configurazione;

import it.bologna.ausl.model.entities.configurazione.DominioFirma;
import it.bologna.ausl.model.entities.configurazione.QDominioFirma;
import it.bologna.ausl.model.entities.configurazione.projections.generated.DominioFirmaWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${configurazione.mapping.url.root}/dominiofirma", defaultProjection = DominioFirmaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "dominiofirma", path = "dominiofirma", exported = false, excerptProjection = DominioFirmaWithPlainFields.class)
public interface DominoiFirmaRepository extends
        NextSdrQueryDslRepository<DominioFirma, String, QDominioFirma>,
        JpaRepository<DominioFirma, String> {
}
