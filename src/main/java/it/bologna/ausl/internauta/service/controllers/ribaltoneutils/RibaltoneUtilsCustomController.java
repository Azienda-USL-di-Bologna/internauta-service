/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.service.controllers.ribaltoneutils;

import it.bologna.ausl.internauta.service.repositories.ribaltoneutils.RibaltoneDaLanciareRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Top
 */
@RestController
@RequestMapping(value = "${ribaltoneutils.mapping.url.root}")
public class RibaltoneUtilsCustomController {
    
    @Autowired
    private RibaltoneDaLanciareRepository ribaltoneDaLanciareRepository;
    
    @Autowired
    private CachedEntities cachedEntities;

    @RequestMapping(value = "lanciaTrasformatore", method = RequestMethod.POST)
    public ResponseEntity<?> lanciaTrasformatore(
            @RequestBody RibaltoneDaLanciare ribaltoneDaLanciare,
            HttpServletRequest request) {
//        Azienda azienda = cachedEntities.getAziendaFromCodice(ribaltoneDaLanciare.getCodiceAzienda());
//        Persona persona = cachedEntities.getPersonaFromCodiceFiscale("RIBALTONE");
//        Utente user = persona.getUtenteList().stream()
//                    .filter(utente -> utente.getIdAzienda().getId().equals(azienda.getId())).findFirst().get();
        if (StringUtils.hasText(ribaltoneDaLanciare.getNote())){ribaltoneDaLanciare.setNote("nessuna nota");}
        ribaltoneDaLanciareRepository.sendNotifyInternauta(ribaltoneDaLanciare.getCodiceAzienda(), 
                ribaltoneDaLanciare.getRibaltaArgo(),
                ribaltoneDaLanciare.getRibaltaInternauta(),
                ribaltoneDaLanciare.getNote(),
                ribaltoneDaLanciare.getEmail(),
                ribaltoneDaLanciare.getIdUtente().getId());
        return new ResponseEntity("", HttpStatus.OK);
    }
    
}
