package it.bologna.ausl.internauta.service.controllers.baborg;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.controllers.shpeck.ShpeckCustomController;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecUtente;
import it.bologna.ausl.model.entities.baborg.Permesso;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecUtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithPlainFields;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${internauta.mapping.url.debug}")
public class BaborgDebugController {

   
    @Autowired
    StrutturaRepository strutturaRepository; 
    
    @Autowired
    UtenteRepository utenteRepository; 
    
    @Autowired
    ProjectionFactory factory;
            
    @RequestMapping(value = "ping", method = RequestMethod.GET)
    public String ping() {
        return "pong";
    }

    @RequestMapping(value = "test", method = RequestMethod.GET)
    @Transactional(rollbackFor = Throwable.class)
    public Object test() throws EmlHandlerException, UnsupportedEncodingException {
          
        
          
          
//       return EmlHandler.handleEml("C:\\Users\\mdonza\\Desktop\\Eml\\test_mail.eml");
       return EmlHandler.handleEml("C:\\Users\\mdonza\\Desktop\\Eml\\0_originale.eml");
    }
    
    
    
}
