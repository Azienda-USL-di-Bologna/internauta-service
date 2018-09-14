package it.bologna.ausl.baborg.service.interceptors;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author gus and zolo
 */
@Component
@NextSdrInterceptor(name = "struttura-unificata-interceptor")
public class StrutturaUnificataInterceptor extends NextSdrEmptyControllerInterceptor {
    
    private static final String GET_DATA_BY_STATO = "getDataByStato";
    private static enum Stati { Bozza, Corrente, Storico  };
       
    @Override
    public Class getTargetEntityClass() {
        return StrutturaUnificata.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) {
        System.out.println("in: beforeSelectQueryInterceptor di Struttura-Unificata");
        
        String getDataByStatoValue = additionalData.get(GET_DATA_BY_STATO);
        
        if (getDataByStatoValue != null) {
            LocalDateTime today = LocalDate.now().atTime(0, 0);
            QStrutturaUnificata strutturaUnificata = QStrutturaUnificata.strutturaUnificata;
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Utente utente = (Utente) authentication.getPrincipal();
            List<Ruolo> ruoli = utente.getRuoli();
            BooleanExpression customFilter;
            Boolean isCA = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.CA);
            Boolean isCI = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.CI);
            
            if (getDataByStatoValue.equals(Stati.Bozza.toString())) {
                /*  La bozza ha la data disattivazione a null.
                    La data attivazione può essere null.
                    Se non è null deve essere maggiore di oggi
                    oppure può essere minore/uguale ad oggi purché la data accensione ativazione sia null sia disattivo.  */
                if (isCI) {
                    customFilter = strutturaUnificata.dataDisattivazione.isNull()
                        .and(strutturaUnificata.dataAttivazione.isNull()
                                .or(strutturaUnificata.dataAttivazione.isNotNull().and(strutturaUnificata.dataAttivazione.gt(today))
                                    .or(strutturaUnificata.dataAttivazione.isNotNull().and(strutturaUnificata.dataAttivazione.loe(today)).and(strutturaUnificata.dataAccensioneAttivazione.isNull()))));
                } else {
                    customFilter = Expressions.asBoolean(true).isFalse();
                }
                
                initialPredicate = customFilter.and(initialPredicate);
                
            } else if(getDataByStatoValue.equals(Stati.Corrente.toString())) {
                if (isCA || isCI) {
                    customFilter = strutturaUnificata.dataDisattivazione.isNull()
                            .or(strutturaUnificata.dataDisattivazione.isNotNull().and(strutturaUnificata.dataDisattivazione.gt(today)))
                            .and(strutturaUnificata.dataAttivazione.isNotNull()
                                .and(strutturaUnificata.dataAttivazione.loe(today)
                                .and(strutturaUnificata.dataAccensioneAttivazione.isNotNull())));
                } else {
                    customFilter = Expressions.asBoolean(true).isFalse();
                }
                
                initialPredicate = customFilter.and(initialPredicate);
                
            } else if(getDataByStatoValue.equals(Stati.Storico.toString())) {
                if (isCA || isCI) {
                    customFilter = strutturaUnificata.dataDisattivazione.isNotNull()
                        .and(strutturaUnificata.dataDisattivazione.loe(today));
                } else {
                    customFilter = Expressions.asBoolean(true).isFalse();
                } 
                
                initialPredicate = customFilter.and(initialPredicate);
                
            } else {
                customFilter = Expressions.asBoolean(true).isFalse();
                initialPredicate = customFilter.and(initialPredicate);
            }
        }
        return initialPredicate;
    }
    
    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        System.out.println("in: beforeCreateEntityInterceptor di Struttura-Unificata");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        List<Ruolo> ruoli = utente.getRuoli();
        Boolean isCI = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.CI);
        if (!isCI) {
            throw new AbortSaveInterceptorException();
        }
        
        return entity;
    }
    
    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        System.out.println("in: beforeUpdateEntityInterceptor di Struttura-Unificata");
         
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        List<Ruolo> ruoli = utente.getRuoli();
        Boolean isCI = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.CI);
        if (!isCI) {
            throw new AbortSaveInterceptorException();
        }
        
        return entity;
    }
}