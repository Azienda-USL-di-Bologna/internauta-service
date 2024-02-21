package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.PersonaVedente;
import it.bologna.ausl.model.entities.scripta.QPersonaVedente;
import it.bologna.ausl.model.entities.scripta.projections.generated.PersonaVedenteWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    @Query(value = "SELECT pv.piena_visibilita FROM scripta.persone_vedenti pv WHERE id_doc_detail = ?1 AND id_persona = ?2" ,
        nativeQuery = true)
    public Boolean hasPienaVisibilita(Integer idDoc, Integer idPersona);
    
    @Modifying
    @Query(value = 
            "WITH doc_da_permessizzare AS ( " +
                    "SELECT d.id as id, " +
                    "   false as mio_documento, " +
                    "   true as piena_visibilita, " +
                    "   d.data_creazione as data_creazione, " +
                    "   d.data_registrazione as data_registrazione, " +
                    "   d.id_azienda as id_azienda " +
                    "FROM scripta.docs_details d " +
                    "WHERE d.id IN :idDocDaAggiungere " +
            "), " +
            "id_persone AS ( " +
                    "SELECT a.id " +
                    "FROM UNNEST(STRING_TO_ARRAY(:idPersone , ',')\\:\\:integer[]) AS a(id) " +
            ") " +
            "INSERT INTO scripta.persone_vedenti( " +
                    "id_doc_detail, " +
                    "id_persona, " +
                    "mio_documento, " +
                    "piena_visibilita, " +
                    "data_creazione, " +
                    "data_registrazione, " +
                    "id_azienda) " +
            "SELECT doc_da_permessizzare.id, " +
                    "id_persone.id, " +
                    "doc_da_permessizzare.mio_documento, " +
                    "doc_da_permessizzare.piena_visibilita, " +
                    "doc_da_permessizzare.data_creazione, " +
                    "doc_da_permessizzare.data_registrazione, " +
                    "doc_da_permessizzare.id_azienda " +
            "FROM doc_da_permessizzare CROSS JOIN id_persone " +
            "ON CONFLICT DO NOTHING ",
        nativeQuery = true)
    public void insertPersoneVedentiMassiva(List<Integer> idDocDaAggiungere, String idPersone);
}

