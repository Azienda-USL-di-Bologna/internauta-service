package it.bologna.ausl.internauta.service.interceptors.rubrica;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "contatto-interceptor")
public class ContattoInterceptor extends InternautaBaseInterceptor{
    private static final Logger LOGGER = LoggerFactory.getLogger(ContattoInterceptor.class);
    
    @Autowired
    ContattoRepository contattoRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Override
    public Class getTargetEntityClass() {
        return Contatto.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(user.getIdPersona());
        // List<Integer> idAziendePersona = aziendePersona.stream().map(a -> a.getId()).collect(Collectors.toList());
        
        BooleanExpression permessoAziendaleFilter = QContatto.contatto.idAziende.isNull().or(
            Expressions.booleanTemplate("tools.array_overlap({0}, tools.string_to_integer_array({1}, ','))=true", 
                QContatto.contatto.idAziende, org.apache.commons.lang3.StringUtils.join(aziendePersona.stream().map(a -> a.getId()).collect(Collectors.toList()), ",")
            )
        );
        
        
        initialPredicate = permessoAziendaleFilter.and(initialPredicate);
     
        return initialPredicate;
    }
    
//    @Override
//    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
//        Contatto c = (Contatto) entity;
//        if (c.getCategoria().equals(Contatto.CategoriaContatto.GRUPPO)) {
//            this.httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.ContattoGruppoAppenaCreato, c);
//        }
//        
//        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
//    }
}
    

