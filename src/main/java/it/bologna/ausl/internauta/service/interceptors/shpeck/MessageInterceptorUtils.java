package it.bologna.ausl.internauta.service.interceptors.shpeck;

import com.google.common.base.CaseFormat;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class MessageInterceptorUtils {
    
    @Autowired
    UserInfoService userInfoService;

    public BooleanExpression messageInPecWithPermission(AuthenticatedSessionData authenticatedSessionData, Class entityClass) throws AbortLoadInterceptorException {
        Persona persona = authenticatedSessionData.getPerson();
        PathBuilder<?> qEntity = new PathBuilder(entityClass, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,entityClass.getSimpleName()));
        try {
            Map<Integer, List<String>> permessiPec = userInfoService.getPermessiPec(persona);
            if (!permessiPec.isEmpty()) {
                List<Integer> myPec = new ArrayList<Integer>();
                myPec.addAll(permessiPec.keySet());
                PathBuilder<Pec> qpec = qEntity.get("idPec", Pec.class);
                return qpec.get("id").in(myPec);
            } else {
                return Expressions.FALSE.eq(true);
            }
        } catch (BlackBoxPermissionException ex) {
            throw new AbortLoadInterceptorException("errore nella lettura del permessi sulle caselle pec", ex);
        }
    }
}
