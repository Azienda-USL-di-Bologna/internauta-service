/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.repositories.gru;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author mdonza
 */
@Component
public class MdrAppartenentiRepositoryCustomImpl implements MdrAppartenentiRepositoryCustom{
    @PersistenceContext
    private EntityManager em;

    @Autowired
    ObjectMapper objectMapper;
    
    @Override
    public Map<String, List<Map<String, Object>>> selectDateOnAppartenentiByIdAzienda(Integer idAzienda) throws SQLException {
        String queryString = "select * from gru.select_appartenenti_date("+ idAzienda +")";
        Query query = em.createNativeQuery(queryString);
        //query.setParameter ( 1 , new TypedParameterValue(IntegerType.INSTANCE, idAzienda));
//        Object resultList = query.getResultList();
        Object resDB = query.getSingleResult();
        Map<String, List<Map<String, Object>>> res = objectMapper.convertValue(resDB, new TypeReference<Map<String, List<Map<String, Object>>>>() { });
        //NativeQueryTools nativeQueryTools = new NativeQueryTools(em);
        //List<Map<String, Object>> res = nativeQueryTools.asListOfMaps(resultList, nativeQueryTools.getColumnNameToIndexMap(queryString));
        return res;
    }
}
