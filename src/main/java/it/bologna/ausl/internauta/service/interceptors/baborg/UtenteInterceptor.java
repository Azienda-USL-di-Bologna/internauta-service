package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData.OperationsRequested.FilterBitPermessoMinimo;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 *
 * @author salo
 */
@Component
@NextSdrInterceptor(name = "utente-interceptor")
@Order(1)
public class UtenteInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtenteInterceptor.class);

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    UserInfoService userInfoService;

    @Override
    public Class getTargetEntityClass() {
        return Utente.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                     case FilterBitPermessoMinimo:
                            String bit = additionalData.get(InternautaConstants.AdditionalData.Keys.BitPermessoMinimo.toString());
                            BooleanExpression filter = QUtente.utente.bitRuoli.goe(Integer.parseInt(bit));
                            initialPredicate = filter.and(initialPredicate);
                    // se la chiamata arriva dal cambia utente
                    case CambioUtente:
                        // per prima cosa prendo l'utente reale (non dovrebbe mai arrivare un cambia utente da un utente impersonato, ma per sicurezza faccio il controllo
                        Persona persona;
                        Utente utente;
                        if (authenticatedSessionData.getRealUser() != null) {
                            persona = authenticatedSessionData.getRealPerson();
                            utente = authenticatedSessionData.getRealUser();
                        } else {
                            persona = authenticatedSessionData.getPerson();
                            utente = authenticatedSessionData.getUser();
                        }
                        // se sono SD, CI, posso impersonare chiunque, per cui non faccio nessun controllo e non modifico la where condition della query
                        // se sono CA posso impersonare solo gli utenti delle aziende in cui sono CA
                        // se non sono SD, CI o CA, posso cambiare utente solo se ho un permesso di avatar
                        if (!userInfoService.isSD(utente) && !userInfoService.isCI(utente) && !userInfoService.isCA(utente)) {
                            // qui entro se non sono nè SD, nè CI, nè CA, per cui controllo se sono avatar
                            try {
                                // controllo se sono AVATAR chiedendo alla black-box.
                                // la blackbox mi torna tuti gli id degli utenti che posso impersonare
                                List<Integer> idUtentiAvatar = userInfoService.getPermessiAvatar(utente);
                                if (idUtentiAvatar != null && idUtentiAvatar.size() > 0) {
                                    // aggiungo alla where condition un id_utente in (utenti che posso impersonare)
                                    BooleanExpression filterUtentiAvatar = QUtente.utente.id.in(idUtentiAvatar);
                                    initialPredicate = filterUtentiAvatar.and(initialPredicate);
                                } else {
                                    // se la blackbox non mi torna nulla allora non posso impersonare nessuno, 
                                    // per cui rendo falsa la where condition in modo che la query non torni nulla
                                    initialPredicate = Expressions.FALSE.eq(true);
                                }
                            } catch (BlackBoxPermissionException ex) {
                                throw new AbortLoadInterceptorException(ex);
                            }
                            break;
                        } else if (!userInfoService.isSD(utente) && !userInfoService.isCI(utente) && userInfoService.isCA(utente)) {
                            // qui entro se non sono SD o CI, ma sono CA di almeno un'azienda
                            
                            /* 
                                leggo la mappa dei ruoli suddivisi per utente, fatta in questo modo:
                             
                                105: {GENERALE: ["UG", "MOS", "OS", "CA"], POOLS: ["UG", "MOS", "OS", "CA"],…}
                                908: {GENERALE: ["UG", "CA"], POOLS: ["UG", "CA"], MATRINT: ["UG", "CA"]}
                                909: {GENERALE: ["UG"], POOLS: ["UG"], MATRINT: ["UG"]}
                                960: {GENERALE: ["UG", "CA"], POOLS: ["UG", "CA"], MATRINT: ["UG", "CA"]}
                                CA: {GENERALE: ["908", "960", "105"], POOLS: ["908", "960", "105"], MATRINT: ["908", "960", "105"]}
                                CI: {GENERALE: ["interaziendali"]}
                                MOS: {GENERALE: ["105"], POOLS: ["105"], MATRINT: ["105"]}
                                OS: {GENERALE: ["105"], POOLS: ["105"], MATRINT: ["105"]}
                                UG: {GENERALE: ["908", "909", "960", "105"], POOLS: ["908", "909", "960", "105"],…}
                                interaziendali: {GENERALE: ["CI"]}
                            
                                mi interessa la chiave CA -> GENERALE: lì dentro ci sono scritti i codici delle aziende di cui sono CA
                                quindi modifico la where condition facendo id_azienda dell'utente in (quelle aziende)
                            */
                            Map<String, Map<String, List<String>>> ruoliUtentiPersona = utente.getRuoliUtentiPersona();
                            BooleanExpression aziendeUtenteFilter = QUtente.utente.idAzienda.id.in(
                                // prendo le aziende dell'utente
                                userInfoService.getAziendePersona(persona).stream().filter(
                                    // filtro per tenere solo quelle di cui sono CA
                                    a -> ruoliUtentiPersona.get(Ruolo.CodiciRuolo.CA.toString()).get(Ruolo.ModuliRuolo.GENERALE.toString()).contains(a.getCodice())   
                                ).map(
                                    // prendo gli id delle aziende filtrate e converto in List
                                    a -> a.getId()).collect(Collectors.toList()
                                )
                            );
                            initialPredicate = aziendeUtenteFilter.and(initialPredicate);
                        }
                }
            }
        }
        return initialPredicate;
    }
}
