package it.bologna.ausl.baborg.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "test")
public class Test {

    @RequestMapping(value = "versione", method = RequestMethod.GET)
    public String getVersion() {
        return "ciao";
    }

}
