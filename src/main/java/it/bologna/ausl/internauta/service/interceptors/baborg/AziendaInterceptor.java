package it.bologna.ausl.internauta.service.interceptors.baborg;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@NextSdrInterceptor(name = "azienda-interceptorTest")
public class AziendaInterceptor extends NextSdrEmptyControllerInterceptor {

    @Override
    public Class getTargetEntityClass() {
        return Azienda.class;
    }

//    @Override
//    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) {
//        return QAzienda.azienda.id.eq(2).and(initialPredicate);
//    }

//    @Override
//    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) {
//        if (entity != null) {
//            Azienda azienda = (Azienda) entity;
//            if (azienda.getId() != 2) {
//                System.out.println("222222222222222222");
//                return null;
//            }
//            else {
//                System.out.println("hahahahahahayh");
//                return azienda;
//            }
//        }
//        else
//            return entity;
//    }
    
    
    
}
