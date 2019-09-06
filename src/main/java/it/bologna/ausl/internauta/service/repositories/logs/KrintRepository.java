package it.bologna.ausl.internauta.service.repositories.logs;

import it.bologna.ausl.internauta.service.krint.KrintLogDescription;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.QKrint;
import it.bologna.ausl.model.entities.logs.projections.generated.KrintWithPlainFields;

import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${logs.mapping.url.root}/krint", defaultProjection = KrintWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "krint", path =  "krint", exported = false, excerptProjection = KrintWithPlainFields.class)
public interface KrintRepository extends
        NextSdrQueryDslRepository<Krint, Integer, QKrint>,
        JpaRepository<Krint, Integer> {
    
    
    // TODO: Non funziona. La sote procedure torna una table() Bisogna o trovare
    // il modo di farla funzionare oppure la funzione deve tornare qualcos'altro
    // ad es un json
    @Procedure("logs.get_logs")
    public List<KrintLogDescription> getLogs(
        @Param("codici_operazioni") String[] codiciOperazioni,
        @Param("p_id_oggetto") String idOggetto,
        @Param("p_tipo_oggetto") String tipoOggetto,
        @Param("p_id_utente") Integer idUtente,
        @Param("id_oggetto_contenitore") String idOggettoContenitore,
        @Param("tipo_oggetto_contenitore") String tipoOggettoContenitore,
        @Param("data_da") Date dataDa,
        @Param("data_a") Date dataA
    );
}