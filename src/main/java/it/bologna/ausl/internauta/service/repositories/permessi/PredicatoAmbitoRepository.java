package it.bologna.ausl.internauta.service.repositories.permessi;

import org.springframework.stereotype.Component;
import it.bologna.ausl.model.entities.permessi.PredicatoAmbito;
import it.bologna.ausl.model.entities.permessi.QPredicatoAmbito;
import it.bologna.ausl.model.entities.permessi.projections.generated.PredicatoAmbitoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@Component("PredicatoAmbitoInternauta")
@NextSdrRepository(repositoryPath = "${permessi.mapping.url.root}/predicatoambito", defaultProjection = PredicatoAmbitoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "predicatoambito", path = "predicatoambito", exported = false, excerptProjection = PredicatoAmbitoWithPlainFields.class)
public interface PredicatoAmbitoRepository extends
        NextSdrQueryDslRepository<PredicatoAmbito, Integer, QPredicatoAmbito>,
        JpaRepository<PredicatoAmbito, Integer> {

}
