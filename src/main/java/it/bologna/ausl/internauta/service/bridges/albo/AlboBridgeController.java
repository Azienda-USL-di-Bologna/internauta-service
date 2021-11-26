package it.bologna.ausl.internauta.service.bridges.albo;

import it.bologna.ausl.internauta.service.argo.utils.gd.SottoDocumentiUtils;
import it.bologna.ausl.internauta.service.bridges.albo.exceptions.AlboBridgeException;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.MasterChefUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${bridges.mapping.url.albo}")
public class AlboBridgeController implements ControllerHandledExceptions {
    
    private boolean gediInternauta = false;
    
    @Autowired
    private MasterChefUtils masterChefUtils;
    
    @Autowired
    private SottoDocumentiUtils sottoDocumentiUtils;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    public static enum TipiAllegato {
        STAMPA_UNICA, RELATA
    }
    
    @RequestMapping(value = {"getAllegato"}, method = RequestMethod.GET)
    public void getAllegato(
            @RequestParam(required = true) String azienda,
            @RequestParam(required = true) String codice,
            @RequestParam(required = true) TipiAllegato tipo,
            HttpServletRequest request,
            HttpServletResponse response) throws AlboBridgeException {
        
        //TODO: controllo parametri
        
        Azienda aziendaObj = cachedEntities.getAziendaFromCodice(azienda);
        String repositoryFileId;
        if (gediInternauta) {
            //TODO: da fare
        } else {
            List<Map<String, Object>> sottoDocumenti = null;
            try {
                sottoDocumenti = sottoDocumentiUtils.getSottoDocumentoByCodice(aziendaObj.getId(), codice);
            } catch (Exception ex) {
                throw new AlboBridgeException(String.format("errore nel reperimento del sottodocumento gedi per l'azienda %s, con il codice %s di tipo %s", azienda, codice, tipo));
            }
            if (sottoDocumenti == null || sottoDocumenti.isEmpty()) {
                throw new AlboBridgeException(String.format("sottodocumento non trovato in gedi per l'azienda %s, con il codice %s di tipo %s", azienda, codice, tipo));
            } else if (sottoDocumenti.size() > 1) {
                throw new AlboBridgeException(String.format("trovato più di un sottodocumento in gedi per l'azienda %s, con il codice %s di tipo %s", azienda, codice, tipo));
            }
        }
        
    }
    
    
}
