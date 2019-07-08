/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.controllers.baborg;

import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${baborg.mapping.url.root}")
public class BaborgCustomController {

    private static final Logger log = LoggerFactory.getLogger(RestController.class);

    @Autowired
    StrutturaRepository strutturaRepository;

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

}
