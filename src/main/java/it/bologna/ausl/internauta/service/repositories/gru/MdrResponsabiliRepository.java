package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrResponsabili;
import it.bologna.ausl.model.entities.gru.QMdrResponsabili;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrResponsabiliWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdrresponsabili", defaultProjection = MdrResponsabiliWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrresponsabili", path = "mdrresponsabili", exported = false, excerptProjection = MdrResponsabiliWithPlainFields.class)
public interface MdrResponsabiliRepository extends
        NextSdrQueryDslRepository<MdrResponsabili, Integer, QMdrResponsabili>,
        JpaRepository<MdrResponsabili, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_responsabili where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
    
    @Procedure("gru.count_multidefinictions_respo_byidazienda")
    public Integer countMultiReponsabilePerStruttura(
            @Param("codice_ente_par") Integer codiceEnte,
            @Param("id_casella_par")Integer idCasella,
            @Param("datafi_par") String datafine,
            @Param("datain_par") String datainizio
    );
}
