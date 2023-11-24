package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.model.entities.baborg.QTipologiaStruttura;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.TipologiaStruttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "azienda-interceptor")
public class TipologiaStrutturaInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TipologiaStrutturaInterceptor.class);

    @Autowired
    PersonaRepository personaRepository;
    

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public Class getTargetEntityClass() {
        return TipologiaStruttura.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        if (mainEntity) {
            AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
            Utente utente = authenticatedUserProperties.getUser();
            boolean isCA = userInfoService.isCA(utente);
            boolean isCI = userInfoService.isCI(utente);
            boolean isSD = userInfoService.isSD(utente);
            List<String> predicati = new ArrayList();
            // TODO: bisognerebbe mettere il modulo adeguato e non generale
            List<String> ruoli = utente.getMappaRuoli().get(Ruolo.ModuliRuolo.POOLS.toString()).stream().map(ruolo -> ruolo.getNomeBreve().toString()).collect(Collectors.toList());
            try {
                //            Map<String, List<PermessoEntitaStoredProcedure>> permessiDiFlussoByCodiceAzienda = utente.getPermessiDiFlussoByCodiceAzienda();
//            List<PermessoEntitaStoredProcedure> permessiDiFlusso = utente.getPermessiDiFlusso();
                Map<String, List<String>> predicatiDiFlussoByCodiceAzienda = userInfoService.getPredicatiDiFlussoByCodiceAzienda(authenticatedUserProperties.getPerson());
                predicati = predicatiDiFlussoByCodiceAzienda.get(utente.getIdAzienda().getCodice());
            } catch (BlackBoxPermissionException ex) {
                throw new AbortLoadInterceptorException("errore nel caricamento predicati di flusso");
            }
            if (!isCA && !isCI && !isSD) {
                BooleanTemplate booleanTemplateRuoli = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", QTipologiaStruttura.tipologiaStruttura.ruoli, String.join(",", ruoli));
                BooleanTemplate booleanTemplatePermessi = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", QTipologiaStruttura.tipologiaStruttura.predicati, String.join(",", predicati));
                initialPredicate = (booleanTemplateRuoli.or(booleanTemplatePermessi)).and(initialPredicate);

            }
        }
        return initialPredicate;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return super.afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return super.afterSelectQueryInterceptor(entities, additionalData, request, mainEntity, projectionClass);
    }

}
