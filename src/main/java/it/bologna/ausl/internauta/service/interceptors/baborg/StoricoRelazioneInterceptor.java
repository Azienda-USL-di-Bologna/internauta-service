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
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.nextsw.common.annotations.NextSdrInterceptor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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

    @Override
    public Class getTargetEntityClass() {
        return StoricoRelazione.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) {
        LOGGER.info("in: beforeSelectQueryInterceptor di Storico-Relazione");
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
        
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        boolean isCA = userInfoService.isCA(utente);
        boolean isCI = userInfoService.isCI(utente);
        
        String key = InternautaConstants.AdditionalData.Keys.dataRiferimento.toString();
        LocalDateTime dataRiferimento;
        if (additionalData != null && additionalData.containsKey(key)) {
            dataRiferimento = Instant.ofEpochMilli(Long.parseLong(additionalData.get(key))).atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(ChronoUnit.DAYS);
        } else {
            dataRiferimento = LocalDateTime.now();
        }
        BooleanExpression filter = qStoricoRelazione.attivaDal.loe(dataRiferimento)
                                    .and((qStoricoRelazione.attivaAl.isNull()).or(qStoricoRelazione.attivaAl.goe(dataRiferimento)));
                            initialPredicate = filter.and(initialPredicate);
        
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        
        if (operationsRequested == null || operationsRequested.isEmpty()) {
                initialPredicate = filter.and(initialPredicate);
        } else {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case RootLoading:
                        String ruoliNomeBreveString = additionalData.get(InternautaConstants.AdditionalData.Keys.ruoli.toString());
                        
                        if (isCA || isCI || StringUtils.isEmpty(ruoliNomeBreveString)) {
                            BooleanExpression rootNodeFilter = qStoricoRelazione.idStrutturaPadre.isNull();
                            initialPredicate = rootNodeFilter.and(filter).and(initialPredicate);
                        } else {
                            try {
                                List<ParametroAziende> filtraResponsabiliMatrintParams = parametriAziende.getParameters("AccessoMatrintFiltratoPerRuolo", new Integer[] {utente.getIdAzienda().getId()});
                                if (filtraResponsabiliMatrintParams != null && !filtraResponsabiliMatrintParams.isEmpty() && parametriAziende.getValue(filtraResponsabiliMatrintParams.get(0), Boolean.class)) {
                                    Integer mascheraBit = getSommaMascheraBit(ruoliNomeBreveString);
                                    Map<String, Integer> struttureConStoricoRelazione = objectMapper.convertValue(
                                            storicoRelazioneRepository.getStruttureRuolo(mascheraBit, utente.getId(), dataRiferimento).get("result"), 
                                            new TypeReference<Map<String, Integer>>(){});
                                    initialPredicate = QStoricoRelazione.storicoRelazione.id.in(struttureConStoricoRelazione.values()).and(initialPredicate);
                                } else {
                                    initialPredicate = Expressions.FALSE.eq(true);
                                }
                            } catch (Exception ex) {
                                initialPredicate = Expressions.FALSE.eq(true);
                                LOGGER.error("errore nell'interceptor di storicoRelazione", ex);
                            }
                        }
                }
            }
        }
        return initialPredicate;
    }
    
    private Integer getSommaMascheraBit(String ruoliNomeBreveString) {
        Integer res = 0;
        String[] ruoliSplitted = ruoliNomeBreveString.split(";");
        for (String ruoloNomeBreve : ruoliSplitted) {
            Ruolo ruolo = cachedEntities.getRuoloByNomeBreve(Ruolo.CodiciRuolo.valueOf(ruoloNomeBreve.toUpperCase()));
            Integer mascheraBit = ruolo.getMascheraBit();
            res += mascheraBit;
        }
        return res;
    }
}
