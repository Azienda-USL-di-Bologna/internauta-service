package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
@NextSdrInterceptor(name = "utentestruttura-interceptor")
public class UtenteStrutturaInterceptor extends InternautaBaseInterceptor {

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    ProjectionFactory projectionFactory;

    private static final String FILTER_COMBO = "filterCombo";

    @Override
    public Class getTargetEntityClass() {
        return UtenteStruttura.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) {
        System.out.println("in: beforeSelectQueryInterceptor di UtenteStruttura");

        String filterComboValue = additionalData.get(FILTER_COMBO);

        if (filterComboValue != null) {
            BooleanExpression customFilter = QUtenteStruttura.utenteStruttura.idUtente.idPersona.cognome
                    .concat(" ")
                    .concat(QUtenteStruttura.utenteStruttura.idUtente.idPersona.nome)
                    .containsIgnoreCase(filterComboValue);
            initialPredicate = customFilter.and(initialPredicate);
        }

        // vogliamo che per default si cerchino le righe con campo attivo = true
        // NB: il front-end a volte lo mette già nei filtri dell'initialPredicate
        BooleanExpression customFilterUtenteStrutturaAttivo = QUtenteStruttura.utenteStruttura.attivo.eq(true);
        initialPredicate = customFilterUtenteStrutturaAttivo.and(initialPredicate);

        return initialPredicate;
    }

    private Object getUtenteStruttura(Map<String, Object> map, Class projection) {
        UtenteStruttura utenteStruttura = utenteStrutturaRepository.getOne((Integer) map.get("id"));
        Object res = projectionFactory.createProjection(projection, utenteStruttura);
        return res;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request,
            boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        // if richiede di caricare sotto resp
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested
                = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operation : operationsRequested) {
                switch (operation) {
                    case CaricaSottoResponsabili:
                        //idProvenienzaOggetto=28618
                        String idStrutturaString = additionalData.get(InternautaConstants.AdditionalData.Keys.idProvenienzaOggetto.toString());
                        if (StringUtils.hasText(idStrutturaString)) {
                            Integer idStruttura = Integer.parseInt(idStrutturaString);
                            List<Map<String, Object>> utentiStrutturaSottoResponsabili = strutturaRepository.getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(idStruttura);

                            List<Object> res = utentiStrutturaSottoResponsabili.stream().map(utenteStrutturaMap -> {
                                Object utenteStruttura = this.getUtenteStruttura(utenteStrutturaMap, projectionClass);
                                //return factory.createProjection(UtenteStrutturaWithIdUtente.class, utenteStruttura);
                                return utenteStruttura;
                            }).collect(Collectors.toList());
                            entities.addAll(res);
                        }
                        break;

                }
            }
        }
        return entities;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return entity;
    }
}
