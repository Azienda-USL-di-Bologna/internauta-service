package it.bologna.ausl.internauta.service.controllers.tip;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.service.InternautaApplication;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 *
 * @author Top
 */
public class TipUtils {
    private static final Logger log = LoggerFactory.getLogger(TipUtils.class);

    /**
     * Funzione che prende in input le colonne del csv e ritorna un array
     * ordinato di elementi che rappresentano la riga del csv
     *
     * @param colonne
     * @param importazioneOggetto
     * @param objectMapper
     * @return array
     */
    public static Object[] buildCsvRowFromImportazioneOggetto(
            ColonneImportazioneOggetto[] colonne,
            ImportazioneOggetto importazioneOggetto,
            ObjectMapper objectMapper) {
        BeanWrapper wrapper = new BeanWrapperImpl(importazioneOggetto);
        Object[] res = new String[colonne.length];
        int i = 0;
        for (ColonneImportazioneOggetto colonna : colonne) {
            Object valore = wrapper.getPropertyValue(colonna.toString());
            if ((colonna.toString().equals("errore") || colonna.toString().equals("errori")) && valore != null) {
                //uso l'objectMapper per crearmi una classe errore e poter comodamente scrivere la colonna errori
                Map<String, Map<String, Map<String, String>>> colonnaErroriMap = objectMapper.convertValue(valore, new TypeReference<Map<String, Map<String, Map<String, String>>>>() {
                });

                StringBuilder importazioneErrore = new StringBuilder();
                String importazioneErroreInt = "Errori di Importazione: ";
                
                StringBuilder importazioneWarning = new StringBuilder();
                String importazioneWarningInt = "Warning di Importazione: ";
                
                StringBuilder validazioneErrore = new StringBuilder();
                String validazioneErroreInt ="Errori di Validazione: ";
                
                StringBuilder validazioneWarning = new StringBuilder();
                String validazioneWarningInt = "Warning di Validazione: ";
                
                
                colonnaErroriMap.forEach((colonnaInErrore, value) -> {
                    Map<String, Map<String, String>> validazioneImportazioneMap = colonnaErroriMap.get(colonnaInErrore);
                    if (validazioneImportazioneMap != null) {

                        Map<String, String> validazioneInfoErrorWarningMap = validazioneImportazioneMap.get("validazione");
                        Map<String, String> importazioneInfoErrorWarningMap = validazioneImportazioneMap.get("importazione");

                        if (importazioneInfoErrorWarningMap != null) {
                            String importazioneError = importazioneInfoErrorWarningMap.get("error");
                            if (importazioneError != null) {
                                importazioneErrore.append(colonnaInErrore).append(": ").append(importazioneError);
                            }
                            
                            String importazioneWarn = importazioneInfoErrorWarningMap.get("warning");
                            if (importazioneWarn != null){
                                importazioneWarning.append(colonnaInErrore).append(": ").append(importazioneWarn);
                            }
                        }
                        if (validazioneInfoErrorWarningMap != null) {
                            String validazioneError = validazioneInfoErrorWarningMap.get("error");
                            if (validazioneError != null) {
                                validazioneErrore.append(colonnaInErrore).append(": ").append(validazioneError);
                            }
                            
                            String validazioneWarn = validazioneInfoErrorWarningMap.get("warning");
                            if (validazioneWarn != null){
                                validazioneWarning.append(colonnaInErrore).append(": ").append(validazioneWarn);
                            }
                        } 
                    }
                });
                
                log.info(importazioneErrore.toString());
                log.info(importazioneWarning.toString());
                log.info(validazioneErrore.toString());
                log.info(validazioneWarning.toString());
                String errore = "";
                
                if (StringUtils.hasText(importazioneErrore.toString())){
                    errore += importazioneErroreInt + importazioneErrore.toString();
                }
                if (StringUtils.hasText(importazioneWarning.toString())){
                    if (StringUtils.hasText(errore)){
                        errore +="\n";
                    }
                    errore += importazioneWarningInt + importazioneWarning.toString();
                }
                if (StringUtils.hasText(validazioneErrore.toString())){
                    if (StringUtils.hasText(errore)){
                        errore +="\n";
                    }
                    errore += validazioneErroreInt + validazioneErrore.toString();
                }
                if (StringUtils.hasText(validazioneWarning.toString())){
                    if (StringUtils.hasText(errore)){
                        errore +="\n";
                    }
                    errore += validazioneWarningInt + validazioneWarning.toString();
                }
                
                res[i++] = errore;

            } else {
                res[i++] = valore;
            }
        }
        return res;
    }
}
