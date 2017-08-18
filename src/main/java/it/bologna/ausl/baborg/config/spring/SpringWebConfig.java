package it.bologna.ausl.baborg.config.spring;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
//@ComponentScan("it.bologna.ausl.entities")
//@ComponentScan(basePackages = {"it.nextsw", "it.bologna.ausl"})
//@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = {"it.bologna.ausl.baborg.service"})
@ImportResource("classpath:spring/spring.xml")
public class SpringWebConfig {






    //    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurerAdapter() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//                registry.addMapping("/**")
//                        .allowedOrigins("*")
//                        .allowedHeaders("*")
//                        .allowedMethods("*")
//                        //.exposedHeaders("*")
//                        .allowCredentials(false).maxAge(3600);
//            }
//        };
//    }

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("*")
//                .allowedMethods("PUT", "DELETE","OPTIONS","POST","GET")
//                .allowedHeaders("*")
//                .exposedHeaders("*")
//                .allowCredentials(false).maxAge(3600);
//    }
}