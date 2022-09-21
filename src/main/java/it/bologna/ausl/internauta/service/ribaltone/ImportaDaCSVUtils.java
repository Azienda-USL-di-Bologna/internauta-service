package it.bologna.ausl.internauta.service.ribaltone;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
public class ImportaDaCSVUtils {

    private static final Logger log = LoggerFactory.getLogger(ImportaDaCSVUtils.class);

    public static List<Integer> arco(List<Map<String, Object>> elementi, ZonedDateTime dataInizio, ZonedDateTime dataFine) {
        List<Integer> lista = new ArrayList<>();
        if (!elementi.isEmpty()) {
            for (Map<String, Object> elemento : elementi) {
                if (overlap(formattattore(elemento.get("datain")), formattattore(elemento.get("datafi")), dataInizio, dataFine)) {
                    lista.add(Integer.parseInt(elemento.get("riga").toString()));
                }
            }
        }
        return lista;
    }

    public static ZonedDateTime formattattore(Object o) {
        if (o != null) {
            try {
                // String format = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
//                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(o.toString()).toInstant();
                return LocalDate.parse(o.toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay(ZoneId.systemDefault());
            } catch (Exception e) {

            }
            try {

                // String format = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                Instant toInstant = new SimpleDateFormat("dd/MM/yy").parse(o.toString()).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }
            try {
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(o.toString()).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }
            try {
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(o.toString()).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }

            try {
                String time = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }
            try {
                String time = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
            }

        }
        return null;
    }

    public static Boolean overlap(ZonedDateTime dataInizioA, ZonedDateTime dataFineA, ZonedDateTime dataInizioB, ZonedDateTime dataFineB) {

        if (dataFineA == null) {
            dataFineA = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        if (dataFineB == null) {
            dataFineB = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        return (dataInizioA.compareTo(dataFineB) <= 0 && dataFineA.compareTo(dataInizioB) >= 0) && dataInizioA.compareTo(dataInizioB) <= 0;
    }

    /**
     *
     * @param elementi
     * @param dataInizio
     * @param dataFine
     * @return false se elementi è vuoto
     */
    public static Boolean isPeriodiSovrapposti(List<Map<String, Object>> elementi, ZonedDateTime dataInizio, ZonedDateTime dataFine) {
        if (elementi.isEmpty()) {
            return false;
        }
        return elementi.stream().anyMatch(elemento -> ImportaDaCSVUtils.overlap(ImportaDaCSVUtils.formattattore(elemento.get("datain")), ImportaDaCSVUtils.formattattore(elemento.get("datafi")), dataInizio, dataFine));
    }

    public static Boolean controllaEstremi(ZonedDateTime dataStrutturaInizio, ZonedDateTime dataStrutturaFine, ZonedDateTime dataAppartenenteInizio, ZonedDateTime dataAppartenenteFine) {
        if (dataAppartenenteFine == null) {
            dataAppartenenteFine = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        if (dataStrutturaFine == null) {
            dataStrutturaFine = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        if (dataStrutturaFine.compareTo(dataAppartenenteFine) < 0) {
            return false;
        }

        if (dataStrutturaInizio.compareTo(dataAppartenenteInizio) > 0) {
            return false;
        }

        return true;
    }

    public static Map<String, ZonedDateTime> maxMin(List<Map<String, Object>> elementi) {
        HashMap<String, ZonedDateTime> maxmin = new HashMap<>();
        ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());

        for (Map<String, Object> map1 : elementi) {
            if (min.compareTo(ImportaDaCSVUtils.formattattore(map1.get("datain").toString())) > 0) {
                min = ImportaDaCSVUtils.formattattore(map1.get("datain").toString());
            }
            if (map1.get("datafi") == null) {
                max = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
            } else if (max.compareTo(ImportaDaCSVUtils.formattattore(map1.get("datafi").toString())) < 0) {
                max = ImportaDaCSVUtils.formattattore(map1.get("datafi").toString());
            }

        }
        maxmin.put("max", max);
        maxmin.put("min", min);
        return maxmin;
    }

    public static String checkCodiceEnte(Map<String, Object> xmap, Map<String, Object> mapError, String codiceAzienda) {
        if (xmap.get("codice_ente") == null || xmap.get("codice_ente").toString().trim().equals("") || xmap.get("codice_ente") == "") {
            mapError.put("codice_ente", "");
            mapError.put("ERRORE", mapError.get("Errore") + "codice ente assente,");
            mapError.put("Anomalia", "true");
            return codiceAzienda;
        } else {
            if (xmap.get("codice_ente").toString().length() <= 3) {
                mapError.put("ERRORE", mapError.get("Errore") + "codice ente troppo corto,");
                mapError.put("Anomalia", "true");
            }
            mapError.put("codice_ente", xmap.get("codice_ente"));
            return xmap.get("codice_ente").toString();
        }
    }

    public static boolean checkDateFinisconoDopoInizio(ZonedDateTime datain, ZonedDateTime datafi) {
        if (datain != null) {
            if (datafi == null) {
                return false;
            } else {
                return (datafi.isBefore(datain));
            }
        } else {
            return true;
        }
    }

    public static boolean checkDatain(Map<String, Object> appartenentiMap, Map<String, Object> mapError, String tipo) {
        if (appartenentiMap.get("datain") == null || appartenentiMap.get("datain").toString().trim().equals("") || appartenentiMap.get("datain") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " datain non presente,");
            mapError.put("datain", "");
            if (tipo.equals("A")) {
                mapError.put("Anomalia", "true");
            }
        } else {
            if (ImportaDaCSVUtils.formattattore(appartenentiMap.get("datain").toString()) != null) {
                mapError.put("datain", appartenentiMap.get("datain"));
                return true;
            } else {
                mapError.put("datain", appartenentiMap.get("datain"));
                if (tipo.equals("A")) {
                    mapError.put("Anomalia", "true");
                }
                mapError.put("ERRORE", mapError.get("ERRORE") + " datain non riconosciuta,");
            }
        }
        return false;
    }
}
