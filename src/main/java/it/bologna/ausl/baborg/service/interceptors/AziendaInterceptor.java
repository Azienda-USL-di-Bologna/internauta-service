package it.bologna.ausl.baborg.service.interceptors;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.baborg.model.entities.Azienda;
import it.bologna.ausl.baborg.model.entities.QAzienda;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
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
