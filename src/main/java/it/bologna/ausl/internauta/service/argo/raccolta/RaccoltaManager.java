package it.bologna.ausl.internauta.service.argo.raccolta;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Matteo Next
 */
public class RaccoltaManager {

    public static String queryRaccoltaSemplice() {
        String query = "SELECT r.id, r.id_gddoc "
                + ", r.id_gddoc_associato, r.codice \n "
                + ", r.applicazione_chiamante "
                + ", r.additional_data \n "
                + ", r.creatore, r.oggetto "
                + ", r.id_struttura_responsabile_internauta \n "
                + ", r.id_struttura_responsabile_argo, r.descrizione_struttura \n "
                + ", r.stato, r.storico, r.tipo_documento "
                + ", r.create_time \n "
                + " FROM gd.raccolte r \n"
                + "WHERE r.create_time::date >= :from \n"
                + "and r.create_time::date < :to \n";
        return query;
    }

    public static String queryCoinvoltiRaccolta(String id) {
        String query = "SELECT cr.id_coinvolto from gd.coinvolti_raccolte cr"
                + " WHERE cr.id_raccolta = '" + id + "'";
        return query;
    }

    public static String queryCodiceBabel(String id) {
        String query = "SELECT g.codice_registro, "
                + "g.numero_registrazione, "
                + "g.anno_registrazione "
                + "FROM gd.gddocs g "
                + " WHERE g.id_gddoc =  '" + id + "'";
        return query;
    }

    public static String queryNumerazioneGerarchica(String id) {
        String query = "select f.numerazione_gerarchica "
                + " from gd.fascicoligd f join gd.fascicoli_gddocs fg"
                + " on fg.id_fascicolo = f.id_fascicolo "
                + "	where fg.id_gddoc = '" + id + "'";
        return query;
    }

    public static String queryCoinvolti(String id) {
        String query = "SELECT c.id, c.nome, c.cognome, c.ragione_sociale, "
                + "c.descrizione, c.cf, c.partitaiva, c.tipologia, "
                + "c.id_contatto_internauta, c.mail, c.telefono, "
                + "c.via, c.civico, c.cap, c.comune, c.provincia, "
                + "c.nazione from gd.coinvolti c WHERE "
                + "c.id = " + id;
        return query;
    }

    public static String querySottoDocumenti(String id) {
        String query = "SELECT nome_sottodocumento, "
                + "mimetype_file_originale "
                + "FROM gd.sotto_documenti "
                + "WHERE id_gddoc = '" + id + "'";
        return query;
    }

    public static Map<String, String> mapSottoDocumenti() {
        Map<String, String> map = new HashMap<>();
        map.put("nome_sottodocumento", "nomeOriginale");
        map.put("mimetype_file_originale", "mimeTypeOriginale");
        return map;
    }

    public static Map<String, String> mapCoinvoltiRaccolta() {
        Map<String, String> map = new HashMap<>();
        map.put("id_coinvolto", "idCoinvolto");
        return map;
    }

    public static Map<String, String> mapCoinvolti() {
        Map<String, String> map = new HashMap<>();
        map.put("nome", "nome");
        map.put("cognome", "cognome");
        map.put("ragione_sociale", "ragioneSociale");
        map.put("descrizione", "descrizione");
        map.put("cf", "cf");
        map.put("partitaiva", "partitaiva");
        map.put("tipologia", "tipo");
        map.put("id_contatto_internauta", "idContattoInternauta");
        map.put("mail", "mail");
        map.put("telefono", "telefono");
        map.put("via", "via");
        map.put("civico", "civico");
        map.put("cap", "cap");
        map.put("comune", "comune");
        map.put("provincia", "provincia");
        map.put("nazione", "nazione");
        return map;
    }

    public static String queryInfoSottoDocumenti(String id) {
        String query = "SELECT s.nome_sottodocumento, s.guid_sottodocumento, "
                + "s.mimetype_file_originale, s.uuid_mongo_originale, s.id_gddoc, lower(f.estensione) as estensione "
                + "FROM gd.sotto_documenti s join bds_tools.file_supportati f on s.mimetype_file_originale = f.mime_type "
                + "WHERE s.guid_sottodocumento = '" + id + "'";
        return query;
    }

    public static Map<String, String> mapNumerazioneGerarchica() {
        Map<String, String> map = new HashMap<>();
        map.put("numerazione_gerarchica", "numerazioneGerarchica");
        return map;
    }

    public static String queryGetStorico(String id) {
        String query = "SELECT storico from "
                + "gd.raccolte WHERE id = " + id + " ";
        return query;
    }

    public static String queryUpdateStorico(String storico, String id, String stato) {
        String query = "UPDATE gd.raccolte "
                + "SET storico = storico || '" + storico + "', "
                + "stato = '" + stato + "' "
                + "WHERE id = " + id + " ";
        return query;
    }

    public static Map<String, String> mapQueryStorico() {
        Map<String, String> map = new HashMap<>();
        map.put("storico", "lista");
        return map;
    }

    public static Map<String, String> mapQueryCodiceBabel() {
        Map<String, String> map = new HashMap<>();
        map.put("numero_registrazione", "numero");
        map.put("codice_registro", "codiceRegistro");
        map.put("anno_registrazione", "anno");
        return map;
    }

    public static Map<String, String> mapQueryGetRaccoltaSemplice() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("id", "id");
        mappings.put("id_gddoc", "idGddoc");
        mappings.put("id_gddoc_associato", "idGddocAssociato");
        mappings.put("codice", "codice");
        mappings.put("applicazione_chiamante", "applicazioneChiamante");
        mappings.put("creatore", "creatore");
        mappings.put("additional_data", "additionalData");
        mappings.put("id_struttura_responsabile_internauta", "idStrutturaResponsabileInternauta");
        mappings.put("id_struttura_responsabile_argo", "idStrutturaResponsabileArgo");
        mappings.put("descrizione_struttura", "descrizioneStruttura");
        mappings.put("stato", "stato");
        mappings.put("tipo_documento", "tipoDocumento");
        mappings.put("oggetto", "oggetto");
        mappings.put("storico", "storico");
        mappings.put("create_time", "createTime");
        mappings.put("tscol", "tscol");
        mappings.put("version", "version");
        return mappings;
    }

    public static Map<String, String> mapQueryGetFascicoli() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("guid_fascicolo", "guidFascicolo");
        mappings.put("id_livello_fascicolo", "idLivelloFascicolo");
        mappings.put("nome_fascicolo_interfaccia", "nomeFascicoloInterfaccia");
        mappings.put("numero_fascicolo", "numeroFascicolo");
        mappings.put("nome_fascicolo", "nomeFascicolo");
        mappings.put("anno_fascicolo", "annoFascicolo");
        mappings.put("id_utente_creazione", "idUtenteCreazione");
        mappings.put("numerazione_gerarchica", "numerazioneGerarchica");
        mappings.put("id_utente_responsabile", "idUtenteResponsabile");

        return mappings;
    }

    public static String queryGetFascicoli(String idUtente, String strToFind) {

        // La stringa cercata è una numerazione gerarchica? (Deve iniziare con un numero)
        boolean matcha = strToFind.matches("(\\d+-?)"
                + // n- , n
                "|(\\d+/\\d*)"
                + // n/ , n/n
                "|(\\d+\\-\\d+/?)"
                + // n-n/ , n-n
                "|(\\d+\\-\\d+/\\d+)"
                + // n-n/n
                "|(\\d+\\-\\d+\\-\\d*)"
                + // n-n- , n-n-n
                "|(\\d+\\-\\d+\\-\\d+/\\d*)");   // n-n-n/n , n-n-n/

        String whereCondition;

        if (matcha) {
            // E' una numerazione gerarchica completa?
            if (strToFind.matches("(\\d+/\\d) | (\\d+\\-\\d+/\\d+) | (\\d+\\-\\d+\\-\\d+/\\d)")) {
                whereCondition = "where f.numerazione_gerarchica = '" + strToFind + "' "; // In questo caso uso l'uguale
            } else {
                whereCondition = "where f.numerazione_gerarchica like ('" + strToFind + "%')"; // In questo caso si vuole l'"inizia con"
            }
        } else {
            whereCondition = "where f.nome_fascicolo ilike ('%" + strToFind + "%')";
        }

        String query = "select distinct(f.guid_fascicolo), f.id_livello_fascicolo, "
                + "CASE f.id_livello_fascicolo WHEN '2' THEN (select nome_fascicolo from gd.fascicoligd where f.id_fascicolo_padre = id_fascicolo) "
                + "WHEN '3' THEN (select nome_fascicolo from gd.fascicoligd where id_fascicolo = (select id_fascicolo_padre from gd.fascicoligd where f.id_fascicolo_padre = id_fascicolo)) "
                + "ELSE nome_fascicolo "
                + "END as nome_fascicolo_interfaccia, "
                + "f.numero_fascicolo, "
                + "f.nome_fascicolo,f.anno_fascicolo, f.id_utente_creazione, "
                + "f.numerazione_gerarchica, f.id_utente_responsabile, t.codice_gerarchico || '' || t.codice_titolo || ' ' || t.titolo as titolo "
                + "from "
                + "gd.fascicoligd f "
                + "left join procton.titoli t on t.id_titolo = f.id_titolo "
                + "left join gd.log_azioni_fascicolo laf on laf.id_fascicolo= f.id_fascicolo "
                + "join gd.fascicoli_modificabili fv on fv.id_fascicolo=f.id_fascicolo and fv.id_utente= '" + idUtente + "' "
                + "join procton.utenti uResp on uResp.id_utente=f.id_utente_responsabile "
                + "join procton.utenti uCrea on f.id_utente_creazione=uCrea.id_utente "
                + whereCondition + " "
                + "and f.numero_fascicolo != '0' and f.speciale != -1  "
                + "and f.stato_fascicolo != 'c' "
                + "and f.stato_fascicolo != 'p' order by f.nome_fascicolo";

        return query;
    }

    public static Map<String, String> mapQueryGetDocumentiBabel() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("guid_gddoc", "guidGddoc");
        mappings.put("guid_documento", "guidDocumento");
        mappings.put("oggetto", "oggetto");
        mappings.put("protocollo", "numero");
        mappings.put("anno_protocollo", "anno");
        mappings.put("data_protocollo", "dataProtocollo");
        mappings.put("data_documento", "dataDocumento");
        mappings.put("codice_registro", "codiceRegistro");

        return mappings;
    }

    public static String queryGetProtocolliBabel(String idUtente, String numero, Integer anno, String oggetto) {

        String query = "SELECT DISTINCT d.guid_documento, g.codice_registro, g.guid_gddoc, d.oggetto, d.protocollo, d.anno_protocollo, d.data_documento, d.data_protocollo "
                + "FROM procton.documenti d, gd.gddocs g "
                + "WHERE g.id_oggetto_origine = 'babel_suite_' || d.guid_documento "
                + "AND protocollo is not null "
                + "AND  '[{\"u\":\"" + idUtente + "\"}]'::jsonb<@d.utenti_vedenti_json "
                + (numero != null ? "AND d.protocollo = lpad('" + numero + "', 7, '0') " : "")
                + (anno != null ? "AND d.anno_protocollo = " + anno + " " : "")
                + "AND d.annullato = 0 "
                + (oggetto != null ? "AND d.oggetto ilike '%" + oggetto + "%'" : "")
                + "ORDER BY d.data_protocollo desc nulls last, d.data_documento desc limit 300";

        return query;
    }
}
