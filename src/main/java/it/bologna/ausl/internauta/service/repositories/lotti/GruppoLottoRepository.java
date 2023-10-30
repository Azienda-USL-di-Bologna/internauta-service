package it.bologna.ausl.internauta.service.repositories.lotti;

import it.bologna.ausl.model.entities.lotti.projections.generated.GruppoLottoWithComponentiList;
import it.bologna.ausl.model.entities.lotti.GruppoLotto;
import it.bologna.ausl.model.entities.lotti.QGruppoLotto;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author QB
 */
@NextSdrRepository(repositoryPath = "${lotti.mapping.url.root}/gruppolotto", defaultProjection = GruppoLottoWithComponentiList.class)
@RepositoryRestResource(collectionResourceRel = "gruppolotto", path = "gruppolotto", exported = false, excerptProjection = GruppoLottoWithComponentiList.class)
public interface GruppoLottoRepository extends 
        NextSdrQueryDslRepository<GruppoLotto, Integer, QGruppoLotto>,
        JpaRepository<GruppoLotto, Integer> {}
