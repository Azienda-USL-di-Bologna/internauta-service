package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.QUtenteStruttura;
import it.bologna.ausl.baborg.model.entities.UtenteStruttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteStrutturaWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "utentestruttura", path = "utentestruttura", exported = true, excerptProjection = UtenteStrutturaWithPlainFields.class)
public interface UtenteStrutturaRepository extends
        CustomQueryDslRepository<UtenteStruttura, Integer, QUtenteStruttura>,
        JpaRepository<UtenteStruttura, Integer> {
}
