package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrTrasformazioni;
import it.bologna.ausl.model.entities.gru.QMdrTrasformazioni;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrTrasformazioniWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
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
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdrtrasformazioni", defaultProjection = MdrTrasformazioniWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrtrasformazioni", path = "mdrtrasformazioni", exported = false, excerptProjection = MdrTrasformazioniWithPlainFields.class)
public interface MdrTrasformazioniRepository extends
        NextSdrQueryDslRepository<MdrTrasformazioni, Integer, QMdrTrasformazioni>,
        JpaRepository<MdrTrasformazioni, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_trasformazioni where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
    
    @Query(value =  "SELECT progressivo_riga, id_casella_partenza, id_casella_arrivo, data_trasformazione, motivo, datain_partenza, dataora_oper, codice_ente, id_azienda FROM gru.mdr_trasformazioni WHERE id_azienda= ?1", nativeQuery = true)
    public  List<Map<String,Object>> selectTrasformazioniByIdAzienda(Integer idAzienda);
    
    @Query(value = "select count(id_azienda) FROM gru.mdr_trasformazioni where id_azienda = ?1", nativeQuery = true)
    public Integer countRow(Integer idAzienda);
    
    @Query(value = "select count(ms.id_casella) FROM gru.mdr_struttura ms where ms.id_azienda = ?1 and ms.id_casella=?2 and ms.datain <= ?3 and ms.datafi > ?3 ", nativeQuery = true)
    public Integer isTransformableByIdAzienda(Integer idAzienda, Integer id_casella, ZonedDateTime data);
    
    @Procedure("gru.isSpentaAccesaBeneByIdAzienda")
    public Integer isSpentaAccesaBeneByIdAzienda(
            @Param("p_id_azienda") Integer idAzienda,
            @Param("p_id_casella")Integer idCasella,
            @Param("p_data_trasformazione") String data_trasformazione,
            @Param("p_datain") String datain
    );
    
    @Query(value = "select count(ms.id_casella) FROM gru.mdr_struttura ms where ms.id_azienda = ?1 and ms.id_casella=?2 and ms.datain=?3 ", nativeQuery = true)
    public Integer isAccesaBeneByIdAzienda(Integer idAzienda, Integer id_casella, ZonedDateTime data_trasformazione);
    
    @Query(value = "select count(ms.id_casella) FROM gru.mdr_struttura ms where ms.id_azienda = ?1 and ms.id_casella=?2 and ms.datain<=?3 and (ms.datafi is null or ms.datafi >= ?3)", nativeQuery = true)
    public Integer isAccesaIntervalloByIdAzienda(Integer idAzienda, Integer id_casella, ZonedDateTime data_trasformazione);
}

