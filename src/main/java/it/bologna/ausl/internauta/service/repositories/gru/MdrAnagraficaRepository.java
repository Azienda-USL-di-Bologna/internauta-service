package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrAnagrafica;
import it.bologna.ausl.model.entities.gru.QMdrAnagrafica;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrAnagraficaWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdranagrafica", defaultProjection = MdrAnagraficaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdranagrafica", path = "mdranagrafica", exported = false, excerptProjection = MdrAnagraficaWithPlainFields.class)
public interface MdrAnagraficaRepository extends
        NextSdrQueryDslRepository<MdrAnagrafica, Integer, QMdrAnagrafica>,
        JpaRepository<MdrAnagrafica, Integer>, MdrAppartenentiRepositoryCustom {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_anagrafica where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
    
    
    @Query(value = "select count(id_azienda) FROM gru.mdr_anagrafica where id_azienda = ?1", nativeQuery = true)
    public Integer countRow(Integer idAzienda);
    
    @Query(value =  "SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, email FROM gru.mdr_anagrafica WHERE id_azienda= ?1", nativeQuery = true)
    public  List<Map<String,Object>> selectAnagraficaByIdAzienda(Integer idAzienda);

}
