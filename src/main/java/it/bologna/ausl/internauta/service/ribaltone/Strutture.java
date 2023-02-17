package it.bologna.ausl.internauta.service.ribaltone;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class Strutture {

    private static final Logger log = LoggerFactory.getLogger(Strutture.class);

    public static Boolean checkAccesaSpentaMale(List<Map<String, Object>> date, ZonedDateTime dataTrasformazione, ZonedDateTime dataInPartenza) {
        for (Map<String, Object> data : date) {
            if (    ImportaDaCSVUtils.formattattore(data.get("datafi")) != null && 
                    ImportaDaCSVUtils.formattattore(data.get("datain").toString()).equals(dataInPartenza) &&
                    ImportaDaCSVUtils.formattattore(data.get("datafi")).equals(dataTrasformazione.minusDays(1))) {
                return false;
            }
        }
        return true;
    }

    public static String checkDescrizione(Map<String, Object> strutturaMap, Map<String, Object> mapError, Integer lineNumber) {
        if (strutturaMap.get("descrizione") == null || strutturaMap.get("descrizione").toString().trim().equals("") || strutturaMap.get("descrizione") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " descrizione assente,");
            mapError.put("descrizione", "");
            log.error("Importa CSV --Struttura-- errore alla righa:" + lineNumber.toString() + " descrizione vuota");
            return "";
        } else {
            mapError.put("descrizione", strutturaMap.get("descrizione"));
            return strutturaMap.get("descrizione").toString().replaceAll("(\\n\\r+)|(\\n+)", " ");

        }
    }

    public static String checkIdCasella(Map<String, Object> strutturaMap,
            Map<String, Object> mapError,
            Integer lineNumber,
            Map<Integer, List<Map<String, Object>>> strutturaCheckDateMap) {

        if (strutturaMap.get("id_casella") == null || strutturaMap.get("id_casella").toString().trim().equals("")) {
            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella assente,");
            log.error("Importa CSV --Struttura-- errore alla righa:" + lineNumber + " idCasella vuota");
            mapError.put("id_casella", "");
            return "";
        } else {
            Integer idCasella = Integer.parseInt(strutturaMap.get("id_casella").toString());
            mapError.put("id_casella", strutturaMap.get("id_casella"));

            if (strutturaCheckDateMap.get(idCasella) == null) {
                List<Map<String, Object>> listaMapDataInDataFi = new ArrayList();
                Map<String, Object> mapDataInDataFi = new HashMap();
                mapDataInDataFi.put("datain", strutturaMap.get("datain"));
                mapDataInDataFi.put("datafi", strutturaMap.get("datafi"));
                listaMapDataInDataFi.add(mapDataInDataFi);
                strutturaCheckDateMap.put(idCasella, listaMapDataInDataFi);
            } else {
                //struttura definita piu volte nello stesso arco temporale
                if ((strutturaMap.get("datain") != null) && (ImportaDaCSVUtils.isPeriodiSovrapposti(strutturaCheckDateMap.get(idCasella), ImportaDaCSVUtils.formattattore(strutturaMap.get("datain")), ImportaDaCSVUtils.formattattore(strutturaMap.get("datafi"))))) {
                    log.error("Importa CSV --Struttura-- errore alla righa:" + lineNumber.toString() + " idCasella definita piu volte");
                    if (strutturaMap.get("ERRORE") != null) {
                        mapError.put("ERRORE", mapError.get("ERRORE") + " struttura definita piu volte nello stesso arco temporale,");
                    } else {
                        mapError.put("ERRORE", " struttura definita piu volte nello stesso arco temporale,");
                    }
                }

                Map<String, Object> mapDataInDataFi = new HashMap();
                mapDataInDataFi.put("datain", strutturaMap.get("datain"));
                mapDataInDataFi.put("datafi", strutturaMap.get("datafi"));
                strutturaCheckDateMap.get(idCasella).add(mapDataInDataFi);
            }
            return strutturaMap.get("id_casella").toString();
        }
    }

    public static ZonedDateTime checkDatafi(Map<String, Object> strutturaMap, Map<String, Object> mapError) {
        if (strutturaMap.get("datafi") == null
                || strutturaMap.get("datafi").toString().trim().equals("")
                || strutturaMap.get("datafi") == ""
                || strutturaMap.get("datafi").toString().trim().equals("3000-12-31")
                || strutturaMap.get("datafi").toString().trim().equals("31/12/3000")) {
            mapError.put("datafi", "");
            return null;
        } else {
            mapError.put("datafi", strutturaMap.get("datafi"));
            return ImportaDaCSVUtils.formattattore(strutturaMap.get("datafi"));
        }
    }
}
