package it.bologna.ausl.internauta.service.interceptors.messaggero;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QAmministrazioneMessaggio;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it> with GDM and Gus collaboration
 */
@Component
@NextSdrInterceptor(name = "amministrazionemessaggi-interceptor")
public class AmministrazioneMessaggiInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmministrazioneMessaggiInterceptor.class);
    
    @Autowired
    KrintShpeckService krintShpeckService;
    
    @Override
    public Class getTargetEntityClass() {
        return AmministrazioneMessaggio.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        BooleanExpression filtroAmministrazioneMessaggi = null;
        if (mainEntity) {
            if (mt.getIdTag().getName().equals(Tag.SystemTagName.in_error.toString())) {
                krintShpeckService.writeMessageTag(mt.getIdMessage(), mt.getIdTag(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_ERRORE_NON_VISTO);
            } else if (mt.getIdTag().getName().equals(Tag.SystemTagName.assigned.toString()) || mt.getIdTag().getType().equals(Tag.TagType.CUSTOM.toString())) {
                krintShpeckService.writeMessageTag(mt.getIdMessage(), mt.getIdTag(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_AGGIUNTA_TAG);
            }
        }
        return mt;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        // KRINT dell'eliminazione del tag purché sia custom o "assigned" o "in_error"
        MessageTag mt = (MessageTag) entity;
        
        if (mainEntity) {
            if (mt.getIdTag().getName().equals(Tag.SystemTagName.in_error.toString())) {
                krintShpeckService.writeMessageTag(mt.getIdMessage(), mt.getIdTag(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_ERRORE_VISTO);
            } else if (mt.getIdTag().getName().equals(Tag.SystemTagName.assigned.toString()) || mt.getIdTag().getType().equals(Tag.TagType.CUSTOM.toString())) {
                krintShpeckService.writeMessageTag(mt.getIdMessage(), mt.getIdTag(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_ELIMINAZIONE_TAG);
            }
        }
        
        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
    
    

        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case GetAmministrazioneMessaggiAttivi:
                        filtroAmministrazioneMessaggi = QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNull()
                                .or( QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.gt(LocalDateTime.now()));
                        break;
                    case GetAmministrazioneMessaggiStorico:
                        filtroAmministrazioneMessaggi = QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.loe(LocalDateTime.now());
                        break;
               }
           }
        }
        return filtroAmministrazioneMessaggi != null ? filtroAmministrazioneMessaggi.and(initialPredicate) : initialPredicate;
    }
}