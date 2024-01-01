package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithPlainFields;
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
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/utentestruttura", defaultProjection = UtenteStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "utentestruttura", path = "utentestruttura", exported = false, excerptProjection = UtenteStrutturaWithPlainFields.class)
public interface UtenteStrutturaRepository extends
        NextSdrQueryDslRepository<UtenteStruttura, Integer, QUtenteStruttura>,
        JpaRepository<UtenteStruttura, Integer> {

    @Query(value = "select id_struttura from baborg.utenti_strutture where id_utente = ?1 and attivo = true "
            + "and id_afferenza_struttura = (select id from baborg.afferenza_struttura where codice = 'DIRETTA') limit 1", nativeQuery = true)
    public Integer getIdStrutturaAfferenzaDirettaAttivaByIdUtente(Integer idUtente);
    
    @Query(value = "select id_struttura from baborg.utenti_strutture where id_utente = ?1 and attivo = true "
            + "and id_afferenza_struttura = (select id from baborg.afferenza_struttura where codice = 'UNIFICATA') limit 1", nativeQuery = true)
    public Integer getIdStrutturaAfferenzaUnificataAttivaByIdUtente(Integer idUtente);
    
    @Query(value = "select * from baborg.utenti_strutture where id_utente = ?1 and attivo = true ", nativeQuery = true)
    public List<UtenteStruttura> getListUtentiStrutturaAfferenzeAttiveByIdUtente(Integer idUtente);

    @Query(value = "select p.id from baborg.persone p "
            + "join baborg.utenti u on u.id_persona = p.id "
            + "join baborg.utenti_strutture us on us.id_utente = u.id "
            + "where us.id_struttura in (?1) "
            + "and us.attivo = true", nativeQuery = true)
    public List<Integer> getIdPersonaListInStrutture(List<Integer> idStrutture);

}
