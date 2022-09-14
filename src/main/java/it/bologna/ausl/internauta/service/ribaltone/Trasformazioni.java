package it.bologna.ausl.internauta.service.ribaltone;

import java.time.ZonedDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.ICsvMapReader;

/**
 *
 * @author Top
 */
public class Trasformazioni {

    private static final Logger log = LoggerFactory.getLogger(Trasformazioni.class);

    public static Integer checkProgressivoRiga(Map<String, Object> trasformazioniMap, Map<String, Object> mapError, ICsvMapReader mapReader) {
        if (trasformazioniMap.get("progressivo_riga") == null || trasformazioniMap.get("progressivo_riga").toString().trim().equals("") || trasformazioniMap.get("progressivo_riga") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " progressivo_riga,");
            mapError.put("progressivo_riga", "");
            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " progressivo_riga assente");
            return null;
        } else {
            mapError.put("progressivo_riga", trasformazioniMap.get("progressivo_riga"));
            return Integer.parseInt(trasformazioniMap.get("progressivo_riga").toString());
        }
    }

    public static ZonedDateTime checkDataInPartenza(Map<String, Object> trasformazioniMap, Map<String, Object> mapError, ICsvMapReader mapReader) {
        if (trasformazioniMap.get("datain_partenza") == null || trasformazioniMap.get("datain_partenza").toString().trim().equals("") || trasformazioniMap.get("datain_partenza") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " datain_partenza assente,");
            mapError.put("datain_partenza", "");
            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " datain_partenza assente");
            return null;
        } else {
            mapError.put("datain_partenza", trasformazioniMap.get("datain_partenza"));
            return ImportaDaCSVUtils.formattattore(trasformazioniMap.get("datain_partenza"));
        }
    }

    public static ZonedDateTime checkDataOraOper(Map<String, Object> trasformazioniMap, Map<String, Object> mapError) {
        if (trasformazioniMap.get("dataora_oper") == null || trasformazioniMap.get("dataora_oper").toString().trim().equals("")) {
            mapError.put("ERRORE", mapError.get("ERRORE") + " DATAORA_OPER inserito automaticamente,");
            ZonedDateTime now = ZonedDateTime.now();
            mapError.put("dataora_oper", now.toString());
            return now;
        } else {
            mapError.put("dataora_oper", trasformazioniMap.get("dataora_oper"));
            return ImportaDaCSVUtils.formattattore(trasformazioniMap.get("dataora_oper"));
        }
    }

    public static ZonedDateTime checkDataTrasformazione(Map<String, Object> trasformazioniMap, Map<String, Object> mapError, ICsvMapReader mapReader) {
        if (trasformazioniMap.get("data_trasformazione") == null || trasformazioniMap.get("data_trasformazione").toString().trim().equals("") || trasformazioniMap.get("data_trasformazione") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " data_trasformazione assente,");
            mapError.put("data_trasformazione", "");
            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " data_trasformazione assente");
            return null;
        } else {
            mapError.put("data_trasformazione", trasformazioniMap.get("data_trasformazione"));
            return ImportaDaCSVUtils.formattattore(trasformazioniMap.get("data_trasformazione"));
        }
    }

    public static Integer checkIdCasellaPartenza(Map<String, Object> trasformazioniMap, Map<String, Object> mapError, ICsvMapReader mapReader) {
        if (trasformazioniMap.get("id_casella_partenza") == null || trasformazioniMap.get("id_casella_partenza").toString().trim().equals("") || trasformazioniMap.get("id_casella_partenza") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella_partenza,");
            mapError.put("id_casella_partenza", "");
            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " id_casella_partenza assente");
            return -1;
        } else {
            mapError.put("id_casella_partenza", trasformazioniMap.get("id_casella_partenza"));
            return Integer.parseInt(trasformazioniMap.get("id_casella_partenza").toString());
        }
    }
}
