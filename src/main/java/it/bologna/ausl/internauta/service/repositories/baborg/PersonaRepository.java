package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/persona", defaultProjection = PersonaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "persona", path = "persona", exported = false, excerptProjection = PersonaWithPlainFields.class)
public interface PersonaRepository extends
        NextSdrQueryDslRepository<Persona, Integer, QPersona>,
        JpaRepository<Persona, Integer> {

    @Transactional
    @Modifying
//    @Query("update Persona p set p.messaggiVisti = ?2, p.nome = ?3 where p.id = ?1 and version = ?4")
    @Query(value = "update baborg.persone set messaggi_visti = tools.string_to_integer_array(?2, ','), nome = ?3 where id = ?1", nativeQuery = true)
    public void updateSeenMessage(Integer id, String messaggiVisti, String nome);

    public Persona findByCodiceFiscale(String codiceFiscale);

}
