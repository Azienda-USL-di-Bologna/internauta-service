package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.QPec;
import it.bologna.ausl.model.entities.baborg.QTitolo;
import it.bologna.ausl.model.entities.baborg.Titolo;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author spritz
 */
@Component
@NextSdrInterceptor(name = "titolo-interceptor")
public class TitoloInterceptor extends InternautaBaseInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TitoloInterceptor.class);

    @Override
    public Class getTargetEntityClass() {
        return Titolo.class;
    }

//    @Override
//    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
//        Integer idAzienda = null;
//        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
//        if (operationsRequested != null && !operationsRequested.isEmpty()) {
//
//            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
//                switch (operationRequested) {
//                    case FilterMassimarioPerAzienda:
//                        idAzienda = Integer.parseInt(additionalData.get(InternautaConstants.AdditionalData.Keys.idAzienda.toString()));
//                        BooleanExpression filter = QTitolo.titolo.idAzienda.id.eq(idAzienda);
//                        initialPredicate = filter.and(initialPredicate);
//                        break;
//                }
//            }
//        }
//        return initialPredicate;
//    }
}
