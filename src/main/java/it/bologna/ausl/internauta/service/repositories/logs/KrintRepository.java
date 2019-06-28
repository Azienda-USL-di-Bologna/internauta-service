package it.bologna.ausl.internauta.service.repositories.logs;

import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.QOperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.generated.KrintWithPlainFields;
import it.bologna.ausl.model.entities.logs.projections.generated.OperazioneKrintWithPlainFields;

import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${logs.mapping.url.root}/krint", defaultProjection = KrintWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "krint", path =  "krint", exported = false, excerptProjection = KrintWithPlainFields.class)
public interface KrintRepository extends
        NextSdrQueryDslRepository<Krint, Integer, QOperazioneKrint>,
        JpaRepository<Krint, Integer> {
    
    
}