package it.bologna.ausl.internauta.service.interceptors.baborg;

//package it.bologna.ausl.baborg.service.interceptors;
//
//import com.querydsl.core.types.Predicate;
//import it.bologna.ausl.model.entities.baborg.TipoPermesso;
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
//@NextSdrInterceptor(name = "tipopermesso-interceptorTest")
//public class TipoPermessoInterceptorTest extends NextSdrEmptyControllerInterceptor {
//
//    @Override
//    public Class getTargetEntityClass() {
//        return TipoPermesso.class;
//    }
//
//    @Override
//    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
//        System.out.println("in: beforeCreateEntityInterceptor di " + entity.getClass().getSimpleName());
//        TipoPermesso tipoPermesso = (TipoPermesso) entity;
//        tipoPermesso.setNomeVisualizzazione(tipoPermesso.getNomeVisualizzazione() + "_cambiato nell'interceptor beforeinsert");
//        return tipoPermesso;
//    }
//
//    @Override
//    public Object beforeUpdateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
//        System.out.println("in: beforeUpdateEntityInterceptor di " + entity.getClass().getSimpleName());
//        TipoPermesso tipoPermesso = (TipoPermesso) entity;
//        tipoPermesso.setNomeVisualizzazione(tipoPermesso.getNomeVisualizzazione() + "_cambiato nell'interceptor beforeupdate");
//        return tipoPermesso;
//    }
//}
