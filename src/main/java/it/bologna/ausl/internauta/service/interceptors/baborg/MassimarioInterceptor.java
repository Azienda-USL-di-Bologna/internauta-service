package it.bologna.ausl.internauta.service.interceptors.baborg;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.baborg.Massimario;
import it.nextsw.common.annotations.NextSdrInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author spritz
 */
@Component
@NextSdrInterceptor(name = "massimario-interceptor")
public class MassimarioInterceptor extends InternautaBaseInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MassimarioInterceptor.class);

    @Override
    public Class getTargetEntityClass() {
        return Massimario.class;
    }

}
