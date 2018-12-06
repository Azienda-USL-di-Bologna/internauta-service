package it.bologna.ausl.internauta.service.repositories.configurazione;

import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.configuration.QParametroAziende;
import it.bologna.ausl.model.entities.configuration.projections.generated.ParametroAziendeWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${configurazione.mapping.url.root}/parametroaziende", defaultProjection = ParametroAziendeWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "parametroaziende", path = "parametroaziende", exported = false, excerptProjection = ParametroAziendeWithPlainFields.class)
public interface ParametroAziendeRepository extends
        NextSdrQueryDslRepository<ParametroAziende, Integer, QParametroAziende>,
        JpaRepository<ParametroAziende, Integer> {
}
