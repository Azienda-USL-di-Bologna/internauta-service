package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/utente", defaultProjection = UtenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "utente", path = "utente", exported = false, excerptProjection = UtenteWithPlainFields.class)
public interface UtenteRepository extends
        NextSdrQueryDslRepository<Utente, Integer, QUtente>,
        JpaRepository<Utente, Integer> {

    public Utente findByIdAziendaAndIdPersona(Azienda azienda, Persona persona);

    public List<Utente> findByIdPersonaAndAttivo(Persona persona, boolean attivo);
    
//     @Query(value = "select baborg.get_strutture_con_permesso_segr(?1) as result", nativeQuery = true)
//    public Integer[] getStruttureConPermessoSegr(Integer utente);
    
    @Query(value = "SELECT  s.* " +
        "FROM permessi.permessi p  " +
        "JOIN permessi.entita e on e.id  = p.id_soggetto  " +
        "JOIN permessi.entita e2 on e2.id  = p.id_oggetto  " +
        "JOIN permessi.predicati p3 on p3.id = p.id_predicato " +
        "JOIN baborg.strutture s on s.id = e2.id_provenienza  " +
        "WHERE e.id_provenienza = (?1)  " + 
        "AND p3.predicato  = 'SEGR' " + 
        "AND p.attivo_al is null ", nativeQuery = true)
     public List<Struttura> getStruttureConPermessoSegr(Integer utente);
}
