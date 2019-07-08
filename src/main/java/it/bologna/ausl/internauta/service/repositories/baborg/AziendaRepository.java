package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/azienda", defaultProjection = AziendaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "azienda", path = "azienda", exported = false, excerptProjection = AziendaWithPlainFields.class)
public interface AziendaRepository extends
        NextSdrQueryDslRepository<Azienda, Integer, QAzienda>,
        JpaRepository<Azienda, Integer> {
}
