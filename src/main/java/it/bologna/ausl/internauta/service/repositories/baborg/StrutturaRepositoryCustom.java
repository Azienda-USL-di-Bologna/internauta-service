package it.bologna.ausl.internauta.service.repositories.baborg;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StrutturaRepositoryCustom {

   
    public List<Map<String, Object>> getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(Integer idStruttura, LocalDateTime dataRiferimento) throws SQLException;
//    public List<Integer> getStruttureRuoloEFiglie(Integer bitRuoli, Integer idUtente, LocalDateTime date) throws SQLException;
}
