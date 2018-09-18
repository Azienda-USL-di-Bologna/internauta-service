/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.controllers.baborg;

import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.internauta.service.repositories.baborg.RibaltoniDaLanciareRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author fayssel
 */
@RestController
@RequestMapping(value = "${baborg.mapping.url.root}" + "/ribaltone")
public class RibaltoneController {

    @Autowired
    private RibaltoniDaLanciareRepository ribaltoniDaLanciareRepository;

    private final String ribaltoniDaLanciareUniqueIndex = "ribaltoni_da_lanciare_codice_azienda_email_idx";

    public static class RibaltoneParams {

        private String codiceAzienda;
        private String indirizzoMail;

        public RibaltoneParams() {
        }

        public String getCodiceAzienda() {
            return codiceAzienda;
        }

        public void setCodiceAzienda(String codiceAzienda) {
            this.codiceAzienda = codiceAzienda;
        }

        public String getIndirizzoMail() {
            return indirizzoMail;
        }

        public void setIndirizzoMail(String indirizzoMail) {
            this.indirizzoMail = indirizzoMail;
        }
    }

    @RequestMapping(value = "lanciaRibaltone", method = RequestMethod.POST)
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public ResponseEntity ribaltaAzienda(@RequestBody RibaltoneParams params) {
//        System.out.println("==== SERVLET CALLED ====");
//        System.out.println("Indirizzo: " + params.getIndirizzoMail());
//        System.out.println("Azienda: " + params.getCodiceAzienda());
//        return new ResponseEntity("ok", HttpStatus.OK);

        // Controllo di avere i parametri necessari. Se non li ho torno bad request
        if (!StringUtils.hasText(params.getCodiceAzienda()) || !StringUtils.hasText(params.getIndirizzoMail())) {
            return new ResponseEntity("Error", HttpStatus.BAD_REQUEST);
        }

        // Mi prendo l'utente cacheable per sapere se l'utente ha il ruolo di demiurgo.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        List<Ruolo> ruoli = utente.getRuoli();

        if (ruoli.stream().anyMatch(ruolo -> ruolo.getNomeBreve() == Ruolo.CodiciRuolo.CA)) {
            try {
                ribaltoniDaLanciareRepository.inserisciRibaltoneDaLanciare(params.getCodiceAzienda(), params.getIndirizzoMail(), utente.getId());
            } catch (JpaSystemException | DataIntegrityViolationException ex) {
                if (ex.getRootCause() != null && ex.getRootCause().getMessage().contains(ribaltoniDaLanciareUniqueIndex)) {
                    return new ResponseEntity("Attenzione, è già previsto il lancio del ribaltone su questa azienda e per questa mail.\\n Se la mail non dovesse arrivare entro qualche minuto si prega di contattare Babelcare.", HttpStatus.CONFLICT);
                }
                return new ResponseEntity("Attenzione, errore legato al database non previsto. Contattare Babelcare.", HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
                return new ResponseEntity("Attenzione, errore generico non previsto. Contattare Babelcare.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity("Attenzione, non sei abilitato all'utilizzo di questa funzione.", HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity("ok", HttpStatus.OK);
    }
}
