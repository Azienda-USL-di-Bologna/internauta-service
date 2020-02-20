package it.bologna.ausl.internauta.service.repositories.permessi;

import org.springframework.stereotype.Component;
import it.bologna.ausl.model.entities.permessi.Predicato;
import it.bologna.ausl.model.entities.permessi.QPredicato;
import it.bologna.ausl.model.entities.permessi.projections.generated.PredicatoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@Component("PredicatoInternauta")
@NextSdrRepository(repositoryPath = "${permessi.mapping.url.root}/predicato", defaultProjection = PredicatoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "predicato", path = "predicato", exported = false, excerptProjection = PredicatoWithPlainFields.class)
public interface PredicatoRepository extends
        NextSdrQueryDslRepository<Predicato, Integer, QPredicato>,
        JpaRepository<Predicato, Integer> {

}
