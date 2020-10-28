package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.AttributiStruttura;
import it.bologna.ausl.model.entities.baborg.QAttributiStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.AttributiStrutturaWithIdStrutturaAndIdTipologiaStruttura;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/attributistruttura", defaultProjection = AttributiStrutturaWithIdStrutturaAndIdTipologiaStruttura.class)
@RepositoryRestResource(collectionResourceRel = "attributistruttura", path = "attributistruttura", exported = false)
public interface AttributiStrutturaRepository extends
        NextSdrQueryDslRepository<AttributiStruttura, Integer, QAttributiStruttura>,
        JpaRepository<AttributiStruttura, Integer> {
}
