package it.bologna.ausl.internauta.service.ribaltone;

import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.RibaltoneCSVCheckException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class Responsabili {

    private static final Logger log = LoggerFactory.getLogger(Responsabili.class);

    public static String checkCodiceMatricola(Map<String, Object> responsabiliMap, Map<String, Object> mapError, Map<Integer, List<Map<String, Object>>> selectDateOnAppartenentiByIdAzienda) {
        if (responsabiliMap.get("codice_matricola") == null || responsabiliMap.get("codice_matricola").toString().trim().equals("") || responsabiliMap.get("codice_matricola") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola,");
            mapError.put("codice_matricola", "");

            return "";
        } else {
            mapError.put("codice_matricola", responsabiliMap.get("codice_matricola"));
            //responsabile presente tra gli autenti
            if (!selectDateOnAppartenentiByIdAzienda.containsKey(Integer.parseInt(responsabiliMap.get("codice_matricola").toString()))) {
                mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola non trovata nella tabella appartenenti,");
                return "";
            }
            return responsabiliMap.get("codice_matricola").toString();
        }
    }

    public static String checkTipo(Map<String, Object> responsabiliMap, Map<String, Object> mapError) {
        if (responsabiliMap.get("tipo") == null || responsabiliMap.get("tipo").toString().trim().equals("") || responsabiliMap.get("tipo") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " manca il tipo afferenza,");
            return null;
        } else {
            mapError.put("tipo", responsabiliMap.get("tipo"));
            return responsabiliMap.get("tipo").toString();
        }
    }

    public static String checkIdCasella(Map<String, Object> responsabiliMap, Map<String, Object> mapError, Map<Integer, List<Map<String, Object>>> selectStruttureUtentiByIdAzienda, Integer idAzienda) throws RibaltoneCSVCheckException {
        if (responsabiliMap.get("id_casella") == null || responsabiliMap.get("id_casella").toString().trim().equals("") || responsabiliMap.get("id_casella") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella assente,");
            mapError.put("id_casella", "");
            return "";
        } else {
            mapError.put("id_casella", responsabiliMap.get("id_casella"));
//                            mR.setIdCasella(Integer.parseInt(responsabiliMap.get("id_casella").toString())); faccio il check della casella le domande sono queste esiste? vale nel periodo temporale? 
            if (!selectStruttureUtentiByIdAzienda.containsKey(Integer.parseInt(responsabiliMap.get("id_casella").toString()))) {
                mapError.put("ERRORE", mapError.get("ERRORE") + " casella non trovata nella tabella strutture,");
                throw new RibaltoneCSVCheckException("checkIdCasella", responsabiliMap.get("id_casella").toString(), " casella non trovata nella tabella strutture,");
            } else {
                List<Map<String, Object>> mieiPadri = selectStruttureUtentiByIdAzienda.get(Integer.parseInt(responsabiliMap.get("id_casella").toString()));
                if (responsabiliMap.get("datain") != null && !responsabiliMap.get("datain").toString().trim().equals("") && responsabiliMap.get("datain") != "") {
                    if (!ImportaDaCSVUtils.isPeriodiSovrapposti(mieiPadri, ImportaDaCSVUtils.formattattore(responsabiliMap.get("datain")), ImportaDaCSVUtils.formattattore(responsabiliMap.get("datafi")))) {
                        mapError.put("ERRORE", mapError.get("ERRORE") + " casella non valida per periodo temporale,");
                        mapError.put("Anomalia", "true");
                        throw new RibaltoneCSVCheckException("checkIdCasella", responsabiliMap.get("id_casella").toString(), " casella non valida per periodo temporale,");

                    } else {
                        Map<String, ZonedDateTime> maxMin = ImportaDaCSVUtils.maxMin(mieiPadri);
                        if (!ImportaDaCSVUtils.controllaEstremi(maxMin.get("min"), maxMin.get("max"), ImportaDaCSVUtils.formattattore(responsabiliMap.get("datain")), ImportaDaCSVUtils.formattattore(responsabiliMap.get("datafi")))) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " casella non rispetta l'arco temporale della struttura,");
                            mapError.put("Anomalia", "true");
                            throw new RibaltoneCSVCheckException("checkIdCasella", responsabiliMap.get("id_casella").toString(), " non rispetta l'arco temporale della struttura,");
                        }
                    }
                }
            }
            return responsabiliMap.get("id_casella").toString();
        }
    }
}
