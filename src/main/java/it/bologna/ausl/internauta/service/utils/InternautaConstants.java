package it.bologna.ausl.internauta.service.utils;

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
            REDIGE, FIRMA, AGFIRMA, DG, DS, DSC, DA, RISERVA
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
}
