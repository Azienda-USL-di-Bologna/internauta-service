package it.bologna.ausl.internauta.service.interceptors.baborg;

import it.bologna.ausl.model.entities.baborg.Gdm1;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@NextSdrInterceptor(name = "gdm1-interceptorTest")
public class Gdm1InterceptorTest extends NextSdrEmptyControllerInterceptor {

    @Override
    public Class getTargetEntityClass() {
        return Gdm1.class;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Gdm1 gdm1 = (Gdm1) entity;
        if (gdm1.getId().equals("1"))
            throw new SkipDeleteInterceptorException("ho saltato volutamente il salvataggio");
    }
}
