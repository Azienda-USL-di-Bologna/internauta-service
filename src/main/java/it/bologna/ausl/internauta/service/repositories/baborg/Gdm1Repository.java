package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.Gdm1;
import it.bologna.ausl.model.entities.baborg.QGdm1;
import it.bologna.ausl.model.entities.baborg.projections.generated.Gdm1WithPlainFields;
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
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/gdm1", defaultProjection = Gdm1WithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "gdm1", path = "gdm1", exported = false)
public interface Gdm1Repository extends
        NextSdrQueryDslRepository<Gdm1, Integer, QGdm1>,
        JpaRepository<Gdm1, Integer> {
}
