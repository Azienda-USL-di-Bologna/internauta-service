package it.bologna.ausl.baborg.service.interceptors;

import it.bologna.ausl.model.entities.baborg.Gdm2;
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
@NextSdrInterceptor(name = "gdm2-interceptorTest")
public class Gdm2InterceptorTest extends NextSdrEmptyControllerInterceptor {

    @Override
    public Class getTargetEntityClass() {
        return Gdm2.class;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Gdm2 gdm2 = (Gdm2) entity;
        if (gdm2.getId().equals("2"))
            throw new SkipDeleteInterceptorException("ho saltato volutamente il salvataggio");
    }
}
