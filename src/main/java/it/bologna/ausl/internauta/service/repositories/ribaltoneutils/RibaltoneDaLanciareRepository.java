package it.bologna.ausl.internauta.service.repositories.ribaltoneutils;


import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.QRibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.projections.generated.RibaltoneDaLanciareWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${ribaltoneutils.mapping.url.root}/ribaltonedalanciare", defaultProjection = RibaltoneDaLanciareWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "ribaltonedalanciare", path = "ribaltonedalanciare", exported = false, excerptProjection = RibaltoneDaLanciareWithPlainFields.class)
public interface RibaltoneDaLanciareRepository extends
        NextSdrQueryDslRepository<RibaltoneDaLanciare, Integer, QRibaltoneDaLanciare>,
        JpaRepository<RibaltoneDaLanciare, Integer> {
}
