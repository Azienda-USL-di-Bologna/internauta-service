//package it.bologna.ausl.baborg.service.interceptors;
//
//import com.querydsl.core.types.Predicate;
//import it.bologna.ausl.model.entities.baborg.Permesso;
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
//@NextSdrInterceptor(name = "permesso-interceptorTest")
//public class PermessoInterceptorTest extends NextSdrEmptyControllerInterceptor {
//
//    @Override
//    public Class getTargetEntityClass() {
//        return Permesso.class;
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
//        Permesso permesso = (Permesso) entity;
//        System.out.println(permesso.getProvenienza());
//        permesso.setProvenienza(permesso.getProvenienza()  + "_cambiato nell'interceptor beforeinsert");
//        return permesso;
//    }
//
//    @Override
//    public Object beforeUpdateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
//        System.out.println("in: beforeUpdateEntityInterceptor di " + entity.getClass().getSimpleName());
//        Permesso permesso = (Permesso) entity;
//        System.out.println(permesso.getProvenienza());
//        permesso.setProvenienza(permesso.getProvenienza()  + "_cambiato nell'interceptor beforeupdate");
//        return permesso;
//    }
//}
