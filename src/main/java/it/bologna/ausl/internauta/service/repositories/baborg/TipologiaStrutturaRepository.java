package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QTipologiaStruttura;
import it.bologna.ausl.model.entities.baborg.TipologiaStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.TipologiaStrutturaWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/tipologiastruttura", defaultProjection = TipologiaStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "tipologiastruttura", path = "tipologiastruttura", exported = false)
public interface TipologiaStrutturaRepository extends
        NextSdrQueryDslRepository<TipologiaStruttura, Integer, QTipologiaStruttura>,
        JpaRepository<TipologiaStruttura, Integer> {
}
