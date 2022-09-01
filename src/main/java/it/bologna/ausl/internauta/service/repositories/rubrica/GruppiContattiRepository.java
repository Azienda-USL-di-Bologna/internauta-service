package it.bologna.ausl.internauta.service.repositories.rubrica;

import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.rubrica.QGruppiContatti;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${rubrica.mapping.url.root}/gruppicontatti", defaultProjection = GruppiContattiWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "gruppicontatti", path = "gruppicontatti", exported = false, excerptProjection = GruppiContattiWithPlainFields.class)
public interface GruppiContattiRepository extends
        NextSdrQueryDslRepository<GruppiContatti, Integer, QGruppiContatti>,
        JpaRepository<GruppiContatti, Integer> {

}
