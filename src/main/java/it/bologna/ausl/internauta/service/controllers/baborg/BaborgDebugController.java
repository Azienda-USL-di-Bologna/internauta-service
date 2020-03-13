package it.bologna.ausl.internauta.service.controllers.baborg;

import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecUtente;
import it.bologna.ausl.model.entities.baborg.Permesso;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecUtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithPlainFields;
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
    public Object test() {
        
//        Utente u = strutturaRepository.getResponsabile(28046);
        Integer u = strutturaRepository.getResponsabile(28046);
//        UtenteWithPlainFields u2 = factory.createProjection(UtenteWithPlainFields.class, u);

        System.out.println("sonno  qui:");        
        
        Utente one = utenteRepository.findById(u).get();
        UtenteWithIdAzienda u2 = factory.createProjection(UtenteWithIdAzienda.class, one);
        System.out.println("sonno  qui 2:");
        
//        System.out.println(one.getIdPersona());
//        System.out.println(u.getEmail());
//        System.out.println(u.getOmonimia());
        
        return u2;
       
    }
}
