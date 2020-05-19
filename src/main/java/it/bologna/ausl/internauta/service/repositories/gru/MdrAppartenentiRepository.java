package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrAppartenenti;
import it.bologna.ausl.model.entities.gru.QMdrAppartenenti;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrAppartenentiWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.time.LocalDateTime;
import java.util.List;
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
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdrappartenenti", defaultProjection = MdrAppartenentiWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrappartenenti", path = "mdrappartenenti", exported = false, excerptProjection = MdrAppartenentiWithPlainFields.class)
public interface MdrAppartenentiRepository extends
        NextSdrQueryDslRepository<MdrAppartenenti, Integer, QMdrAppartenenti>,
        JpaRepository<MdrAppartenenti, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_appartenenti where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
        
    @Procedure("gru.select_multidefinictions_user_byidazienda")
    public Integer select_multidefinictions_user_byidazienda(
            @Param("codice_ente_par") Integer codiceEnte,
            @Param("codice_matricola_par")Integer codiceMatricola,
            @Param("id_casella_par")Integer idCasella,
            @Param("datafi_par") String datafine,
            @Param("datain_par") String datainizio
    );
     
    @Query(value = "select count(ma.codice_matricola) from gru.mdr_appartenenti ma where ma.codice_matricola = ?1", nativeQuery = true)
    public  Integer countUsertByCodiceMatricola(Integer codice_matricola);
}
