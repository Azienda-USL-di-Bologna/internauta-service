package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrStruttura;
import it.bologna.ausl.model.entities.gru.QMdrStruttura;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrStrutturaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
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
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdrstruttura", defaultProjection = MdrStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrstruttura", path = "mdrstruttura", exported = false, excerptProjection = MdrStrutturaWithPlainFields.class)
public interface MdrStrutturaRepository extends
        NextSdrQueryDslRepository<MdrStruttura, Integer, QMdrStruttura>,
        JpaRepository<MdrStruttura, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_struttura where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
    
    @Query(value = "SELECT id_casella, id_padre, descrizione, datain, datafi, tipo_legame, codice_ente, id_azienda FROM gru.mdr_struttura WHERE id_azienda= ?1", nativeQuery = true)
    public  List<Map<String,Object>> selectStruttureByIdAzienda(Integer idAzienda);
    
    @Procedure("gru.select_multidefinictions_structure_byidazienda")
    public Integer selectMultiDefinictionsStructureByIdAzienda(
            @Param("id_azienda_par") Integer idAzienda,
            @Param("id_casella_par")Integer idCasella,
            @Param("datafi_par") String datafine,
            @Param("datain_par") String datainizio
    );
    
    @Query(value="select gru.lista_padri(?1)", nativeQuery=true)
    public List<Integer> listaPadri(Integer idAzienda);
    
    @Query(value="select * from gru.date_casella(?1,?2)", nativeQuery=true)
    public List<Object> dateCasella(Integer idAzienda,Integer id_casella);
    
    @Query(value="select ms.datain , ms.datafi from gru.mdr_struttura ms where ms.id_casella = ?2 and ms.id_azienda=?1 order by ms.datain asc", nativeQuery=true)
    public List<Map<String,Object>> dateCasella2(Integer idAzienda,Integer id_casella);
    
    @Query(value="select ms.datain , ms.datafi from gru.mdr_struttura ms where ms.id_casella = ?2 and ms.id_azienda=?1 order by ms.datain asc", nativeQuery=true)
    public List<Map<String,Object>> mieiPadri(Integer idAzienda,Integer id_casella);
    
    @Query(value = "select array_agg(distinct(id_casella)) from gru.mdr_struttura ms where ms.id_padre is not null and ms.id_padre != 0 and id_azienda = ?1 and ms.id_padre not in (select id_casella from gru.mdr_struttura ms3 where id_azienda =?1)", nativeQuery = true)
    public List<Integer> selectDaddyByIdAzienda(Integer idAzienda);
    
    @Query(value = "select array_agg(ms2.id_casella) from gru.mdr_struttura ms2 where ms2.id_azienda =?1 and (ms2.id_padre is not null and ms2.id_padre != 0) and " +
                    " ms2.id not in ( with figli as ( " +
                    " select ms.id, ms.id_casella, ms.datain as inFiglio, ms.datafi as fiFiglio, ms.id_padre from gru.mdr_struttura ms where ms.id_azienda =?1 and " +
                    " (ms.id_padre is not null and ms.id_padre != 0) order by ms.id_padre, ms.datain ) " +
                    " select f.id from figli f, gru.mdr_struttura ms where f.id_padre != 0 and f.id_padre=ms.id_casella and ((f.inFiglio >= ms.datain and f.fiFiglio <= ms.datafi) or (f.inFiglio >= ms.datain and ms.datafi is null)))", nativeQuery = true)
    public List<Integer> caselleInvalide(Integer idAzienda);
    
    @Query(value = "select count(id_casella) from gru.mdr_struttura ms where id_casella =?1 and id_azienda=?2", nativeQuery = true)
    public Integer selectStrutturaUtenteByIdCasellaAndIdAzienda(Integer idCasella,Integer idAzienda);
    
    @Query(value = "select case when exists (select id_casella from gru.mdr_struttura where id_casella=?1  and id_azienda =?2) then true else false end", nativeQuery = true)
    public Boolean esistePapa(Integer idCasella,Integer idAzienda);
    
    @Query(value = "select id_casella from gru.mdr_struttura where id_azienda=?1", nativeQuery = true)
    public List<Integer> listaStrutture(Integer idAzienda);
    
}
