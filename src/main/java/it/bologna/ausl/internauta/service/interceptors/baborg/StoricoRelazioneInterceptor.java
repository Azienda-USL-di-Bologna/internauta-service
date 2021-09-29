package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.StoricoRelazioneRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author Geair and Gdm
 */
@Component
@NextSdrInterceptor(name = "storicorelazione-interceptor")
public class StoricoRelazioneInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoricoRelazioneInterceptor.class);

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ParametriAziende parametriAziende;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    StoricoRelazioneRepository storicoRelazioneRepository;
    
    @Autowired
    InternautaUtils internautaUtils;

    @Override
    public Class getTargetEntityClass() {
        return StoricoRelazione.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        LOGGER.info("in: beforeSelectQueryInterceptor di Storico-Relazione");
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;

        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        boolean isCA = userInfoService.isCA(utente);
        boolean isCI = userInfoService.isCI(utente);
        boolean isSD = userInfoService.isSD(utente);

        String key = InternautaConstants.AdditionalData.Keys.dataRiferimento.toString();
        ZonedDateTime dataRiferimento;
        if (additionalData != null && additionalData.containsKey(key)) {
            dataRiferimento = Instant.ofEpochMilli(Long.parseLong(additionalData.get(key))).atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS);
        } else {
            dataRiferimento = ZonedDateTime.now();
        }
        BooleanExpression filter = qStoricoRelazione.attivaDal.loe(dataRiferimento)
                .and((qStoricoRelazione.attivaAl.isNull()).or(qStoricoRelazione.attivaAl.goe(dataRiferimento)));
        initialPredicate = filter.and(initialPredicate);

        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);

        if (operationsRequested == null || operationsRequested.isEmpty()) {
            initialPredicate = filter.and(initialPredicate);
        } else {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                String ruoliNomeBreveString;
                switch (operationRequested) {
                    case RootLoading:
                        ruoliNomeBreveString = additionalData.get(InternautaConstants.AdditionalData.Keys.ruoli.toString());

                        if (isCA || isCI || isSD || StringUtils.isEmpty(ruoliNomeBreveString)) {
                            BooleanExpression rootNodeFilter = qStoricoRelazione.idStrutturaPadre.isNull();
                            initialPredicate = rootNodeFilter.and(filter).and(initialPredicate);
                        } else {
                            try {
                                List<ParametroAziende> filtraResponsabiliMatrintParams = parametriAziende.getParameters("AccessoMatrintFiltratoPerRuolo", new Integer[]{utente.getIdAzienda().getId()});
                                if (filtraResponsabiliMatrintParams != null && !filtraResponsabiliMatrintParams.isEmpty() && parametriAziende.getValue(filtraResponsabiliMatrintParams.get(0), Boolean.class)) {
                                    Integer mascheraBit = internautaUtils.getSommaMascheraBit(ruoliNomeBreveString);
                                    //strutture su cui l'utente è responsabilmente diretto
                                    Map<String, Integer> struttureConStoricoRelazione = objectMapper.convertValue(
                                            storicoRelazioneRepository.getStruttureRuolo(mascheraBit, utente.getId(), dataRiferimento).get("result"),
                                            new TypeReference<Map<String, Integer>>() {
                                    });
                                    // aggiungo le strutture su cui sono responsabilmente delegato
                                    List<Integer> idUtentiDelegati = userInfoService.getPermessiDelega(utente);
                                    if (struttureConStoricoRelazione == null) {
                                        struttureConStoricoRelazione = new HashMap();
                                    }
                                    for (Integer idUtente : idUtentiDelegati) {
                                        Map<String, Integer> struttureStoricoRelazioneDelegato = objectMapper.convertValue(
                                                storicoRelazioneRepository.getStruttureRuolo(mascheraBit, idUtente, dataRiferimento).get("result"),
                                                new TypeReference<Map<String, Integer>>() {
                                        });

                                        struttureConStoricoRelazione.putAll(struttureStoricoRelazioneDelegato);
                                    }
                                    if (!struttureConStoricoRelazione.isEmpty()) {
                                        initialPredicate = QStoricoRelazione.storicoRelazione.id.in(struttureConStoricoRelazione.values()).and(initialPredicate);
                                    } else {
                                        initialPredicate = Expressions.FALSE.eq(true);
                                    }
                                } else {
                                    initialPredicate = Expressions.FALSE.eq(true);
                                }
                            } catch (Exception ex) {
                                initialPredicate = Expressions.FALSE.eq(true);
                                LOGGER.error("errore nell'interceptor di storicoRelazione", ex);
                            }
                        }
                    break;
//                    case FilterStrutturePoolsRuolo:
                    case FilterStruttureRuolo:
                        try {
                            ruoliNomeBreveString = additionalData.get(InternautaConstants.AdditionalData.Keys.ruoli.toString());
                            if (!isCA && !isCI && !isSD && !StringUtils.isEmpty(ruoliNomeBreveString)) {
                                List<ParametroAziende> filtraResponsabiliParams = parametriAziende.getParameters("AccessoPoolFiltratoPerRuolo", new Integer[]{utente.getIdAzienda().getId()});
                                if (filtraResponsabiliParams != null && !filtraResponsabiliParams.isEmpty() && parametriAziende.getValue(filtraResponsabiliParams.get(0), Boolean.class)) {
                                    Integer mascheraBit = internautaUtils.getSommaMascheraBit(ruoliNomeBreveString);
                                    Map<String, Integer> struttureRuoloEFiglie = objectMapper.convertValue(
                                            storicoRelazioneRepository.getStruttureRuoloEFiglie(mascheraBit, utente.getId(), dataRiferimento).get("result"),
                                            new TypeReference<Map<String, Integer>>(){}
                                    );
                                    if (struttureRuoloEFiglie != null && !struttureRuoloEFiglie.isEmpty()) {
                                        Collection<Integer> idStoricoRelazioneResponsabilita = struttureRuoloEFiglie.values();
                                        BooleanExpression filterRuolo = qStoricoRelazione.id.in(idStoricoRelazioneResponsabilita);
                                        initialPredicate = filterRuolo.and(initialPredicate);
                                    } else {
                                        initialPredicate = Expressions.FALSE.eq(true);
                                    }
                                } else {
                                    initialPredicate = Expressions.FALSE.eq(true);
                                }
                            }
                        } catch (Exception ex) {
                            throw new AbortLoadInterceptorException("errore nella chiamata alla funzione db get_strutture_ruolo_e_figlie", ex);
                        }
                    break;
                }
            }
        }
        return initialPredicate;
    }
}
