package it.bologna.ausl.baborg.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

/**
 * Permette di ottenere i Bean di spring nelle classi non instanziate da Spring
 *
 * @author Naresh Joshi
 */
@Service
public class SpringBeanUtil implements ApplicationEventPublisherAware {

    private static ApplicationContext context;

    //interface ApplicationContextAware
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        context = applicationContext;
//    }

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }


    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        context= (ApplicationContext) applicationEventPublisher;
    }
}