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
    
    public static Boolean checkDiretto(
            List<Map<String,Object>> listaPeriodiAfferenzaDiretta,
            ZonedDateTime datain,
            ZonedDateTime datafi,
            Map<String, Object> mapError,
            ICsvMapReader mapReader,
            List<Integer> righeAnomaleDirette,
            List<String> codiciMatricolaAnomaliaDiretta,
            String codiceMatricola){
    if (ImportaDaCSVUtils.isPeriodiSovrapposti(listaPeriodiAfferenzaDiretta,datain,datafi)){
        //mapError.put("ERRORE", mapError.get("ERRORE") + " doppia afferenza diretta per questo utente,");
        mapError.put("Anomalia", "true");
        if (!righeAnomaleDirette.contains(mapReader.getLineNumber())) {
            righeAnomaleDirette.add(mapReader.getLineNumber());
        }
        if (!codiciMatricolaAnomaliaDiretta.contains(codiceMatricola)){
            codiciMatricolaAnomaliaDiretta.add(codiceMatricola);
        }
        return true;
    }
    return false;
    }

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

    public static List<String> codiciMatricoleConAppFunzionaliENonDirette(Map<String, Map<Integer, List<Map<String, Object>>>> appartenentiFunzionali, Map<String, Map<Integer, List<Map<String, Object>>>> appartenentiDiretti) {
        List<String> codiciMatricoleConAppFunzionaliENonDirette = new ArrayList<>();
        for (String codiceMatricola : appartenentiFunzionali.keySet()) {
            
            if (!appartenentiDiretti.containsKey(codiceMatricola)) {
                if (!codiciMatricoleConAppFunzionaliENonDirette.contains(codiceMatricola)){
                    codiciMatricoleConAppFunzionaliENonDirette.add(codiceMatricola);
                }
            }
            //devo valutare anche l'arco temporale
            if (appartenentiDiretti.containsKey(codiceMatricola)){
                Map<Integer, List<Map<String, Object>>> struttureConAfferenzaDirettaDiX = appartenentiDiretti.get(codiceMatricola);
//                ciclo tutte le afferenze funzionali
//                per ogni afferenza controllo che sia nell'arco temporale di una diretta
                Map<Integer, List<Map<String, Object>>> struttureConAfferenzaFunzionaleDiX = appartenentiFunzionali.get(codiceMatricola);
                List<Map<String,Object>> resultConPeriodiUniti = new ArrayList();
                for (Integer idCasellaDiretta : struttureConAfferenzaDirettaDiX.keySet()){
                    resultConPeriodiUniti.addAll(struttureConAfferenzaDirettaDiX.get(idCasellaDiretta));
                }
                resultConPeriodiUniti = ImportaDaCSVUtils.mergeTimePeriods(resultConPeriodiUniti);
                for (Integer idCasella : struttureConAfferenzaFunzionaleDiX.keySet()) {
                    Boolean periodoFunzionaleIsOk = false;
                    List<Map<String, Object>> periodiTemporaliAfferenzaFunzionale = struttureConAfferenzaFunzionaleDiX.get(idCasella);
                    
                    for (Map<String, Object> periodoTemporale : periodiTemporaliAfferenzaFunzionale){
                        if (ImportaDaCSVUtils.isPeriodoContenuto(
                                    resultConPeriodiUniti,
                                    ImportaDaCSVUtils.formattattore(periodoTemporale.get("datain")),
                                    ImportaDaCSVUtils.formattattore(periodoTemporale.get("datafi")))){
                                periodoFunzionaleIsOk = true;
                        }
                    }
                    if (!periodoFunzionaleIsOk && !codiciMatricoleConAppFunzionaliENonDirette.contains(codiceMatricola)){
                        codiciMatricoleConAppFunzionaliENonDirette.add(codiceMatricola);
                    } 
                    
                    
                }
            }
        }
        return codiciMatricoleConAppFunzionaliENonDirette;
    }

    /**
     * 
     * @param appartenentiMap
     * @param mapError
     * @param idCasella
     * @param datain
     * @param datafi
     * @param appartenentiDirettiPerControlloSovrapposizioneConDiretta
     * @param appartenentiDirettiPerControlloSovrapposizioneConFunzionale
     * @param appartenentiFunzionaliPerControlloSovrapposizioneConFunzionale
     * @param mapReader
     * @param righeAnomaleFunzionali
     * @param righeAnomaleDirette
     * @return true se 
     *  - appartenenza di tipo T multiple (sono una per utente per periodo temporale valida)
     *  - appartenenza di tipo F su stessa casella casella con tipo T periodo sovrapposto
     *  - piu afferenze di tipo F su stessa casella con periodo sovrapposto
     *  - tipo appartenenza assente o non riconosciuta
     * altrimenti ritorno false
     */
    public static boolean checkErroreInTipoAppatenenza(
            Map<String, Object> appartenentiMap,
            Map<String, Object> mapError,
            String idCasella,
            ZonedDateTime datain,
            ZonedDateTime datafi,
            Map<String, List<Map<String,Object>>> appartenentiDirettiPerControlloSovrapposizioneConDiretta,
            Map<String, Map<Integer, List<Map<String, Object>>>> appartenentiDirettiPerControlloSovrapposizioneConFunzionale,
            Map<String, Map<Integer, List<Map<String, Object>>>> appartenentiFunzionaliPerControlloSovrapposizioneConFunzionale,
            ICsvMapReader mapReader,
            List<Integer> righeAnomaleFunzionali,
            List<Integer> righeAnomaleDirette,
            List<String> codiciMatricolaAnomaliaDiretta) {

        Boolean anomalia = false;
        Integer idCasellaInt = Integer.valueOf(idCasella);
        String codiceMatricola = appartenentiMap.get("codice_matricola").toString();
        Boolean checkDiretto = false;
        if (
            appartenentiMap.get("tipo_appartenenza") == null || 
            appartenentiMap.get("tipo_appartenenza").toString().trim().equals("") || 
            appartenentiMap.get("tipo_appartenenza") == "" || idCasella.equals("")
            ) {
                mapError.put("ERRORE", mapError.get("ERRORE") + " tipo appartenenza assente,");
                mapError.put("tipo_appartenenza", "");
                mapError.put("Anomalia", "true");
                return true;
                
        } else if (appartenentiMap.get("tipo_appartenenza").toString().trim().equalsIgnoreCase("T")) {
            
            mapError.put("tipo_appartenenza", appartenentiMap.get("tipo_appartenenza"));
            Map<Integer, List<Map<String, Object>>> appartenenteDirettoConCasella = 
                appartenentiDirettiPerControlloSovrapposizioneConFunzionale.get(codiceMatricola);
            List<Map<String, Object>> listaPeriodiAfferenzaDiretta = appartenentiDirettiPerControlloSovrapposizioneConDiretta.get(codiceMatricola);
            
            if (appartenenteDirettoConCasella == null && listaPeriodiAfferenzaDiretta == null) {
                //non ho quella matricola nelle mappe quindi creo tutti i contenuti e li assegno
                
                //aggiungo il periodo alla mappa appartenentiDirettiPerControlloSovrapposizioneConFunzionale
                appartenenteDirettoConCasella = new HashMap();
                List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                Map<String, Object> periodoDaCasellare = new HashMap();
                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                periodoCasellato.add(periodoDaCasellare);
                appartenenteDirettoConCasella.put(idCasellaInt, periodoCasellato);
                appartenentiDirettiPerControlloSovrapposizioneConFunzionale.put(codiceMatricola, appartenenteDirettoConCasella);
                
                //aggiungo il periodo alla mappa appartenentiDirettiPerControlloSovrapposizioneConDiretta
                listaPeriodiAfferenzaDiretta = new ArrayList<>();
                Map<String, Object> periodoPerControlloDiretteMap = new HashMap();
                periodoPerControlloDiretteMap.put("datain", appartenentiMap.get("datain"));
                periodoPerControlloDiretteMap.put("datafi", appartenentiMap.get("datafi"));
                listaPeriodiAfferenzaDiretta.add(periodoPerControlloDiretteMap);
                appartenentiDirettiPerControlloSovrapposizioneConDiretta.put(codiceMatricola, listaPeriodiAfferenzaDiretta);
                
            } else if (appartenenteDirettoConCasella == null && listaPeriodiAfferenzaDiretta != null) {
                 //aggiungo il periodo alla mappa appartenentiDirettiPerControlloSovrapposizioneConFunzionale
                appartenenteDirettoConCasella = new HashMap();
                List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                Map<String, Object> periodoDaCasellare = new HashMap();
                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                periodoCasellato.add(periodoDaCasellare);
                appartenenteDirettoConCasella.put(idCasellaInt, periodoCasellato);
                appartenentiDirettiPerControlloSovrapposizioneConFunzionale.put(codiceMatricola, appartenenteDirettoConCasella);
                
                //controllo che la nuova afferenza non sia coincidente con quelle preesistenti
                anomalia = checkDiretto(listaPeriodiAfferenzaDiretta, datain, datafi, mapError, mapReader, righeAnomaleDirette,codiciMatricolaAnomaliaDiretta, codiceMatricola);
                
                //aggiungo il periodo alla mappa appartenentiDirettiPerControlloSovrapposizioneConDiretta
                Map<String, Object> periodoPerControlloDiretteMap = new HashMap();
                periodoPerControlloDiretteMap.put("datain", appartenentiMap.get("datain"));
                periodoPerControlloDiretteMap.put("datafi", appartenentiMap.get("datafi"));
                listaPeriodiAfferenzaDiretta.add(periodoPerControlloDiretteMap);
                appartenentiDirettiPerControlloSovrapposizioneConDiretta.put(codiceMatricola, listaPeriodiAfferenzaDiretta);
                
            } else {
                //ho trovato la matricola in tutte e due le mappe quindi esiste almeno un periodo e una casella gia presente nella mappa
                //appartenentiDirettiPerControlloSovrapposizioneConFunzionale 
                //un periodo è presente nella mappa appartenentiDirettiPerControlloSovrapposizioneConDiretta
                
                //controllo che la nuova afferenza non sia coincidente con quelle preesistenti
                anomalia = checkDiretto(listaPeriodiAfferenzaDiretta, datain, datafi, mapError, mapReader, righeAnomaleDirette,codiciMatricolaAnomaliaDiretta,codiceMatricola);
                
                //aggiungo il periodo alla mappa appartenentiDirettiPerControlloSovrapposizioneConDiretta
                Map<String, Object> periodoPerControlloDiretteMap = new HashMap();
                periodoPerControlloDiretteMap.put("datain", appartenentiMap.get("datain"));
                periodoPerControlloDiretteMap.put("datafi", appartenentiMap.get("datafi"));
                listaPeriodiAfferenzaDiretta.add(periodoPerControlloDiretteMap);
                appartenentiDirettiPerControlloSovrapposizioneConDiretta.put(codiceMatricola, listaPeriodiAfferenzaDiretta);
                
                //aggiungo il periodo alla mappa appartenentiDirettiPerControlloSovrapposizioneConFunzionale
                //due casi 1)la casella non c'è 2)la casella c'è
                List<Map<String, Object>> periodoCasellatoPerMapAppartenentiDirettiPerControlloSovrapposizioneConFunzionale = 
                        appartenenteDirettoConCasella.get(idCasellaInt);
                
                if (periodoCasellatoPerMapAppartenentiDirettiPerControlloSovrapposizioneConFunzionale == null) {
                    //caso 1 mi manca ancora la casella e devo aggiungere il nuovo periodo temporale
                    periodoCasellatoPerMapAppartenentiDirettiPerControlloSovrapposizioneConFunzionale = new ArrayList();
                    Map<String, Object> periodoDaCasellare = new HashMap();
                    periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                    periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                    periodoDaCasellare.put("riga", mapReader.getLineNumber());
                    periodoCasellatoPerMapAppartenentiDirettiPerControlloSovrapposizioneConFunzionale.add(periodoDaCasellare);
                    
                } else {
                    //caso 2 aggiungo solo il periodo alla lista
                    Map<String, Object> periodoDaCasellare = new HashMap();
                    periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                    periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                    periodoDaCasellare.put("riga", mapReader.getLineNumber());
                    periodoCasellatoPerMapAppartenentiDirettiPerControlloSovrapposizioneConFunzionale.add(periodoDaCasellare);
                }
                appartenenteDirettoConCasella.put(idCasellaInt, periodoCasellatoPerMapAppartenentiDirettiPerControlloSovrapposizioneConFunzionale);
            }
            
        } else if (appartenentiMap.get("tipo_appartenenza").toString().trim().equalsIgnoreCase("F")){
            mapError.put("tipo_appartenenza", appartenentiMap.get("tipo_appartenenza"));
            Map<Integer, List<Map<String, Object>>> appFunzionale = appartenentiFunzionaliPerControlloSovrapposizioneConFunzionale.get(appartenentiMap.get("codice_matricola").toString());
            
            if (appFunzionale == null) {
                //non ho quella matricola nella mapppa
                //creo tutto
                appFunzionale = new HashMap();
                List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                appFunzionale.put(idCasellaInt, periodoCasellato);
                Map<String, Object> periodoDaCasellare = new HashMap();
                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                periodoCasellato.add(periodoDaCasellare);
                appartenentiFunzionaliPerControlloSovrapposizioneConFunzionale.put(appartenentiMap.get("codice_matricola").toString(), appFunzionale);
                
            } else {
                //ho quella matricola 
                //adesso posso avere la casella oppure no
                List<Map<String, Object>> periodoCasellato = appFunzionale.get(Integer.valueOf(appartenentiMap.get("id_casella").toString()));
                if (periodoCasellato == null) {
                    //caso in cui non ho la casella
                    //creo la lista e la metto nella mappa
                    periodoCasellato = new ArrayList<>();
                    Map<String, Object> periodoDaCasellare = new HashMap();
                    periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                    periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                    periodoDaCasellare.put("riga", mapReader.getLineNumber());
                    periodoCasellato.add(periodoDaCasellare);
                    appFunzionale.put(Integer.valueOf(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                } else {
                    //caso in cui ho la casella
                    //controllo di non avere un'altra afferenza funzionale che si sovrappone 
                    if (ImportaDaCSVUtils.isPeriodiSovrapposti(periodoCasellato, datain, datafi)) {
                        mapError.put("Anomalia", "true");
                        mapError.put("ERRORE", mapError.get("ERRORE") + " doppia afferenza funzionale su stessa casella in periodo sovrapposto,");
                        if (!righeAnomaleFunzionali.contains(mapReader.getLineNumber())) {
                            righeAnomaleFunzionali.add(mapReader.getLineNumber());
                        }
                        anomalia = true;
                    }
                    //vado ad aggiungere il periodo alla lista
                    Map<String, Object> periodoDaCasellare = new HashMap();
                    periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                    periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                    periodoDaCasellare.put("riga", mapReader.getLineNumber());
                    periodoCasellato.add(periodoDaCasellare);
                }
            }

        } else {
            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo appartenenza non riconosciuto deve essere T per la diretta o F per la funzionale,");
            mapError.put("tipo_appartenenza", "");
            mapError.put("Anomalia", "true");
            return true;
        }
    
        //adesso controllo che le mie afferenze funzionali non si sovrappongano alle dirette
        
        if (appartenentiMap.get("tipo_appartenenza").toString().trim().equalsIgnoreCase("T")) {
            if (appartenentiMap.get("codice_matricola") != null && !appartenentiMap.get("codice_matricola").toString().trim().equals("")) {
                
                if (appartenentiFunzionaliPerControlloSovrapposizioneConFunzionale.containsKey(codiceMatricola)) {
                    if (appartenentiMap.get("id_casella") != null && !appartenentiMap.get("id_casella").toString().trim().equals("")) {
                        Integer idCasellaApp = Integer.valueOf(appartenentiMap.get("id_casella").toString());
                        if (appartenentiFunzionaliPerControlloSovrapposizioneConFunzionale.get(codiceMatricola).containsKey(idCasellaApp)) {
                            if (ImportaDaCSVUtils.isPeriodiSovrapposti(
                                    appartenentiFunzionaliPerControlloSovrapposizioneConFunzionale.get(codiceMatricola).get(idCasellaApp),
                                    ImportaDaCSVUtils.formattattore(appartenentiMap.get("datain")),
                                    ImportaDaCSVUtils.formattattore(appartenentiMap.get("datafi")))
                                    ) {
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
                if (appartenentiDirettiPerControlloSovrapposizioneConFunzionale.containsKey(codiceMatricola)) {
                    if (appartenentiMap.get("id_casella") != null && !appartenentiMap.get("id_casella").toString().trim().equals("")) {
                        Integer idCasellaApp = Integer.valueOf(appartenentiMap.get("id_casella").toString());
                        if (appartenentiDirettiPerControlloSovrapposizioneConFunzionale.get(codiceMatricola).containsKey(idCasellaApp)) {                           
                            if (ImportaDaCSVUtils.isPeriodiSovrapposti(
                                    appartenentiDirettiPerControlloSovrapposizioneConFunzionale.get(codiceMatricola).get(idCasellaApp),
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
