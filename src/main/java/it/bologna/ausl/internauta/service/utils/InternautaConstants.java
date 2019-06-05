package it.bologna.ausl.internauta.service.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 * 
 * Contiene tutte le costanti e gli enum dellìapplicazione suddivise in inner class per ambito
 */
public class InternautaConstants {
    
    /**
     * Costanti per la balckbox dei permessi
     */
    public static class Permessi {
        public enum Predicati {
            REDIGE, FIRMA, AGFIRMA, DG, DS, DSC, DA, RISERVA, ELIMINA, RISPONDE, LEGGE, SPEDISCE, SPEDISCE_PRINCIPALE
        }
        
        public enum Ambiti {
            PICO, DETE, DELI, GEDI, PECG
        }
        
        public enum Tipi{ 
            FLUSSO, PEC, FASCICOLO
        }
    }

    public static class Configurazione {
        public enum ParametriAzienda {
            crossUrlTemplate
        }
    }

    public static class AdditionalData {
        public enum Keys {
            OperationRequested, idPec, idAzienda, idStruttura
        }
        public enum OperationsRequested {
            GetPermessiGestoriPec, 
            GetPermessiDiFlusso, 
            GetPermessiStrutturePec, 
            FilterPecPerPermissionOfSubject, 
            AddPermissionsOnPec, 
            AddGestoriOnPec, 
            FilterPecPerStandardPermissions,
            GetUltimoStatoRibaltone
        }
        public static List<OperationsRequested> getOperationRequested(Keys key, Map<String, String> additionalData) {
            if (additionalData != null) {
                String values = additionalData.get(key.toString());
                if (StringUtils.hasText(values)) {
                    List<String> operationsRequestedStr = Arrays.asList(values.split(":"));  // Divide sui due punti
                    //Arrays.asList(values.split("\\s*,\\s*")); // Divide sulla virgola
                    List<OperationsRequested> operationsRequested = new ArrayList();
                    operationsRequestedStr.stream().forEach((t) -> {
                        operationsRequested.add(OperationsRequested.valueOf(t));
                    });
                    
                    return operationsRequested;
                }
            }
            
            return null;
        }
    }

    public static class HttpSessionData {
        public enum Keys {
            PersoneWithPecPermissions, ParametriAzienda, StruttureWithPecPermissions, PecOfSubject, UtenteLogin, IdSessionLog
        }
    }
}
