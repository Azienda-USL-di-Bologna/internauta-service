package it.bologna.ausl.internauta.service.repositories.scrivania;

import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.QAttivita;
import it.bologna.ausl.model.entities.scrivania.projections.generated.AttivitaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scrivania.mapping.url.root}/attivita", defaultProjection = AttivitaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "attivita", path = "attivita", exported = false, excerptProjection = AttivitaWithPlainFields.class)
public interface AttivitaRepository extends
        NextSdrQueryDslRepository<Attivita, Integer, QAttivita>,
        JpaRepository<Attivita, Integer> {
    
    @Query(value = "select * from scrivania.attivita a "
            + "where id_persona = ?1 "
            + "and tipo = 'attivita' "
            + "order by id desc limit 50 ", nativeQuery = true)
    public List<Attivita> getLatestFiftyAttivitaInScrivaniaByIdPersona(Integer idPersona);
    
}
