/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.iac;

import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.iac.datibollovirtuale.GetDatiBolloVirtualeController;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Salo
 */
@RestController
@RequestMapping(value = "${internauta_argo_communicator_client.mapping.url.root}")
public class InternautaArgoCommunicatorClient {
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    GetDatiBolloVirtualeController getDatiBolloVirtualeController;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InternautaArgoCommunicatorClient.class);
    
    @RequestMapping(value = {"get_dati_bollo_virtuale"},
            method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDatiBolloVirutale() throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente loggedUser = (Utente) authentication.getPrincipal();
        Persona personaLogged = personaRepository.getOne(loggedUser.getIdPersona().getId());
        List<String> codiciAziende = userInfoService.getAziendeWherePersonaIsCa(personaLogged).stream().map(azienda -> azienda.getCodice()).collect(Collectors.toList());
        JSONArray datiBolloVirtualeAziende = getDatiBolloVirtualeController.getDatiBolloVirtualeAziende(codiciAziende);
        System.out.println(datiBolloVirtualeAziende.toString(4));
        return ResponseEntity.ok(datiBolloVirtualeAziende.toString(4));
    }
}
