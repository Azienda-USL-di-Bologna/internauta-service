package it.bologna.ausl.baborg.service.configuration;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author Utente
 */
@Configuration
public class PageableConfiguration implements WebMvcConfigurer {

    static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 20);

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setFallbackPageable(DEFAULT_PAGE_REQUEST);
        argumentResolvers.add(resolver);
        //super.addArgumentResolvers(argumentResolvers);
    }
}
