package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.Azienda;
import it.bologna.ausl.baborg.model.entities.QAzienda;
import it.bologna.ausl.baborg.model.entities.projections.generated.AziendaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "azienda", defaultProjection = AziendaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "azienda", path = "azienda", exported = false, excerptProjection = AziendaWithPlainFields.class)
public interface AziendaRepository extends
        NextSdrQueryDslRepository<Azienda, Integer, QAzienda>,
        JpaRepository<Azienda, Integer> {
}
