package it.bologna.ausl.internauta.service.configuration.spring;

import it.bologna.ausl.internauta.service.interceptors.RequestInterceptor;
import it.bologna.ausl.internauta.service.repositories.logs.KrintRepository;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.nextsw.common.spring.resolver.DynamicOffsetLimitPageRequestOrPageRequestResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author spritz
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Autowired
    HttpSessionData httpSessionData;
    
    @Autowired
    KrintRepository krintRepository;
    
    static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 20);

//    @Override
//    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
//        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
//        resolver.setFallbackPageable(DEFAULT_PAGE_REQUEST);
//        argumentResolvers.add(resolver);
//        //super.addArgumentResolvers(argumentResolvers);
//    }
    
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        DynamicOffsetLimitPageRequestOrPageRequestResolver resolver = new DynamicOffsetLimitPageRequestOrPageRequestResolver();
        resolver.setFallbackPageable(DEFAULT_PAGE_REQUEST);
        //argumentResolvers.removeIf(e -> PageableHandlerMethodArgumentResolver.class.isAssignableFrom(e.getClass()));
        argumentResolvers.add(resolver);
        //super.addArgumentResolvers(argumentResolvers);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestInterceptor(httpSessionData, krintRepository));
    }
}
