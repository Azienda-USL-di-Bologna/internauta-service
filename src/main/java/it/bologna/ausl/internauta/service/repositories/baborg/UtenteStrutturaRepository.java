package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/utentestruttura", defaultProjection = UtenteStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "utentestruttura", path = "utentestruttura", exported = false, excerptProjection = UtenteStrutturaWithPlainFields.class)
public interface UtenteStrutturaRepository extends
        NextSdrQueryDslRepository<UtenteStruttura, Integer, QUtenteStruttura>,
        JpaRepository<UtenteStruttura, Integer> {
}
