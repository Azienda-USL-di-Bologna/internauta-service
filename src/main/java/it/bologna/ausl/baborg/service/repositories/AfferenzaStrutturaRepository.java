package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.AfferenzaStruttura;
import it.bologna.ausl.baborg.model.entities.QAfferenzaStruttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.AfferenzaStrutturaWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo.
 *
 * JpaRepository: permette di fare le operazioni base (insert, delete, update,
 * select)
 *
 * CustomQueryDslRepository: serve per fare le query con gli oggetti Q
 *
 *
 * exported: definisce se si esporta il repository. Nel nostro framework si
 * passa dal controller e quindi deve essere semrpe settato a false.
 *
 *
 * excerptProjection: projection di default. Se si chiamano le cose senza
 * projection, è valida quella indicata.
 *
 */
@RepositoryRestResource(collectionResourceRel = "afferenzastruttura", path = "afferenzastruttura", exported = false, excerptProjection = AfferenzaStrutturaWithPlainFields.class)
public interface AfferenzaStrutturaRepository extends
        CustomQueryDslRepository<AfferenzaStruttura, Integer, QAfferenzaStruttura>,
        JpaRepository<AfferenzaStruttura, Integer> {
}
