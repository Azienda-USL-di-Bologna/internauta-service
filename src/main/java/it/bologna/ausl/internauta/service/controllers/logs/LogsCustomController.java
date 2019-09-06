package it.bologna.ausl.internauta.service.controllers.logs;

import it.bologna.ausl.internauta.service.krint.KrintLogDescription;
import it.bologna.ausl.internauta.service.repositories.logs.KrintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${logs.mapping.url.root}")
public class LogsCustomController {
    
    @Autowired
    private KrintRepository krintRepository;
    
    @RequestMapping(value = "prova", method = RequestMethod.GET)
    public void prova() {
        // KrintLogDescription logs = krintRepository.getLogs(null, null, null, 294711, null, null, null, null);
        // krintRepository.getLogs(new String[]{"blah", "hey", "yo"}, "vds", "vds", 294711, "vds", "vds", new java.util.Date(), new java.util.Date());
        // System.out.println("res" + logs.toString());
    }
}
