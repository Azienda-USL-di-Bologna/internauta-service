/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.controllers.baborg;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.baborg.utils.BaborgUtils;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ImportazioniOrganigrammaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.ImportazioniOrganigramma;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Array;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "${baborg.mapping.url.root}")
public class BaborgCustomController {

    private static final Logger log = LoggerFactory.getLogger(RestController.class);

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    BaborgUtils baborgUtils;

    @Autowired
    UserInfoService infoService;

    @Autowired
    AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    ImportazioniOrganigrammaRepository importazioniOrganigrammaRepository;

    @RequestMapping(value = "struttureAntenate/{idStruttura}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> struttureAntenate(
            @PathVariable(required = true) Integer idStruttura) {

        String struttureAntenate = strutturaRepository.getStruttureAntenate(idStruttura);
        System.out.println("struttureAntenate: " + struttureAntenate);

        // trasformiamo la stringa restituita in un array
        String[] struttureAntenateArray = struttureAntenate.split(",");

        List<Integer> struttureAntenateList = Arrays.stream(struttureAntenateArray)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        // invertiamo l'ordine degli elementi nell'array in modo che
        // l'id della struttura radice sia in prima posizione, quello della struttura passata in ultima
        Collections.reverse(struttureAntenateList);

        return new ResponseEntity(struttureAntenateList, HttpStatus.OK);
    }

    @RequestMapping(value = "insertImpOrgRowAndCsvUpload", method = RequestMethod.POST)
    public ResponseEntity<String> insertImpOrgRowAndCsvUpload(
            HttpServletRequest request,
            @RequestParam("codiceAzienda") String codiceAzienda,
            @RequestParam("idAzienda") String idAzienda,
            @RequestParam("tipo") String tipo,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName) throws BlackBoxPermissionException, IOException, Http400ResponseException, Exception, Exception {

        // Carico utente e persona
        ImportazioniOrganigramma newRowInserted = null;
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona person = null;
        String completeDataFile = null;

        if (authenticatedUserProperties.getRealPerson() != null) {
            person = authenticatedUserProperties.getRealPerson();
        } else {
            person = authenticatedUserProperties.getPerson();
        }

        Utente user = null;
        if (authenticatedUserProperties.getRealUser() != null) {
            user = authenticatedUserProperties.getRealUser();
        } else {
            user = authenticatedUserProperties.getUser();
        }

        ImportazioniOrganigramma res = null;

        // Se sono CA e i parametri passati sono corretti inizio l'importazione
        if (infoService.isCA(user) && !file.isEmpty() && idAzienda != null && tipo != null) {
            res = baborgUtils.manageUploadFile(user.getId(), file, idAzienda, tipo, codiceAzienda, fileName, person);
        } else {
            throw new Http400ResponseException("1", "I dati passati per l'importazione sono assenti o non corretti");
        }

        return new ResponseEntity(res, HttpStatus.OK);
    }

}
