package it.bologna.ausl.internauta.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.repositories.configurazione.ParametroAziendeRepository;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.configuration.QParametroAziende;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class ParametriAziende {

    @Autowired
    ParametroAziendeRepository parametroAziendeRepository;
    
    @Autowired
    ObjectMapper objectMapper;
    

    public ParametriAziende() {
    }
    
    public List<ParametroAziende> getParameters(String nome) {
        return getParameters(nome, null, null);
    }
    
    public List<ParametroAziende> getParameters(InternautaConstants.Configurazione.ParametriAzienda nome) {
        return getParameters(nome.toString(), null, null);
    }
    
    public List<ParametroAziende> getParameters(String nome, Integer[] idAziende) {
        return getParameters(nome, idAziende, null);
    }
    
    public List<ParametroAziende> getParameters(String nome, String[] idApplicazioni) {
        return getParameters(nome, null, idApplicazioni);
    }

    public List<ParametroAziende> getParameters(String nome, Integer[] idAziende, String[] idApplicazioni) {
        BooleanExpression filter = QParametroAziende.parametroAziende.nome.eq(nome);
        if (idAziende != null) {
            BooleanTemplate filterAzienda = Expressions.booleanTemplate("tools.array_overlap({0}, tools.string_to_integer_array({1}, ','))=true", QParametroAziende.parametroAziende.idAziende, org.apache.commons.lang3.StringUtils.join(idAziende, ","));
            filter = filter.and(filterAzienda);
        }
        if (idApplicazioni != null) {
            BooleanTemplate filterApplicazioni = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", QParametroAziende.parametroAziende.idApplicazioni, org.apache.commons.lang3.StringUtils.join(idApplicazioni, ","));
            filter = filter.and(filterApplicazioni);
        }

        Iterable<ParametroAziende> parametriFound = parametroAziendeRepository.findAll(filter);
        List<ParametroAziende> res = new ArrayList();
        parametriFound.forEach(res::add);
        return res;
    }

    /**
     * Estrae il valore del parametro e lo converte nel tipo passato
     * @param <T>
     * @param parametroAziende 
     * @param valueType
     * @return 
     * @throws java.io.IOException 
     */    
    public <T extends Object> T getValue(ParametroAziende parametroAziende, Class<T> valueType) throws IOException {
        return objectMapper.readValue(parametroAziende.getValore(), valueType);
    }
}
