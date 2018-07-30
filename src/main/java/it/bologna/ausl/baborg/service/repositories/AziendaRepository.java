package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.Azienda;
import it.bologna.ausl.baborg.model.entities.QAzienda;
import it.bologna.ausl.baborg.model.entities.projections.generated.AziendaWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
//@RepositoryRestResource(collectionResourceRel = "azienda", path = "azienda", exported = true)
@RepositoryRestResource(collectionResourceRel = "azienda", path = "azienda", exported = false, excerptProjection = AziendaWithPlainFields.class)
public interface AziendaRepository extends
        CustomQueryDslRepository<Azienda, Integer, QAzienda>,
        JpaRepository<Azienda, Integer> {
}
