package it.bologna.ausl.internauta.service.repositories.gru;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.repositories.gru.MdrStrutturaRepositoryCustom;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocalDateTimeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author mdonza
 */
@Component
public class MdrStrutturaRepositoryCustomImpl implements MdrStrutturaRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public Map<Integer, List<Map<String, Object>>> selectDateOnStruttureByIdAzienda(Integer idAzienda) throws SQLException {
        String queryString = "select * from gru.select_strutture_date("+ idAzienda +")";
        Query query = em.createNativeQuery(queryString);
        //query.setParameter ( 1 , new TypedParameterValue(IntegerType.INSTANCE, idAzienda));
//        Object resultList = query.getResultList();
        Object resDB = query.getSingleResult();
        Map<Integer, List<Map<String, Object>>> res = objectMapper.convertValue(resDB, new TypeReference<Map<Integer, List<Map<String, Object>>>>() { });
        //NativeQueryTools nativeQueryTools = new NativeQueryTools(em);
        //List<Map<String, Object>> res = nativeQueryTools.asListOfMaps(resultList, nativeQueryTools.getColumnNameToIndexMap(queryString));
        return res;
    }
    
    public Object testSal(Integer idAzienda) throws SQLException {
        String queryString = "with arrays as (select id_casella, array_agg( json_build_object('datain',cast(datain as date), 'datafi', cast(datafi as date)))	FROM gru.mdr_struttura WHERE id_azienda= :idAzienda group by id_casella) select cast(json_object_agg(id_casella, array_agg) as text) from arrays ;";
        Object res = em.createNativeQuery(queryString).setParameter("idAzienda", idAzienda).getSingleResult();
        return res;
    }
    
    
}
