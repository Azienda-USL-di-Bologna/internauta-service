package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QMezzo;
import it.bologna.ausl.model.entities.scripta.projections.generated.MezzoWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/mezzo", defaultProjection = MezzoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mezzo", path = "mezzo", exported = false, excerptProjection = MezzoWithPlainFields.class)
public interface MezzoRepository extends
        NextSdrQueryDslRepository<Mezzo, Integer, QMezzo>,
        JpaRepository<Mezzo, Integer> {
}
