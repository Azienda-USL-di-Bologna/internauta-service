package it.bologna.ausl.internauta.service.repositories.lotti;

import it.bologna.ausl.model.entities.lotti.projections.generated.ComponenteWithIdRuolo;
import it.bologna.ausl.model.entities.lotti.Componente;
import it.bologna.ausl.model.entities.lotti.QComponente;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author QB
 */
@NextSdrRepository(repositoryPath = "${lotti.mapping.url.root}/partecipante", defaultProjection = ComponenteWithIdRuolo.class)
@RepositoryRestResource(collectionResourceRel = "partecipante", path = "partecipante", exported = false, excerptProjection = ComponenteWithIdRuolo.class)
public interface PartecipanteRepository extends 
        NextSdrQueryDslRepository<Componente, Integer, QComponente>, 
        JpaRepository<Componente, Integer> {}
