package it.bologna.ausl.internauta.service.configuration.authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 *
 * @author user
 */
@Configuration
public class RestConfiguration {

    @Bean
    public CorsFilter corsFilter() {

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false); // you USUALLY want this (gdm: era true, ma ho dovuto mettere false altrimenti con la versione 2.4.2 di spring non partiva più)
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedOrigin("*.internal.ausl.bologna.it");
        config.addAllowedOrigin("*.avec.emr.it");
        config.addAllowedHeader("*");
        config.addAllowedMethod(HttpMethod.OPTIONS);
        config.addAllowedMethod(HttpMethod.GET);
        config.addAllowedMethod(HttpMethod.PUT);
        config.addAllowedMethod(HttpMethod.PATCH);
        config.addAllowedMethod(HttpMethod.POST);
        config.addAllowedMethod(HttpMethod.DELETE);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
