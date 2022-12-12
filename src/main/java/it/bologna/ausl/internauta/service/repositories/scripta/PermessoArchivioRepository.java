package it.bologna.ausl.internauta.service.repositories.scripta;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.jpa.natiquery.NativeQueryTools;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.PermessoArchivioWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.EntityManager;

import javax.persistence.PersistenceContext;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocalDateTimeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/permessoarchivio", defaultProjection = PermessoArchivioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "permessoarchivio", path = "permessoarchivio", exported = false, excerptProjection = PermessoArchivioWithPlainFields.class)
public interface PermessoArchivioRepository extends
        NextSdrQueryDslRepository<PermessoArchivio, Integer, QPermessoArchivio>, 
        JpaRepository<PermessoArchivio, Integer> {

    
    
    
     @Query(value = "SELECT pa.id_persona " +
            "FROM scripta.docs d " +
            "JOIN scripta.archivi_docs ad ON ad.id_doc = d.id " +
            "JOIN scripta.archivi a ON a.id = ad.id_archivio " +
            "JOIN scripta.permessi_archivi pa ON pa.id_archivio_detail = a.id AND pa.id_azienda = a.id_azienda AND pa.data_creazione = a.data_creazione " +
            "WHERE id_esterno = ?1 " +
            "AND ad.data_eliminazione IS NULL " +
            "AND bit >= ?2 ",
            nativeQuery = true)
    public List<Integer> getIdPersoneConPermessoSuArchiviazioniDelDocByIdEsterno(String idEsterno, Integer minBit);
    
    @Query(value="select * from scripta.get_archivi_da_permessizzare_from_id_permessi(?1, ?2, ?3)", nativeQuery = true)
    public String getArchiviDaPermessizzareFromIdPermessi(Integer idPersona, Integer idAzienda, String idPermessi);
}
