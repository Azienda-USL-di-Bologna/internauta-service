package it.bologna.ausl.internauta.service.controllers.baborg;

import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecUtente;
import it.bologna.ausl.model.entities.baborg.Permesso;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecUtenteRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
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
    PecRepository pecRepository;
    @Autowired
    PecUtenteRepository pecUtenteRepository;

    @RequestMapping(value = "ping", method = RequestMethod.GET)
    public String ping() {
        return "pong";
    }

    @RequestMapping(value = "test", method = RequestMethod.PATCH)
    public void test(@RequestBody Map<String, Object> data) {
        Pec pec = pecRepository.getOne(753);

        PecUtente pecUtente = new PecUtente();
        pecUtente.setIdPec(pec);

//        Permesso permesso = new Permesso();
//        permesso.setProvenienza("GDMGDM");
//
//        Set<Permesso> permessoSet = new HashSet();
//        permessoSet.add(permesso);
//        pecUtente.setPermessoSet(permessoSet);
//        Set<PecUtente> pecUtenteSet = new HashSet();
//        pecUtenteSet.add(pecUtente);
        pec.getPecUtenteList().add(pecUtente);

//        pec.setPecUtenteSet(pecUtenteSet);
        //pecUtente.
        pecRepository.save(pec);
    }
}
