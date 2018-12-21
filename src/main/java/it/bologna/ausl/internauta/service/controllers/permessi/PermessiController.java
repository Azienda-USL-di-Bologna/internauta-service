package it.bologna.ausl.internauta.service.controllers.permessi;

import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.types.PermessoEntitaStoredProcedure;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Gus
 */
@RestController
@RequestMapping(value = "${permessi.mapping.url.root}")
public class PermessiController {
    
    @Autowired
    PermissionRepositoryAccess permissionRepositoryAccess;
    
//    final String token = ";";
    
    @RequestMapping(value = "managePermissions", method = RequestMethod.POST)
    public void updatePermesso(@RequestBody List<PermessoEntitaStoredProcedure> permessiEntita, HttpServletRequest request) throws BlackBoxPermissionException {
        permissionRepositoryAccess.managePermissions(permessiEntita);
//        Set<String> coppieAmbitoTipo = new HashSet();
////        List<Integer> idPermessi = new ArrayList();
//        permessiEntita.forEach((permessoEntita) -> {
//            // Ciclo i permessi di questa coppia Soggetto - Oggetto,
//            // lo scopo è salavarmi le permutazioni ambito - tipo per poi cancellare i permessi prima di reinserirli.
//            permessoEntita.getPermessi().forEach((permesso) -> {
//                coppieAmbitoTipo.add(permesso.getAmbito() + token + permesso.getTipo());
////                idPermessi.add(permesso.getId());
//            });
//            
//            // Ora per soggetto e oggetto passati posso chiamare la delete per ogni combianazione ambito - tipo
//            coppieAmbitoTipo.forEach((coppia) -> {
//                try {
//                    String[] coppiaz = coppia.split(token);
//                    String ambito = coppiaz[0];
//                    String tipo = coppiaz[1];
//                    permissionRepositoryAccess.deletePermission(permessoEntita.getSoggetto(), permessoEntita.getOggetto(), null, null, null, null, null, ambito, tipo, null);
//                    // Se torna una lista di permessi bloccati: id soggetto bloccato, id predicato. aggiungo ad una lista.
//                    // Poi quel soggetto-oggetto-ambito-tipo-predicato lo blocco per quel soggetto facendo altre insert su questa lista. 
//                } catch (BlackBoxPermissionException ex) {
//                    Logger.getLogger(PermessiController.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            });
//            
//            permessoEntita.getPermessi().forEach((permesso) -> {
//                try {
//                    permissionRepositoryAccess.insertSimplePermission(permessoEntita.getSoggetto(), permessoEntita.getOggetto(), permesso.getPredicato(), permesso.getOriginePermesso(), null, permesso.getPropagaSoggetto(), permesso.getPropagaOggetto(), permesso.getAmbito(), permesso.getTipo(), permesso.getIdPermessoBloccato());
//                } catch (BlackBoxPermissionException ex) {
//                    Logger.getLogger(PermessiController.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            });
//        });
    }
}
