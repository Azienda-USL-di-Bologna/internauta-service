package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/struttura", defaultProjection = StrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "struttura", path = "struttura", exported = false, excerptProjection = StrutturaWithPlainFields.class)
public interface StrutturaRepository extends
        NextSdrQueryDslRepository<Struttura, Integer, QStruttura>,
        JpaRepository<Struttura, Integer>, StrutturaRepositoryCustom {

    // la store procedure prende in ingresso un idStruttura e restituisce una stringa in cui sono presenti gli id delle strutture antenate della struttura passata
    // la stringa è formata dagli id numerici separati dal carattere ','
    // l'id della struttura pasasta è in prima posizione, quello della struttura radice in ultima
    // RM 44811: È stata sostituita con la store procedure che si trova in storico relazione repository
    // la differenza è che le strutture vengono estratte dalla tabella Storico Relazione
    @Procedure("baborg.get_strutture_antenate")
    public String getStruttureAntenate(Integer idStruttura);

    @Procedure("baborg.get_responsabile")
    public Integer getResponsabile(Integer idStruttura);

    @Procedure("baborg.get_responsabili")
    public String getResponsabili(Integer idStruttura);

    @Query(value = "select * from baborg.get_utenti_struttura_sottoresponsabili_filtered(?1)", nativeQuery = true)
    public List<Map<String, Object>> getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(Integer idStruttura);

    @Procedure("baborg.get_count_utenti_struttura")
    public String getCountUtentiStruttura(Integer p_id_struttura);
//    @Query(value = "select * from baborg.get_utenti_struttura_sottoresponsabili_filtered_at_date(?1, ?2)", nativeQuery = true)
////    @Procedure("baborg.get_utenti_struttura_sottoresponsabili_filtered_at_date")
//    public List<Map<String, Object>> getIdUtentiStruttureWithSottoResponsabiliByIdStruttura1(Integer idStruttura, LocalDateTime dataRiferimento);

    @Query(value = "select id_struttura from baborg.utenti_strutture "
            + "where id_utente = ?1 and attivo = true "
            + "and bit_ruoli >= (select maschera_bit from baborg.ruoli where nome_breve = ?2)",
            nativeQuery = true)
    public List<Integer> getIdStruttureDiUtenteByCodiceRuolo(Utente utente, String codiceRuolo);

    //@Query(value = "select baborg.get_strutture_ruolo_e_figlie(?1, ?2, ?3);", nativeQuery = true)
//    @Procedure(value = "baborg.get_strutture_ruolo_e_figlie")
//    public List<Integer> getStruttureRuoloEFiglie(Integer bitRuoli, Integer idUtente, LocalDateTime date);

    @Procedure("baborg.get_strutture_figlie_e_cugine")
    public List<Integer> getStruttureFiglieECugine(Integer idStruttura);
    
    @Procedure("baborg.get_strutture_figlie")
    public List<Integer> getStruttureFiglie(Integer idStruttura);
    
    @Query(value= "select s.id from baborg.strutture s " +
                    "join baborg.attributi_strutture as2 on as2.id_struttura = s.id " +
                    "join baborg.tipologie_struttura ts on ts.id = as2.id_tipologia_struttura " +
                    "where ts.tipologia = 'Assegnamento'",
                    nativeQuery = true ) 
    public List<Integer> getPoolAssegnamento();
}
