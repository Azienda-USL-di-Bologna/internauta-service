package it.bologna.ausl.internauta.service.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.utils.firma.remota.controllers.FirmaRemotaRestController;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class CacheableFunctions {
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    @Autowired
    private FirmaRemotaRestController firmaRemotaRestController;
    
    /**
     * Funzione per il reperimento dei providers configurati per l'azienda dell'utente loggato.
     * @return Il set dei providers disponibili.
     * @throws BlackBoxPermissionException errori della blackbox.
     */
    @Cacheable(value = "firmaRemotaProvidersInfo__ribaltorg__")
    public List<Map<String, String>> getFirmaRemotaProvidersInfo() throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(authenticatedUserProperties.getPerson());
        Integer[] aziendeArray = aziendePersona.stream().map(a -> a.getId()).collect(Collectors.toList()).toArray(new Integer[0]);
        
        List<ParametroAziende> parameters = parametriAziende.getParameters("firmaRemotaConfiguration", aziendeArray);
        List<String> hostIds = new ArrayList<>();
        for (ParametroAziende parameter : parameters) {
            hostIds.addAll(parametriAziende.getValue(parameter, new TypeReference<List<String>>(){}));
        }
        return firmaRemotaRestController.getProvidersInfo(hostIds);
    }
}
