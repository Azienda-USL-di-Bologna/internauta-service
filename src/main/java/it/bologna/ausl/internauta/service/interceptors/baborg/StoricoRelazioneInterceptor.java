package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.nextsw.common.annotations.NextSdrInterceptor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Geair and Gdm
 */
@Component
@NextSdrInterceptor(name = "storicorelazione-interceptor")
public class StoricoRelazioneInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoricoRelazioneInterceptor.class);
   
    

    @Override
    public Class getTargetEntityClass() {
        return StoricoRelazione.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) {
        LOGGER.info("in: beforeSelectQueryInterceptor di Storico-Relazione");
        
        String key = InternautaConstants.AdditionalData.Keys.dataRiferimento.toString();
        if (additionalData != null && additionalData.containsKey(key)) {

            LocalDateTime dataRiferimento = Instant.ofEpochMilli(Long.parseLong(additionalData.get(key))).atZone(ZoneId.systemDefault()).toLocalDateTime();
            QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;

            BooleanExpression filter = qStoricoRelazione.attivaDal.loe(dataRiferimento)
                    .and((qStoricoRelazione.attivaAl.isNull()).or(qStoricoRelazione.attivaAl.goe(dataRiferimento)));
            initialPredicate = filter.and(initialPredicate);
        }
      
        return initialPredicate;
    }
}
