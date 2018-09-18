package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.Gdm2;
import it.bologna.ausl.model.entities.baborg.QGdm2;
import it.bologna.ausl.model.entities.baborg.projections.generated.Gdm2WithPlainFields;
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
@NextSdrRepository(repositoryPath = "gdm2", baseUrl = "${baborg.mapping.url.root}", defaultProjection = Gdm2WithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "gdm2", path = "gdm2", exported = false)
public interface Gdm2Repository extends
        NextSdrQueryDslRepository<Gdm2, String, QGdm2>,
        JpaRepository<Gdm2, String> {
}
