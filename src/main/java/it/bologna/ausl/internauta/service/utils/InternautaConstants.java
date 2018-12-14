package it.bologna.ausl.internauta.service.utils;

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
            REDIGE, FIRMA, AGFIRMA, DG, DS, DSC, DA, RISERVA, ELIMINA, RISPONDE, LEGGE
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
            OperationRequested, Data
        }
        public enum OperationsRequested {
            GetPermessiGestoriPec, GetPermessiDiFlusso
        }
        public static OperationsRequested getOperationRequested(Keys key, Map<String, String> additionalData) {
            if (additionalData != null) {
                String value = additionalData.get(key.toString());
                if (StringUtils.hasText(value)) 
                    return OperationsRequested.valueOf(value);
            }
            return null;
        }
    }
    
    public static class HttpSessionData {
        public enum Keys {
            PersoneWithPecPermissions, ParametriAzienda
        }
    }
}
