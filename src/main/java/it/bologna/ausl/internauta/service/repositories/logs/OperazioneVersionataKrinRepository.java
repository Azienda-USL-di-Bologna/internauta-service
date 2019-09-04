package it.bologna.ausl.internauta.service.repositories.logs;

import it.bologna.ausl.model.entities.logs.Counter;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.OperazioneVersionataKrint;
import it.bologna.ausl.model.entities.logs.QCounter;
import it.bologna.ausl.model.entities.logs.QOperazioneKrint;
import it.bologna.ausl.model.entities.logs.QOperazioneVersionataKrint;
import it.bologna.ausl.model.entities.logs.projections.generated.CounterWithPlainFields;
import it.bologna.ausl.model.entities.logs.projections.generated.OperazioneKrintWithPlainFields;
import it.bologna.ausl.model.entities.logs.projections.generated.OperazioneVersionataKrintWithPlainFields;

import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${logs.mapping.url.root}/operazioneversionatakrint", defaultProjection = OperazioneKrintWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "operazioneversionatakrint", path =  "operazioneversionatakrint", exported = false, excerptProjection = OperazioneVersionataKrintWithPlainFields.class)
public interface OperazioneVersionataKrinRepository extends
        NextSdrQueryDslRepository<OperazioneVersionataKrint, Integer, QOperazioneVersionataKrint>,
        JpaRepository<OperazioneVersionataKrint, Integer> {
    
//    Optional<OperazioneKrint> findByCodice(String codice);

    Optional<OperazioneVersionataKrint> findFirstByIdOperazioneOrderByVersioneDesc(OperazioneKrint operazioneKrint);
    
}

//findFirstByOrderByIdAsc();