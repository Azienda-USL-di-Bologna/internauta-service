package it.bologna.ausl.internauta.service.ribaltone;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.supercsv.io.ICsvMapReader;

/**
 *
 * @author Top
 */
public class Appartenenti {

    private static final Logger log = LoggerFactory.getLogger(Appartenenti.class);

    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Autowired
    private AziendaRepository aziendaRepository;

    public static Boolean checkCodiceFiscale(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("codice_fiscale") == null || appartenentiMap.get("codice_fiscale").toString().trim().equals("") || appartenentiMap.get("codice_fiscale") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " CODICE FISCALE,");
            mapError.put("Anomalia", "true");
            mapError.put("codice_fiscale", "");
            return true;

        } else {
            if (appartenentiMap.get("nome") != null && appartenentiMap.get("cognome") != null) {
                if (appartenentiMap.get("codice_fiscale").toString().startsWith(partialCF(appartenentiMap.get("nome").toString(), appartenentiMap.get("cognome").toString()))) {
                    mapError.put("codice_fiscale", appartenentiMap.get("codice_fiscale"));
                } else {
                    mapError.put("ERRORE", mapError.get("ERRORE") + " CODICE FISCALE errato,");
                    mapError.put("Anomalia", "true");
                    mapError.put("codice_fiscale", appartenentiMap.get("codice_fiscale"));
                    return true;
                }

            }
        }
        return false;
    }

    private static String partialCF(String nome, String cognome) {
        return codiceCognome(cognome.trim()).concat(codiceNome(nome.trim()));
    }

    private static String codiceNome(String nome) {
        String vocali_NOME = getVocali(nome).concat("XXX");
        String consonanti_NOME = getConsonanti(nome);

        String s = "";
        if (consonanti_NOME.length() > 3) {
            s = s + consonanti_NOME.charAt(0) + consonanti_NOME.charAt(2) + consonanti_NOME.charAt(3);
            return s;
        }
        if (consonanti_NOME.length() == 3) {
            for (int i = 0; i < 3; i++) {
                s = s + consonanti_NOME.charAt(i);
            }
            return s;
        }
        if (consonanti_NOME.length() == 2) {
            s = s + consonanti_NOME.charAt(0) + consonanti_NOME.charAt(1) + vocali_NOME.charAt(0);
            return s;
        }
        if (consonanti_NOME.length() == 1) {
            s = s + consonanti_NOME.charAt(0) + vocali_NOME.charAt(0) + vocali_NOME.charAt(1);
            return s;
        } else {
            for (int i = 0; i < 3; i++) {
                s = s + vocali_NOME.charAt(i);
            }
            return s;
        }
    }

    public static String codiceCognome(String cognome) {
        String vocali_COGNOME = getVocali(cognome).concat("XXX");
        String consonanti_COGNOME = getConsonanti(cognome);
        String s = "";
        if (consonanti_COGNOME.length() >= 3) {
            for (int i = 0; i < 3; i++) {
                s = s + consonanti_COGNOME.charAt(i);
            }
            return s;
        }
        if (consonanti_COGNOME.length() == 2) {
            if (vocali_COGNOME.length() > 0) {
                s = s + consonanti_COGNOME.charAt(0) + consonanti_COGNOME.charAt(1) + vocali_COGNOME.charAt(0);
            }
            return s;
        }
        if (consonanti_COGNOME.length() == 1) {
            if (vocali_COGNOME.length() >= 2) {
                s = s + consonanti_COGNOME.charAt(0) + vocali_COGNOME.charAt(0) + vocali_COGNOME.charAt(1);
            }
            return s;
        } else {
            for (int i = 0; i < 3; i++) {
                s = s + vocali_COGNOME.charAt(i);
            }

            return s;
        }
    }

    private static String getConsonanti(String string) {

        return string.trim()
                .replaceAll("YẙỲỴỶỸŶŸÝ", "Y")
                .replaceAll("[^qQwWrRtTpPsSdDfFgGhHkKlLzZxXcCvVbBnNmMjJYy]", "");
    }

    private static String getVocali(String string) {
        return string.trim().toUpperCase()
                .replaceAll("[ÀÁÂÃÄÅĀĂĄǺȀȂẠẢẤẦẨẪẬẮẰẲẴẶḀ]", "A")
                .replaceAll("[ÆǼEȄȆḔḖḘḚḜẸẺẼẾỀỂỄỆĒĔĖĘĚÈÉÊË]", "E")
                .replaceAll("[IȈȊḬḮỈỊĨĪĬĮİÌÍÎÏĲ]", "I")
                .replaceAll("OŒØǾȌȎṌṎṐṒỌỎỐỒỔỖỘỚỜỞỠỢŌÒÓŎŐÔÕÖ", "O")
                .replaceAll("[UŨŪŬŮŰŲÙÚÛÜȔȖṲṴṶṸṺỤỦỨỪỬỮỰ]", "U")
                //.replaceAll("YẙỲỴỶỸŶŸÝ", "Y")
                .replaceAll("[^aAeEiIoOuU]", "");

    }

    public static Boolean checkCodiceMatricola(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("codice_matricola") == null || appartenentiMap.get("codice_matricola").toString().trim().equals("") || appartenentiMap.get("codice_matricola") == "") {
            mapError.put("Anomalia", "true");
            mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola,");
            mapError.put("codice_matricola", "");
            return true;
        } else {
            mapError.put("codice_matricola", appartenentiMap.get("codice_matricola"));
        }
        return false;
    }

    public static String checkIdCasella(Map<String, Object> appartenentiMap, Map<String, Object> mapError, Map<Integer, List<Map<String, Object>>> selectDateOnStruttureByIdAzienda) {
        if (appartenentiMap.get("id_casella") == null || appartenentiMap.get("id_casella").toString().trim().equals("") || appartenentiMap.get("id_casella") == "") {

            mapError.put("Anomalia", "true");
            mapError.put("ERRORE", mapError.get("ERRORE") + " IDCASELLA,");
            mapError.put("id_casella", "");
            return "";

        } else {
            mapError.put("id_casella", appartenentiMap.get("id_casella").toString());
            if (!selectDateOnStruttureByIdAzienda.containsKey(Integer.parseInt(appartenentiMap.get("id_casella").toString()))) {
                mapError.put("ERRORE", " manca la struttura nella tabella struttura,");
                mapError.put("Anomalia", "true");

                return "";
            } else {
                if (!ImportaDaCSVUtils.isPeriodiSovrapposti(selectDateOnStruttureByIdAzienda.get(Integer.parseInt(appartenentiMap.get("id_casella").toString())), ImportaDaCSVUtils.formattattore(appartenentiMap.get("datain")), ImportaDaCSVUtils.formattattore(appartenentiMap.get("datafi")))) {
                    mapError.put("ERRORE", mapError.get("ERRORE") + " non rispetta l arco temporale della struttura,");
                    mapError.put("Anomalia", "true");

                    return "";
                } else {
                    List<Map<String, Object>> elementi = selectDateOnStruttureByIdAzienda.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                    Map<String, ZonedDateTime> maxMin = ImportaDaCSVUtils.maxMin(elementi);
                    if (!ImportaDaCSVUtils.controllaEstremi(maxMin.get("min"), maxMin.get("max"), ImportaDaCSVUtils.formattattore(appartenentiMap.get("datain")), ImportaDaCSVUtils.formattattore(appartenentiMap.get("datafi")))) {
                        mapError.put("ERRORE", mapError.get("ERRORE") + " non rispetta l'arco temporale della struttura, ");
                        mapError.put("Anomalia", "true");

                        return "";
                    }
                }
            }

            return appartenentiMap.get("id_casella").toString();
        }
    }

    public static Boolean checkNome(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("nome") == null || appartenentiMap.get("nome").toString().trim().equals("") || appartenentiMap.get("nome") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " nome,");
            mapError.put("Anomalia", "true");
            mapError.put("nome", "");
            return true;
        } else {
            mapError.put("nome", appartenentiMap.get("nome"));
        }
        return false;
    }

    public static Boolean checkDataAssunzione(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("data_assunzione") == null || appartenentiMap.get("data_assunzione").toString().trim().equals("") || appartenentiMap.get("data_assunzione") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " data_assunzione,");
            mapError.put("data_assunzione", "");
            mapError.put("Anomalia", "true");
            return true;
        } else {
            mapError.put("data_assunzione", appartenentiMap.get("data_assunzione"));
        }
        return false;
    }

    public static Boolean checkCognome(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("cognome") == null || appartenentiMap.get("cognome").toString().trim().equals("") || appartenentiMap.get("cognome") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " cognome,");
            mapError.put("Anomalia", "true");
            mapError.put("cognome", "");
            return true;
        } else {
            mapError.put("cognome", appartenentiMap.get("cognome"));
        }
        return false;
    }

    public static List<Integer> codiciMatricoleConAppFunzionaliENonDirette(Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiFunzionali, Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiDiretti) {
        List<Integer> codiciMatricoleConAppFunzionaliENonDirette = new ArrayList<>();
        for (Integer codiceMatricola : appartenentiFunzionali.keySet()) {
            if (!appartenentiDiretti.containsKey(codiceMatricola)) {
                codiciMatricoleConAppFunzionaliENonDirette.add(codiceMatricola);
            }
        }
        return codiciMatricoleConAppFunzionaliENonDirette;
    }

    // lo scopo di questa funzione e' ritornare anomalia quando becco i casi:
    // - appartenenza di tipo T multiple
    // - appartenenza di tipo F su stessa casella casella con tipo T
    // - tipo appartenenza assente 
    // 
    public static boolean checkTipoAppatenenza(
            Map<String, Object> appartenentiMap,
            Map<String, Object> mapError,
            String idCasella,
            ZonedDateTime datain,
            ZonedDateTime datafi,
            Boolean controlloZeroUno,
            Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiDiretti,
            Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiFunzionali,
            ICsvMapReader mapReader,
            List<Integer> righeAnomaleFunzionali,
            List<Integer> righeAnomaleDirette) {

        Boolean anomalia = false;
        if (appartenentiMap.get("tipo_appartenenza") == null || appartenentiMap.get("tipo_appartenenza").toString().trim().equals("") || appartenentiMap.get("tipo_appartenenza") == "" || idCasella.equals("")) {
            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo appartenenza assente,");
            mapError.put("tipo_appartenenza", "");
            mapError.put("Anomalia", "true");
            return true;

        } else {
            mapError.put("tipo_appartenenza", appartenentiMap.get("tipo_appartenenza"));
            if (appartenentiMap.get("codice_ente") != null && !appartenentiMap.get("codice_ente").toString().trim().equals("") && appartenentiMap.get("codice_ente") != "") {
                boolean codiceEnteEndsWith = appartenentiMap.get("codice_ente").toString().endsWith("01");
                if (appartenentiMap.get("tipo_appartenenza").toString().trim().equalsIgnoreCase("T")) {
                    Map<Integer, List<Map<String, Object>>> appDiretto = appartenentiDiretti.get(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()));
                    //controlloZeroUno true controlla solo le afferenze degli gli appartententi che hanno codice ente che finisce con 01
                    if (codiceEnteEndsWith && controlloZeroUno) {

                        if (appDiretto == null) {
                            //non ho quella matricola nella mappa
                            //creo tutti i contenuti della matricola nuova
                            appDiretto = new HashMap();
                            List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                            Map<String, Object> periodoDaCasellare = new HashMap();
                            Integer idCasellaInt = Integer.parseInt(idCasella);
                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                            periodoCasellato.add(periodoDaCasellare);
                            appDiretto.put(idCasellaInt, periodoCasellato);
                            appartenentiDiretti.put(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()), appDiretto);
                        } else {
                            Boolean afferenzaDiretta = false;
                            //l'appartenente c'è devo ciclare su tutte le strutture per verificare che non abbia piu afferenze dirette

                            for (Map.Entry<Integer, List<Map<String, Object>>> listaCasella : appDiretto.entrySet()) {

                                if (!afferenzaDiretta && ImportaDaCSVUtils.isPeriodiSovrapposti(listaCasella.getValue(), datain, datafi)) {
                                    if (!righeAnomaleDirette.contains(mapReader.getLineNumber())) {
                                        righeAnomaleDirette.add(mapReader.getLineNumber());
                                    }
                                    mapError.put("Anomalia", "true");
                                    afferenzaDiretta = true;
                                    List<Integer> righeAnomaleDaControllare = ImportaDaCSVUtils.arco(listaCasella.getValue(), datain, datafi);
                                    for (Integer rigaAnomala : righeAnomaleDaControllare) {
                                        if (!righeAnomaleDirette.contains(rigaAnomala)) {
                                            righeAnomaleDirette.add(rigaAnomala);
                                        }
                                    }
                                    anomalia = true;
                                }
                            }

                            //integer1 appartenenti, integer2 struttura, lista datain,datafi di appartenente in struttura.
                            //Da modificare
                            List<Map<String, Object>> periodoCasellato = appDiretto.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                            if (periodoCasellato == null) {
                                periodoCasellato = new ArrayList<>();
                                Map<String, Object> periodoDaCasellare = new HashMap();
                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                periodoCasellato.add(periodoDaCasellare);
                                appDiretto.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                            } else {
                                if (!afferenzaDiretta && ImportaDaCSVUtils.isPeriodiSovrapposti(periodoCasellato, datain, datafi)) {
                                    if (!righeAnomaleDirette.contains(mapReader.getLineNumber())) {
                                        righeAnomaleDirette.add(mapReader.getLineNumber());
                                    }
                                    mapError.put("Anomalia", "true");
                                    List<Integer> righeAnomaleDaControllare = ImportaDaCSVUtils.arco(periodoCasellato, datain, datafi);
                                    for (Integer rigaAnomala : righeAnomaleDaControllare) {
                                        if (!righeAnomaleDirette.contains(rigaAnomala)) {
                                            righeAnomaleDirette.add(rigaAnomala);
                                        }
                                    }
                                    anomalia = true;
                                }
                                Map<String, Object> periodoDaCasellare = new HashMap();
                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                periodoCasellato.add(periodoDaCasellare);
                            }
                        }
                    }
                    //cazzo di Ferrarra di merda
                    if (!controlloZeroUno) {
                        if (appDiretto == null) {
                            //non ho quella matricola nella mappa
                            //creo tutti i contenuti della matricola nuova
                            appDiretto = new HashMap();
                            List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                            Map<String, Object> periodoDaCasellare = new HashMap();
                            Integer idCasellaInt = Integer.parseInt(idCasella);
                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                            periodoCasellato.add(periodoDaCasellare);
                            appDiretto.put(idCasellaInt, periodoCasellato);
                            appartenentiDiretti.put(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()), appDiretto);
                        } else {
                            Boolean afferenzaDiretta = false;

                            List<Map<String, Object>> periodoCasellato = appDiretto.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                            if (periodoCasellato == null) {
                                periodoCasellato = new ArrayList<>();
                                Map<String, Object> periodoDaCasellare = new HashMap();
                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                periodoCasellato.add(periodoDaCasellare);
                                appDiretto.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                            } else {

                                Map<String, Object> periodoDaCasellare = new HashMap();
                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                periodoCasellato.add(periodoDaCasellare);
                            }
                        }
                    } else if (!codiceEnteEndsWith && !controlloZeroUno) {
                        //caso in cui non finisco per 01 ma ho il controllo 01 attivo il periodo
                        if (appDiretto == null) {
                            //non ho quella matricola nella mappa
                            //creo tutti i contenuti della matricola nuova
                            appDiretto = new HashMap();
                            List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                            Map<String, Object> periodoDaCasellare = new HashMap();
                            Integer idCasellaInt = Integer.parseInt(idCasella);
                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                            periodoCasellato.add(periodoDaCasellare);
                            appDiretto.put(idCasellaInt, periodoCasellato);
                            appartenentiDiretti.put(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()), appDiretto);
                        } else {
                            Boolean afferenzaDiretta = false;

                            List<Map<String, Object>> periodoCasellato = appDiretto.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                            if (periodoCasellato == null) {
                                periodoCasellato = new ArrayList<>();
                                Map<String, Object> periodoDaCasellare = new HashMap();
                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                periodoCasellato.add(periodoDaCasellare);
                                appDiretto.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                            } else {

                                Map<String, Object> periodoDaCasellare = new HashMap();
                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                periodoCasellato.add(periodoDaCasellare);
                            }
                        }
                    }
                } else {
                    Map<Integer, List<Map<String, Object>>> appFunzionale = appartenentiFunzionali.get(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()));
                    if (appFunzionale == null) {
                        //non ho quella matricola nella mappa
                        //adda crea tutto
                        appFunzionale = new HashMap();
                        List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                        appFunzionale.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                        Map<String, Object> periodoDaCasellare = new HashMap();
                        periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                        periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                        periodoDaCasellare.put("riga", mapReader.getLineNumber());
                        periodoCasellato.add(periodoDaCasellare);
                        appartenentiFunzionali.put(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()), appFunzionale);
                    } else {
                        List<Map<String, Object>> periodoCasellato = appFunzionale.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                        if (periodoCasellato == null) {
                            periodoCasellato = new ArrayList<>();
                            Map<String, Object> periodoDaCasellare = new HashMap();
                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                            periodoCasellato.add(periodoDaCasellare);
                            appFunzionale.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                        } else {

                            if (ImportaDaCSVUtils.isPeriodiSovrapposti(periodoCasellato, datain, datafi)) {
                                mapError.put("Anomalia", "true");

                                if (!righeAnomaleFunzionali.contains(mapReader.getLineNumber())) {
                                    righeAnomaleFunzionali.add(mapReader.getLineNumber());
                                }
                                List<Integer> righeAnomaleDaControllare = ImportaDaCSVUtils.arco(periodoCasellato, datain, datafi);
                                for (Integer rigaAnomala : righeAnomaleDaControllare) {
                                    if (!righeAnomaleFunzionali.contains(rigaAnomala)) {
                                        righeAnomaleFunzionali.add(rigaAnomala);
                                    }
                                }
                                anomalia = true;

                            }
                            Map<String, Object> periodoDaCasellare = new HashMap();
                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                            periodoCasellato.add(periodoDaCasellare);
                        }
                    }

                }
            }
        }
        //adesso controllo che la riga di appartenenza diretta o funzionale non abbia sovrapposizioni temporali cross afferenza 
        if (appartenentiMap.get("tipo_appartenenza").toString().trim().equalsIgnoreCase("T")) {
            if (appartenentiMap.get("codice_matricola") != null && !appartenentiMap.get("codice_matricola").toString().trim().equals("")) {
                Integer codiceMatricola = Integer.parseInt(appartenentiMap.get("codice_matricola").toString());
                if (appartenentiFunzionali.containsKey(codiceMatricola)) {
                    if (appartenentiMap.get("id_casella") != null && !appartenentiMap.get("id_casella").toString().trim().equals("")) {
                        Integer idCasellaApp = Integer.parseInt(appartenentiMap.get("id_casella").toString());
                        if (appartenentiFunzionali.get(codiceMatricola).containsKey(idCasellaApp)) {
                            if (ImportaDaCSVUtils.isPeriodiSovrapposti(
                                    appartenentiFunzionali.get(codiceMatricola).get(idCasellaApp),
                                    ImportaDaCSVUtils.formattattore(appartenentiMap.get("datain")),
                                    ImportaDaCSVUtils.formattattore(appartenentiMap.get("datafi")))) {
                                anomalia = true;
                                mapError.put("Anomalia", "true");
                                mapError.put("ERRORE", "apparteneza diretta che si sovrappone ad una funzionale");
                            }
                        }
                    }
                }
            }
        }
        if (appartenentiMap.get("tipo_appartenenza").toString().trim().equalsIgnoreCase("F")) {
            if (appartenentiMap.get("codice_matricola") != null && !appartenentiMap.get("codice_matricola").toString().trim().equals("")) {
                Integer codiceMatricola = Integer.parseInt(appartenentiMap.get("codice_matricola").toString());
                if (appartenentiDiretti.containsKey(codiceMatricola)) {
                    if (appartenentiMap.get("id_casella") != null && !appartenentiMap.get("id_casella").toString().trim().equals("")) {
                        Integer idCasellaApp = Integer.parseInt(appartenentiMap.get("id_casella").toString());
                        if (appartenentiDiretti.get(codiceMatricola).containsKey(idCasellaApp)) {                           
                            if (ImportaDaCSVUtils.isPeriodiSovrapposti(
                                    appartenentiDiretti.get(codiceMatricola).get(idCasellaApp),
                                    ImportaDaCSVUtils.formattattore(appartenentiMap.get("datain")),
                                    ImportaDaCSVUtils.formattattore(appartenentiMap.get("datafi")))) {
                                anomalia = true;
                                mapError.put("Anomalia", "true");
                                mapError.put("ERRORE", "apparteneza funzionale che si sovrappone ad una diretta");
                            }
                        }
                    }
                }
            }
        }
        return anomalia;
    }
}
