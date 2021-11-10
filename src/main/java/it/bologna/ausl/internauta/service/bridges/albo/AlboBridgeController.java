package it.bologna.ausl.internauta.service.bridges.albo;

import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${bridges.mapping.url.albo}")
public class AlboBridgeController implements ControllerHandledExceptions {
    
}
