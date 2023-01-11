package it.bologna.ausl.internauta.service.repositories.ribaltoneutils;


import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.QRibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.projections.generated.RibaltoneDaLanciareWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${ribaltoneutils.mapping.url.root}/ribaltonedalanciare", defaultProjection = RibaltoneDaLanciareWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "ribaltonedalanciare", path = "ribaltonedalanciare", exported = false, excerptProjection = RibaltoneDaLanciareWithPlainFields.class)
public interface RibaltoneDaLanciareRepository extends
        NextSdrQueryDslRepository<RibaltoneDaLanciare, Integer, QRibaltoneDaLanciare>,
        JpaRepository<RibaltoneDaLanciare, Integer> {
    
    
    @Query(
    value = "SELECT distinct on(id_azienda) id FROM ribaltone_utils.ribaltoni_da_lanciare order by id_azienda, data_inserimento_riga desc", nativeQuery = true)
    List<Integer> getUltimoStato();
    
    @Query(value = "SELECT pg_notify('trasforma', 'AVEC_____'||?1||'_____'||?2||'_____'||?3||'_____'||?4)", nativeQuery = true)
//    @Query(value = "NOTIFY trasforma,'AVEC_?1_?2_?3_?4'", nativeQuery = true)
    public void sendNotifyInternauta(
            @Param("codiceEnte") String codiceEnte, 
            @Param("ribaltaArgo") Boolean ribaltaArgo, 
            @Param("ribaltaInternauta") Boolean ribaltaInternauta,
            @Param("note") String note);
}
