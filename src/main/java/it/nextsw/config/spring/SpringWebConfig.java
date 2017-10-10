package it.nextsw.config.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
//@ComponentScan(basePackages = "it.nextsw")
//@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = {"it.nextsw.service"})
@ImportResource({"classpath:spring/spring.xml", "classpath:spring/mail-config.xml"})
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