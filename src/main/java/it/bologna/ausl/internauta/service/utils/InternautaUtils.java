package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.model.entities.baborg.Azienda;
import org.springframework.stereotype.Component;

/**
 *
 * @author andrea
 */
public class InternautaUtils {
    /**
     * Ottiene URL dell'azienda passata come parametro.
     * Questo perchè ci possono essere più url che si riferiscono alla stessa azienda, ma per il nostro scopo basta sapere il primo.
     * @param azienda
     * @return il primo URL dell'azienda corrispondente
     */
    public static String getURLByIdAzienda(Azienda azienda) {
        String res = null;

        String[] paths = azienda.getPath();
        if (paths != null && paths.length > 0) {
            res = paths[0];
        }
        return res;
    }
}
