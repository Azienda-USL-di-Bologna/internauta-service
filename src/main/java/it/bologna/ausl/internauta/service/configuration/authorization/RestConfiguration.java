package it.bologna.ausl.internauta.service.configuration.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${cors.allowed.origins}")
    private String allowedOriginsString;

    @Bean
    public CorsFilter corsFilter() {

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configResources = new CorsConfiguration();
        configResources.setAllowCredentials(true); // con la versione 2.4.2 di spring se settato a true vanno per forza settati anche gli allowed orgin a qualcosa diversa da *
//        config.addAllowedOrigin("http://localhost:4200");
//        config.addAllowedOrigin("*.internal.ausl.bologna.it");
        List<String> allowedOriginList = new ArrayList<>(Arrays.asList(allowedOriginsString.split(",")));
        configResources.setAllowedOrigins(allowedOriginList);
//        configResources.addAllowedHeader("*");
        configResources.addAllowedHeader("application");
        configResources.addAllowedHeader("authorization");
        configResources.addAllowedHeader("krint");
        configResources.addAllowedHeader("content-type");
        configResources.addAllowedMethod(HttpMethod.OPTIONS);
        configResources.addAllowedMethod(HttpMethod.GET);
        configResources.addAllowedMethod(HttpMethod.PUT);
        configResources.addAllowedMethod(HttpMethod.PATCH);
        configResources.addAllowedMethod(HttpMethod.POST);
        configResources.addAllowedMethod(HttpMethod.DELETE);
        
        CorsConfiguration configLogin = new CorsConfiguration();
        configLogin.setAllowCredentials(true);
        configLogin.setAllowedOrigins(allowedOriginList);
        configLogin.addAllowedHeader("*");
        configLogin.addAllowedMethod(HttpMethod.OPTIONS);
        configLogin.addAllowedMethod(HttpMethod.GET);
        configLogin.addAllowedMethod(HttpMethod.POST);
        
        source.registerCorsConfiguration("/internauta-api/login", configLogin);
        source.registerCorsConfiguration("/internauta-api/logout", configLogin);
        source.registerCorsConfiguration("/internauta-api/endpoint/login", configLogin);
        source.registerCorsConfiguration("/internauta-api/resources/**", configResources);
        return new CorsFilter(source);
    }
}
