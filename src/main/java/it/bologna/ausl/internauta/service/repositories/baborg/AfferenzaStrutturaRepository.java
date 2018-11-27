package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.AfferenzaStrutturaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo.
 *
 * JpaRepository: permette di fare le operazioni base (insert, delete, update,
 * select)
 *
 * NextSdrQueryDslRepository: serve per fare le query con gli oggetti Q
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
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/afferenzastruttura", defaultProjection = AfferenzaStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "afferenzastruttura", path = "afferenzastruttura", exported = false)
public interface AfferenzaStrutturaRepository extends
        NextSdrQueryDslRepository<AfferenzaStruttura, Integer, QAfferenzaStruttura>,
        JpaRepository<AfferenzaStruttura, Integer> {
}
