package it.bologna.ausl.internauta.service.repositories.configurazione;

import it.bologna.ausl.model.entities.configurazione.FirmePersona;
import it.bologna.ausl.model.entities.configurazione.QFirmePersona;
import it.bologna.ausl.model.entities.configurazione.projections.generated.FirmePersonaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${configurazione.mapping.url.root}/firmepersona", defaultProjection = FirmePersonaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "firmepersona", path = "firmepersona", exported = false, excerptProjection = FirmePersonaWithPlainFields.class)
public interface FirmePersonaRepository extends
        NextSdrQueryDslRepository<FirmePersona, String, QFirmePersona>,
        JpaRepository<FirmePersona, String> {
}
