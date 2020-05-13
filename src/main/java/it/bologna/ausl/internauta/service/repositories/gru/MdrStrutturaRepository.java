package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrStruttura;
import it.bologna.ausl.model.entities.gru.QMdrStruttura;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrStrutturaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdrstruttura", defaultProjection = MdrStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrstruttura", path = "mdrstruttura", exported = false, excerptProjection = MdrStrutturaWithPlainFields.class)
public interface MdrStrutturaRepository extends
        NextSdrQueryDslRepository<MdrStruttura, Integer, QMdrStruttura>,
        JpaRepository<MdrStruttura, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_struttura where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
    
    @Query(value = "select gru.select_multidefinictions_structure_byidazienda(?1,?2,?3,?4)", nativeQuery = true)
    public  Integer selectMultiDefinictionsStructureByIdAzienda(Integer idAzienda, Integer id_casella, LocalDateTime datafine, LocalDateTime datainizio);
    
    @Query(value = "select count(id_padre) from (select distinct(ms.id_padre) from gru.mdr_struttura ms where ms.id_padre is not null and ms.id_azienda = ?1) as padri where padri.id_padre not in (select distinct(ms.id_casella) from gru.mdr_struttura ms where ms.id_casella in (select distinct(ms.id_padre) from gru.mdr_struttura ms where ms.id_padre is not null and ms.id_azienda =?1))", nativeQuery = true)
    public Integer selectDaddyByIdAzienda(Integer idAzienda);
    
    @Query(value = "select count(id_casella) from gru.mdr_struttura ms where id_casella =?1 and id_azienda=?2", nativeQuery = true)
    public Integer selectStrutturaUtenteByIdCasellaAndIdAzienda(Integer id_casella,Integer id_azienda);
    
}
