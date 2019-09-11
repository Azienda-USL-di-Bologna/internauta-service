package it.bologna.ausl.internauta.service.interceptors.shpeck;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.Draft;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.ArrayList;
import java.util.Collections;
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
@NextSdrInterceptor(name = "draft-interceptor")
public class DraftInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DraftInterceptor.class);

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    private KrintShpeckService krintShpeckService;

    @Override
    public Class getTargetEntityClass() {
        return Draft.class;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Draft draft = (Draft) entity;

        Pec pec = draft.getIdPec();
        if (pec.getAttiva()) {
            try {
                lanciaEccezioneSeNonHaPermessoDiNuovaMail(draft);
            } catch (BlackBoxPermissionException | Http403ResponseException ex) {
                throw new AbortSaveInterceptorException();
            }
        } else {
            throw new AbortSaveInterceptorException();
        }

        return entity;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Draft draft = (Draft) entity;

        if (KrintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeDraft(draft, OperazioneKrint.CodiceOperazione.PEC_DRAFT_CREAZIONE);
        }

        return entity;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Draft draft = (Draft) entity;
        try {
            lanciaEccezioneSeNonHaPermessoDiNuovaMail(draft);
        } catch (BlackBoxPermissionException | Http403ResponseException ex) {
            throw new AbortSaveInterceptorException();
        }
        if (KrintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeDraft(draft, OperazioneKrint.CodiceOperazione.PEC_DRAFT_CANCELLAZIONE);
        }
        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
    }

    private void lanciaEccezioneSeNonHaPermessoDiNuovaMail(Draft draft) throws AbortSaveInterceptorException, BlackBoxPermissionException, Http403ResponseException {
        // Prendo l'utente loggato
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());

        // Prendo i permessi pec
        Map<Integer, List<String>> permessiPec = null;
        permessiPec = userInfoService.getPermessiPec(persona);

        // Controllo che ci sia almeno il RISPONDE sulla pec interessata
        List<String> permessiTrovati = permessiPec.get(draft.getIdPec().getId());
        List<String> permessiSufficienti = new ArrayList();
        permessiSufficienti.add(InternautaConstants.Permessi.Predicati.ELIMINA.toString());
        permessiSufficienti.add(InternautaConstants.Permessi.Predicati.RISPONDE.toString());
        if (Collections.disjoint(permessiTrovati, permessiSufficienti)) {
            throw new Http403ResponseException("1", "Non hai il permesso di creare mail");
        }
    }

}
