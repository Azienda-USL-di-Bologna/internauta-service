package it.bologna.ausl.internauta.service.interceptors.shpeck;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.shpeck.Outbox;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "outbox-interceptor")
public class OutboxInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxInterceptor.class);

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    ShpeckUtils shpeckUtils;

    @Override
    public Class getTargetEntityClass() {
        return Outbox.class;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Outbox outbox = (Outbox) entity;

        Pec pec = outbox.getIdPec();
        if (pec.getAttiva()) {
            try {
                lanciaEccezioneSeNonHaPermessoDiNuovaMail(outbox);
            } catch (BlackBoxPermissionException | Http403ResponseException ex) {
                throw new AbortSaveInterceptorException();
            }
        } else {
            throw new AbortSaveInterceptorException();
        }

        return entity;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Outbox outbox = (Outbox) entity;
        try {
            lanciaEccezioneSeNonHaPermessoDiNuovaMail(outbox);
        } catch (BlackBoxPermissionException | Http403ResponseException ex) {
            throw new AbortSaveInterceptorException();
        }
        
        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
    }

    private void lanciaEccezioneSeNonHaPermessoDiNuovaMail(Outbox outbox) throws AbortSaveInterceptorException, BlackBoxPermissionException, Http403ResponseException {
        // Prendo l'utente loggato
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());

        List<String> permessiSufficienti = new ArrayList();
        permessiSufficienti.add(InternautaConstants.Permessi.Predicati.ELIMINA.toString());
        permessiSufficienti.add(InternautaConstants.Permessi.Predicati.RISPONDE.toString());
        Boolean userHasPermissionOnThisPec = shpeckUtils.userHasPermissionOnThisPec(outbox.getIdPec(), permessiSufficienti, persona);
        if (!userHasPermissionOnThisPec) {
            throw new Http403ResponseException("1", "Non hai il permesso di creare mail");
        }
    }

}
