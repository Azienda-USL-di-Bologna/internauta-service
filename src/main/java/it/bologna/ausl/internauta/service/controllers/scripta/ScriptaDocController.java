package it.bologna.ausl.internauta.service.controllers.scripta;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
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
@RequestMapping(value = "${scripta.mapping.url.root}")
public class ScriptaDocController {
    
    @Autowired
    private DocRepository docRepository;
    
    @RequestMapping(value = "numeraDoc", method = RequestMethod.GET)
    public Integer userHasRealPermission(
            @RequestParam("idDoc") Integer idDoc,
            @RequestParam("idPersonaRegistrante") Integer idPersonaRegistrante,
            @RequestParam("idStrutturaRegistrante") Integer idStrutturaRegistrante,
            HttpServletResponse response,
            HttpServletRequest request) throws BlackBoxPermissionException {
        return docRepository.numeraDoc(1548, 304295, 1064625);
    }
}
