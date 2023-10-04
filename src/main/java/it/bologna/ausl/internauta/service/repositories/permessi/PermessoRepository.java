package it.bologna.ausl.internauta.service.repositories.permessi;

import it.bologna.ausl.model.entities.permessi.Permesso;
import it.bologna.ausl.model.entities.permessi.QPermesso;
import org.springframework.stereotype.Component;
import it.bologna.ausl.model.entities.permessi.projections.generated.PermessoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@Component("PermessoInternauta")
@NextSdrRepository(repositoryPath = "${permessi.mapping.url.root}/permesso", defaultProjection = PermessoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "permesso", path = "permesso", exported = false, excerptProjection = PermessoWithPlainFields.class)
public interface PermessoRepository extends
        NextSdrQueryDslRepository<Permesso, Integer, QPermesso>,
        JpaRepository<Permesso, Integer> {
    
    @Query(value = 
        "WITH permessi_da_spegnere AS (\n" +
        "	SELECT p.id AS id_permesso, es.id_provenienza AS id_persona, eo.id_provenienza AS id_archivio, a.livello\n" +
        "	FROM permessi.permessi p\n" +
        "	JOIN permessi.entita es ON es.id = p.id_soggetto\n" +
        "	JOIN permessi.entita eo ON eo.id = p.id_oggetto\n" +
        "	JOIN permessi.tipi_entita tes ON tes.id = es.id_tipo_entita AND tes.target_table = 'persone' AND tes.target_schema = 'baborg'\n" +
        "	JOIN permessi.tipi_entita teo ON teo.id = eo.id_tipo_entita AND teo.target_table = 'archivi' AND teo.target_schema = 'scripta'\n" +
        "	JOIN scripta.archivi a ON a.id = eo.id_provenienza\n" +
        "	JOIN permessi.predicati pre ON pre.id = p.id_predicato \n" +
        "	WHERE es.id_provenienza IN (?1)\n" +
        "	AND a.id_archivio_radice IN (?2)\n" +
        "	AND p.attivo_dal < now() AND (p.attivo_al IS NULL OR p.attivo_al > now())\n" +
        "	AND pre.predicato IN ('VISUALIZZA', 'MODIFICA', 'ELIMINA', 'PASSAGGIO', 'BLOCCO')\n" +
        "       AND p.tipo = 'ARCHIVIO'\\:\\:permessi.tipo_permesso\n" +
        "	AND p.ambito = 'SCRIPTA'\\:\\:permessi.ambito_permesso\n" +
        "),\n" +
        "spegnimento AS (\n" +
        "	UPDATE permessi.permessi p\n" +
        "	SET attivo_al = now()\n" +
        "	FROM permessi_da_spegnere pds\n" +
        "	WHERE pds.id_permesso = p.id\n" +
        ")\n" +
        "SELECT id_persona AS \"idPersona\", id_archivio AS \"idArchivio\" \n" +
        "FROM permessi_da_spegnere\n" +
        "WHERE livello = 1",
        nativeQuery = true)
    public List<Map<String, Object>> spegniPermessiArchiviGestioneMassiva(
            Integer[] idsPersone,
            Integer[] idsArchivi
    );
    
    @Modifying
    @Query(value = 
        "WITH permessi_da_spegnere AS (\n" +
        "	SELECT p.id AS id_permesso, es.id_provenienza AS id_persona, eo.id_provenienza AS id_archivio, a.livello\n" +
        "	FROM permessi.permessi p\n" +
        "	JOIN permessi.entita es ON es.id = p.id_soggetto\n" +
        "	JOIN permessi.entita eo ON eo.id = p.id_oggetto\n" +
        "	JOIN permessi.tipi_entita tes ON tes.id = es.id_tipo_entita AND tes.target_table = 'persone' AND tes.target_schema = 'baborg'\n" +
        "	JOIN permessi.tipi_entita teo ON teo.id = eo.id_tipo_entita AND teo.target_table = 'archivi' AND teo.target_schema = 'scripta'\n" +
        "	JOIN scripta.archivi a ON a.id = eo.id_provenienza\n" +
        "	JOIN permessi.predicati pre ON pre.id = p.id_predicato \n" +
        "	WHERE es.id_provenienza = ?1 \n" +
        "	AND a.id_archivio_radice IN (?2)\n" +
        "	AND p.attivo_dal < now() AND (p.attivo_al IS NULL OR p.attivo_al > now())\n" +
        "	AND pre.predicato IN ('VISUALIZZA', 'MODIFICA', 'ELIMINA', 'PASSAGGIO', 'BLOCCO')\n" +
        "	AND p.tipo = 'ARCHIVIO'\\:\\:permessi.tipo_permesso\n" +
        "	AND p.ambito = 'SCRIPTA'\\:\\:permessi.ambito_permesso\n" +
        "	AND NOT (pre.predicato = ?3 AND propaga_oggetto = TRUE AND p.id_entita_veicolante = (\n" +
        "			SELECT ev.id \n" +
        "			FROM permessi.entita ev \n" +
        "			JOIN permessi.tipi_entita tev ON tev.id = ev.id_tipo_entita AND tev.target_table = 'strutture' AND tev.target_schema = 'baborg'\n" +
        "			WHERE ev.id_provenienza = ?4\n" +
        "		)\n" +
        "	)\n" +
        ")\n" +
        "UPDATE permessi.permessi p\n" +
        "SET attivo_al = now()\n" +
        "FROM permessi_da_spegnere pds\n" +
        "WHERE pds.id_permesso = p.id\n",
        nativeQuery = true)
    public void spegniPermessiArchiviGestioneMassivaPreservandoneAlcuni(
            Integer idPersona,
            Integer[] idsArchivi,
            String predicatoDaPreservare,
            Integer idStrutturaVeicolante
    );
    
    @Query(value = 
        "WITH valori_utili AS (\n" +
        "	SELECT \n" +
        "		(SELECT e.id FROM permessi.entita e \n" +
        "		JOIN permessi.tipi_entita te ON te.id = e.id_tipo_entita AND te.target_table = 'persone' AND te.target_schema = 'baborg'\n" +
        "		WHERE e.id_provenienza = ?1 ) AS id_soggetto,\n" +
        "		(SELECT ev.id FROM permessi.entita ev \n" +
                "        JOIN permessi.tipi_entita tev ON tev.id = ev.id_tipo_entita AND tev.target_table = 'strutture' AND tev.target_schema = 'baborg'\n" +
                "        WHERE ev.id_provenienza = ?2) AS id_veicolante,\n" +
                "        (SELECT id FROM permessi.predicati p WHERE p.predicato = ?3) AS id_predicato\n" +
        "),\n" +
        "permessi_da_inserire AS (" +
        "    SELECT \n" +
        "	eo.id as id_oggetto,\n" +
        "       a.id_archivio" +
        "    FROM UNNEST(STRING_TO_ARRAY(?4, ',')\\:\\:integer[]) AS a(id_archivio)\n" +
        "    JOIN permessi.entita eo ON eo.id_provenienza = a.id_archivio\n" +
        "    WHERE NOT EXISTS (\n" +
        "	SELECT 1 \n" +
        "	FROM permessi.permessi pp \n" +
        "	WHERE pp.id_soggetto = (SELECT v.id_soggetto FROM valori_utili v) \n" +
        "	AND pp.id_oggetto = eo.id \n" +
        "	AND pp.id_entita_veicolante = (SELECT v.id_veicolante FROM valori_utili v)\n" +
        "	AND pp.tipo = 'ARCHIVIO'\\:\\:permessi.tipo_permesso\n" +
        "	AND pp.ambito = 'SCRIPTA'\\:\\:permessi.ambito_permesso\n" +
        "	AND pp.propaga_oggetto = TRUE\n" +
        "	AND pp.id_predicato = (SELECT v.id_predicato FROM valori_utili v)\n" +
        "       AND pp.attivo_dal < now() AND (pp.attivo_al IS NULL OR pp.attivo_al > now())\n" +
        "    )\n" +
        "),\n" +        
        "inserimento AS (" + 
        "    INSERT INTO permessi.permessi (\n" +
        "	id_soggetto, id_oggetto, id_predicato, origine_permesso, \n" +
        "	propaga_soggetto, propaga_oggetto, ambito, tipo,\n" +
        "	id_entita_veicolante\n" +
        "    )\n" +
        "    SELECT \n" +
        "	(SELECT v.id_soggetto FROM valori_utili v), \n" +
        "	pdi.id_oggetto,\n" +
        "	(SELECT v.id_predicato FROM valori_utili v), \n" +
        "	'GESTIONE_MASSIVA_PERMESSI', \n" +
        "	FALSE, TRUE, 'SCRIPTA', 'ARCHIVIO',\n" +
        "	(SELECT v.id_veicolante FROM valori_utili v)\n" +
        "    FROM permessi_da_inserire pdi \n" +
        ")\n" + 
        "SELECT id_archivio as \"idArchivio\" FROM permessi_da_inserire",
        nativeQuery = true)
    public List<Map<String, Object>> insertPermessiArchiviGestioneMassiva(
            Integer idPersona,
            Integer idStrutturaVeicolante,
            String predicato,
            String idsArchiviString
    );
}
