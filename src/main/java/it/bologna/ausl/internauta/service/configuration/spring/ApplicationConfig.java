package it.bologna.ausl.internauta.service.configuration.spring;

//package it.bologna.ausl.baborg.service.configuration;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.util.List;
//import org.springframework.http.converter.HttpMessageConverter;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
//import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class ApplicationConfig extends WebMvcConfigurerAdapter {
//    public MappingJackson2HttpMessageConverter jacksonMessageConverter(){
//        MappingJackson2HttpMessageConverter messageConverter = new  MappingJackson2HttpMessageConverter();
//
//        ObjectMapper mapper = new ObjectMapper();
//        //Registering Hibernate4Module to support lazy objects
//        mapper.registerModule(new Hibernate5Module());
//
//        messageConverter.setObjectMapper(mapper);
//        return messageConverter;
//
//    }
//
//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        //Here we add our custom-configured HttpMessageConverter
//        converters.add(jacksonMessageConverter());
//        super.configureMessageConverters(converters);
//    }
//}