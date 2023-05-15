package it.bologna.ausl.internauta.service.controllers.tip;

import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${tip.mapping.url.root}")
public class TipCustomController implements ControllerHandledExceptions {
    
}
