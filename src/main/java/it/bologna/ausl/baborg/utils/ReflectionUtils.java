package it.bologna.ausl.baborg.utils;


import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Created by f.longhitano on 21/06/2017.
 */

@Service
public class ReflectionUtils {

    private String basePackage="it.nextsw";
    //private Reflections reflections;

//    @PostConstruct
//    public void init(){
//
//    }

    public Set<Class<?>> getClassesByAnnotation (Class<? extends Annotation> annotationClass){
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(annotationClass);
        return annotated;
    }
}
