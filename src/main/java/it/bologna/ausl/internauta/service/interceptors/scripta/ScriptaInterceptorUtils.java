package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.google.common.base.CaseFormat;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.nextsw.common.interceptors.NextSdrControllerInterceptor;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class ScriptaInterceptorUtils {

    /**
     * Le partizioni di docdetail sono su idAzienda e su dataCreazione. Se
     * questi due campi sono usati come filtro di ricerca allora tali filtri
     * devono essere raddoppiati in modo che venga sfruttata la partizione sia
     * su DocDetail che su PersonaVedente.
     *
     * @param entityClass
     * @param dataCreazioneNameField
     * @return
     */
    public BooleanExpression duplicateFiltersPerPartition(Class entityClass, String dataCreazioneNameField, String idAziendaNameField) {
        BooleanExpression filter = Expressions.TRUE.eq(true);
        Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
        PathBuilder<?> qEntity = new PathBuilder(entityClass, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, entityClass.getSimpleName()));
        if (!filterDescriptorMap.isEmpty()) {
            Pattern pattern = Pattern.compile("\\.(.*?)(\\.|$)");
            Set<Path<?>> pathSet = filterDescriptorMap.keySet();
            for (Path<?> path : pathSet) {
                Matcher matcher = pattern.matcher(path.toString());
                matcher.find();
                String fieldName = matcher.group(1);
                if (fieldName.equals("idAzienda")) {
                    List<Object> ids = filterDescriptorMap.get(path);
                    BooleanExpression filterAziende = Expressions.TRUE.eq(false);
                    for (Object id : ids) {
                        PathBuilder<Azienda> qAzienda = qEntity.get(idAziendaNameField, Azienda.class);
                        filterAziende = filterAziende.or(qAzienda.get("id").eq((Integer) id));
//                        filter = filter.and(qAzienda.get("id").eq((Integer) id)); //ATTENZIONE QUI DOVREBBE ESSERE IN OR E NON IN AND
                    }
                    filter = filter.and(filterAziende);
                } else if (fieldName.equals("dataCreazione")) {
//                     if (List.class.isAssignableFrom(filterDescriptorMap.get(path).getClass())) {
                    DateTimePath<ZonedDateTime> dataCreazionePath = qEntity.getDateTime(dataCreazioneNameField, ZonedDateTime.class);

                    if (filterDescriptorMap.get(path).size() == 2) {
                        ZonedDateTime data1 = (ZonedDateTime) filterDescriptorMap.get(path).get(0);
                        ZonedDateTime data2 = (ZonedDateTime) filterDescriptorMap.get(path).get(1);
                        if (data1.isBefore(data2)) {

                            filter = filter.and(dataCreazionePath.goe(data1).and(dataCreazionePath.lt(data2)));
                        } else {
                            filter = filter.and(dataCreazionePath.goe(data2).and(dataCreazionePath.lt(data1)));
                        }
                    } else {
                        ZonedDateTime data = (ZonedDateTime) filterDescriptorMap.get(path).get(0);
                        ZonedDateTime startDate = data.toLocalDate().atTime(0, 0, 0).atZone(data.getZone());
                        ZonedDateTime endDate = startDate.plusDays(1);
                        filter = filter.and(dataCreazionePath.goe(startDate).and(dataCreazionePath.lt(endDate)));
                    }
                }
            }
        }
        return filter;
    }
}
