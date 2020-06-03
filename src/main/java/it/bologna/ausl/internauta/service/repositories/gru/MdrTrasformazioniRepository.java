package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrTrasformazioni;
import it.bologna.ausl.model.entities.gru.QMdrTrasformazioni;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrTrasformazioniWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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
    
    
    @Query(value = "select count(ms.id_casella) FROM gru.mdr_struttura ms where ms.id_azienda = ?1 and ms.id_casella=?2 and ms.datain <= ?3 and ms.datafi > ?3 ", nativeQuery = true)
    public Integer isTransformableByIdAzienda(Integer idAzienda, Integer id_casella, LocalDateTime data);
}

