package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.QPecAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecAziendaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/pecazienda", defaultProjection = PecAziendaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pecazienda", path = "pecazienda", exported = false, excerptProjection = PecAziendaWithPlainFields.class)
public interface PecAziendaRepository extends
        NextSdrQueryDslRepository<PecAzienda, Integer, QPecAzienda>,
        JpaRepository<PecAzienda, Integer> {

    @Query(value = "select id from baborg.pec_aziende where id_pec = ?1 and id_azienda = ?2", nativeQuery = true)
    public Integer getIdByIdPecAndIdAzienda(
            @Param("id_pec") Integer idPec,
            @Param("id_azienda") Integer idAzienda);
}
