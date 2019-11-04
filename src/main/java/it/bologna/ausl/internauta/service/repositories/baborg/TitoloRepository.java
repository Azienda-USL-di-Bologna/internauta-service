package it.bologna.ausl.internauta.service.repositories.baborg;

/**
 *
 * @author spritz
 */
import it.bologna.ausl.model.entities.baborg.QTitolo;
import it.bologna.ausl.model.entities.baborg.Titolo;
import it.bologna.ausl.model.entities.baborg.projections.generated.TitoloWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/titolo", defaultProjection = TitoloWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "titolo", path = "titolo", exported = false, excerptProjection = TitoloWithPlainFields.class)
public interface TitoloRepository extends
        NextSdrQueryDslRepository<Titolo, Integer, QTitolo>,
        JpaRepository<Titolo, Integer> {
}
