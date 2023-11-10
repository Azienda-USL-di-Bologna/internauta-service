package it.bologna.ausl.internauta.service.repositories.permessi;

import org.springframework.stereotype.Component;
import it.bologna.ausl.model.entities.permessi.QAmbitoSemantico;
import it.bologna.ausl.model.entities.permessi.AmbitoSemantico;
import it.bologna.ausl.model.entities.permessi.projections.generated.AmbitoSemanticoWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@Component("AmbitoSemanticoInternauta")
@NextSdrRepository(repositoryPath = "${permessi.mapping.url.root}/ambitosemantico", defaultProjection = AmbitoSemanticoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "ambitosemantico", path = "ambitosemantico", exported = false, excerptProjection = AmbitoSemanticoWithPlainFields.class)
public interface AmbitoSemanticoRepository extends
        NextSdrQueryDslRepository<AmbitoSemantico, Integer, QAmbitoSemantico>,
        JpaRepository<AmbitoSemantico, Integer> {

}
