package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.projections.generated.StoricoRelazioneWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.json.JSONObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/storicorelazione", defaultProjection = StoricoRelazioneWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "storicorelazione", path = "storicorelazione", exported = false, excerptProjection = StoricoRelazioneWithPlainFields.class)
public interface StoricoRelazioneRepository extends
        NextSdrQueryDslRepository<StoricoRelazione, Integer, QStoricoRelazione>,
        JpaRepository<StoricoRelazione, Integer> {
    
    @Procedure("baborg.get_strutture_antenate_in_storico_relazione")
    public String getStruttureAntenateInStoricoRelazione(Integer idStruttura, String dataRiferimento);
    
    @Query(value = "select baborg.get_strutture_ruolo_e_figlie(?1, ?2, ?3) as result", nativeQuery = true)
//    @Procedure(procedureName = "select baborg.get_strutture_ruolo", outputParameterName = "get_strutture_ruolo" )
    public Map getStruttureRuoloEFiglie(Integer mascheraBit, Integer idUtente, LocalDateTime dataRiferimento);
    
    @Query(value = "select baborg.get_strutture_ruolo(?1, ?2, ?3) as result", nativeQuery = true)
//    @Procedure(procedureName = "select baborg.get_strutture_ruolo", outputParameterName = "get_strutture_ruolo" )
    public Map getStruttureRuolo(Integer mascheraBit, Integer idUtente, LocalDateTime dataRiferimento);
    
    @Query(value = "select baborg.get_strutture_ruolo(?1, ?2) as result", nativeQuery = true)
//    @Procedure(procedureName = "select baborg.get_strutture_ruolo", outputParameterName = "get_strutture_ruolo" )
    public Map getStruttureRuolo(Integer mascheraBit, Integer idUtente);
}
