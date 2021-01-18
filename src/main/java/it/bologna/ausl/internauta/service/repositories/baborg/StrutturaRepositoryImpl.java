package it.bologna.ausl.internauta.service.repositories.baborg;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.jpa.natiquery.NativeQueryTools;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocalDateTimeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class StrutturaRepositoryImpl implements StrutturaRepositoryCustom {

    @PersistenceContext
    private EntityManager em;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Transactional
    @Override
    public List<Map<String, Object>> getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(Integer idStruttura, LocalDateTime dataRiferimento) throws SQLException {
        String queryString = "select * from baborg.get_utenti_struttura_sottoresponsabili_filtered_at_date(?, ?)";
        Query query = em.createNativeQuery(queryString);
        query.setParameter(1, new TypedParameterValue(IntegerType.INSTANCE, idStruttura));
        query.setParameter(2, new TypedParameterValue(LocalDateTimeType.INSTANCE, dataRiferimento));
        List<Object[]> resultList = query.getResultList();
//        System.out.println("dentro:");
//        resultList.stream().forEach(r -> System.out.println(r[1]));

        NativeQueryTools nativeQueryTools = new NativeQueryTools(em);
        List<Map<String, Object>> res = nativeQueryTools.asListOfMaps(resultList, nativeQueryTools.getColumnNameToIndexMap(queryString));

        return res;
    }
    
    @Transactional
    @Override
    public List<Integer> getStruttureRuoloEFiglie(Integer bitRuoli, Integer idUtente, LocalDateTime date) throws SQLException {
        String queryString = "select baborg.get_strutture_ruolo_e_figlie(?, ?, ?);";
        Query query = em.createNativeQuery(queryString);
        query.setParameter(1, new TypedParameterValue(IntegerType.INSTANCE, bitRuoli));
        query.setParameter(2, new TypedParameterValue(IntegerType.INSTANCE, idUtente));
        query.setParameter(3, new TypedParameterValue(LocalDateTimeType.INSTANCE, date));
        List<Integer> resultList = query.getResultList();
        return resultList;
    }
}
