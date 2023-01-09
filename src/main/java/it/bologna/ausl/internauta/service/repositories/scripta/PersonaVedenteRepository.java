package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.PersonaVedente;
import it.bologna.ausl.model.entities.scripta.QPersonaVedente;
import it.bologna.ausl.model.entities.scripta.projections.generated.PersonaVedenteWithPlainFields;
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
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/personavedente", defaultProjection = PersonaVedenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "personavedente", path = "personavedente", exported = false, excerptProjection = PersonaVedenteWithPlainFields.class)
public interface PersonaVedenteRepository extends
        NextSdrQueryDslRepository<PersonaVedente, Long, QPersonaVedente>, 
        JpaRepository<PersonaVedente, Long> {
    
//    @Query(value = "select scripta.aggiungi_persone_vedenti_su_doc_da_permessi_archivi(?1)", nativeQuery = true)
//    public void aggiungiPersoneVedentiSuDocDaPermessiArchivi(
//        Integer idDoc
//    );
    
    @Query(value = "SELECT scripta.calcola_persone_vedenti(?1)", nativeQuery = true)
    public void calcolaPersoneVedenti(
        Integer idDoc
    );
    
    @Query(value = "SELECT pv.piena_visibilita " +
        "FROM scripta.persone_vedenti pv " +
       "WHERE id_doc_detail = ?1 " +
        "AND id_persona = ?2 " ,
        nativeQuery = true)
    public Boolean hasPienaVisibìlita(Integer idDoc, Integer idPersona);
}
