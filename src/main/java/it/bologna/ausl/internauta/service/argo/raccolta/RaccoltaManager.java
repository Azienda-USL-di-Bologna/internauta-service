/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.raccolta;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

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
                + " WHERE cr.id_raccolta = '" + id +"'";
        return query;
    }
    
    public static String queryCodiceBabel(String id) {
        String query = "SELECT g.codice_registro, "
                + "g.numero_registrazione, "
                + "g.anno_registrazione "
                + "FROM gd.gddocs g "              
                +" WHERE g.id_gddoc =  '" + id +"'";
        return query;                
    }
    
    public static String queryNumerazioneGerarchica(String id) {
        String query = "select f.numerazione_gerarchica " 
            + " from gd.fascicoligd f join gd.fascicoli_gddocs fg"
            + " on fg.id_fascicolo = f.id_fascicolo " 
            + "	where fg.id_gddoc = '"+ id +"'";
        return query;
    }
    
    public static String queryCoinvolti(String id) {
        String query = "SELECT c.id, c.nome, c.cognome, c.ragione_sociale, "
                + "c.descrizione, c.cf, c.partitaiva, c.tipologia, "
                + "c.id_contatto_internauta, c.mail, c.telefono, "
                + "c.via, c.civico, c.cap, c.comune, c.provincia, "
                + "c.nazione from gd.coinvolti c WHERE "
                + "c.id = "+id;
        return query;
    }
        
    public static String querySottoDocumenti(String id) {
        String query = "SELECT nome_sottodocumento, "
                + "mimetype_file_originale "
                + "FROM gd.sotto_documenti "
                + "WHERE id_gddoc = '"+id+"'";
        return query;
    }
    
    public static Map<String, String> mapSottoDocumenti() {
        Map<String, String> map = new HashMap<>();
        map.put("nome_sottodocumento", "nomeOriginale");
        map.put("mimetype_file_originale", "mimeTypeOriginale");
        return map;
    }
    
    public static Map<String,String> mapCoinvoltiRaccolta() {
        Map<String, String> map = new HashMap<>();
        map.put("id_coinvolto", "idCoinvolto");
        return map;
    }
    
    public static Map<String, String> mapCoinvolti() {
        Map<String, String> map = new HashMap<>();
        map.put("nome", "nome");
        map.put("cognome" , "cognome");
        map.put("ragione_sociale", "ragioneSociale");
        map.put("descrizione", "descrizione");
        map.put("cf", "cf");
        map.put("partitaiva","partitaiva");
        map.put("tipologia", "tipo");
        map.put("id_contatto_internauta", "idContattoInternauta");
        map.put("mail", "mail");
        map.put("telefono", "telefono");
        map.put("via","via");
        map.put("civico", "civico");
        map.put("cap","cap");
        map.put("comune", "comune");
        map.put("provincia", "provincia");
        map.put("nazione", "nazione");
        return map;                
    }
    
    public static Map<String, String> mapNumerazioneGerarchica() {
        Map<String, String> map = new HashMap<>();
        map.put("numerazione_gerarchica", "numerazioneGerarchica");
        return map;
    }
    
    public static String queryGetStorico(String id) {
        String query = "SELECT storico from "
                + "gd.raccolte WHERE id = "+id+" ";
        return query;
    }
    
    public static String queryUpdateStorico(String storico, String id, String stato) {
        String query = "UPDATE gd.raccolte "
                + "SET storico = storico || '" + storico +"', "
                + "stato = '" + stato +"' "
                + "WHERE id = "+ id +" ";
        return query;
    }
    
    public static Map<String, String> mapQueryStorico() {
        Map<String, String> map = new HashMap<>();
        map.put("storico", "lista");
        return map;
    }
    
    public static Map<String,String> mapQueryCodiceBabel() {
        Map<String, String> map = new HashMap<>();
        map.put("numero_registrazione", "numeroRegistro");
        map.put("codice_registro", "codiceRegistro");
        map.put("anno_registrazione", "annoRegistro");
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
        mappings.put("tipo_documento","tipoDocumento");
        mappings.put("oggetto","oggetto");
        mappings.put("storico", "storico");
        mappings.put("create_time", "createTime");
        return mappings;
    }

}
