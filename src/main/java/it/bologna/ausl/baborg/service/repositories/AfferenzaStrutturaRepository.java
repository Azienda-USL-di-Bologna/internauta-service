package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.AfferenzaStruttura;
import it.bologna.ausl.baborg.model.entities.QAfferenzaStruttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.AfferenzaStrutturaWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

//@RepositoryRestResource(collectionResourceRel = "afferenzastruttura", path = "afferenzastruttura", exported = true)
@RepositoryRestResource(collectionResourceRel = "afferenzastruttura", path = "afferenzastruttura", exported = true, excerptProjection = AfferenzaStrutturaWithPlainFields.class)
public interface AfferenzaStrutturaRepository extends
        CustomQueryDslRepository<AfferenzaStruttura, Integer, QAfferenzaStruttura>,
        JpaRepository<AfferenzaStruttura, Integer> 
        {
}
