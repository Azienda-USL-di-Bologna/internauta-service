/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.repositories.gru;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mdonza
 */
public interface MdrStrutturaRepositoryCustom {
    
    public Map<Integer, List<Map<String, Object>>> selectDateOnStruttureByIdAzienda(Integer idAzienda) throws SQLException;
    
}
