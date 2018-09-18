package it.bologna.ausl.internauta.service.interceptors.baborg;

//package it.bologna.ausl.baborg.service.interceptors;
//
//import com.querydsl.core.types.Predicate;
//import it.bologna.ausl.model.entities.baborg.Utente;
//import it.nextsw.common.annotations.NextSdrInterceptor;
//import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
//import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
//import java.util.Map;
//import javax.servlet.http.HttpServletRequest;
//import org.springframework.stereotype.Component;
//
///**
// *
// * @author gdm
// */
//@Component
//@NextSdrInterceptor(name = "utente-interceptorTest")
//public class UtenteInterceptorTest extends NextSdrEmptyControllerInterceptor {
//
//    @Override
//    public Class getTargetEntityClass() {
//        return Utente.class;
//    }
//    
//    @Override
//    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) {
//        System.out.println("in: beforeSelectQueryInterceptor di UtenteStruttura");
////        return QUtenteStruttura.utenteStruttura.id.ne(3023029).and(initialPredicate);
//    return initialPredicate;
//    }
//
//    @Override
//    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) {
//        System.out.println("in: afterSelectQueryInterceptor di " + entity.getClass().getSimpleName());
//        return entity;
//    }
//
//    @Override
//    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
//        System.out.println("in: beforeCreateEntityInterceptor di " + entity.getClass().getSimpleName());
//        Utente utente = (Utente) entity;
//        utente.setCodiceFiscale(utente.getCodiceFiscale()+ "_INT");
//        System.out.println("CodiceFiscale: " + utente.getCodiceFiscale());
//        return utente;
//    }
//
//    @Override
//    public Object beforeUpdateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
//        System.out.println("in: beforeUpdateEntityInterceptor di " + entity.getClass().getSimpleName());
//        Utente utente = (Utente) entity;
//        utente.setCodiceFiscale(utente.getCodiceFiscale()+ "_cambiato nell'interceptor beforeupdate");
//        return utente;
//    }
//}
