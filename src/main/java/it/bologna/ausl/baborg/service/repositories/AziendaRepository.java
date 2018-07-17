package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.Azienda;
import it.bologna.ausl.baborg.model.entities.QAzienda;
import it.bologna.ausl.baborg.model.entities.projections.generated.AziendaWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@RepositoryRestResource(collectionResourceRel = "azienda", path = "azienda", exported = true, excerptProjection = AziendaWithPlainFields.class)
public interface AziendaRepository extends
        CustomQueryDslRepository<Azienda, Integer, QAzienda>,
        JpaRepository<Azienda, Integer> {
}
