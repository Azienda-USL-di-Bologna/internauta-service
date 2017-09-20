package it.bologna.ausl.baborg.odata.processor;


import it.bologna.ausl.baborg.odata.contex.CustomOdataJpaContextBase;
import it.bologna.ausl.baborg.odata.utils.ODataContextUtil;
import it.nextsw.olingo.edmextension.CustomProcessingEdmExtension;
import it.nextsw.olingo.processor.CustomJpaProcessorImpl;

import it.nextsw.olingo.processor.CustomOdataJpaSingleProcessor;
import org.apache.log4j.Logger;
import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.jpa.processor.api.ODataJPAContext;
import org.apache.olingo.odata2.jpa.processor.api.ODataJPAServiceFactory;
import org.apache.olingo.odata2.jpa.processor.api.access.JPAProcessor;
import org.apache.olingo.odata2.jpa.processor.api.exception.ODataJPARuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


@Component
public class JPAServiceFactory extends ODataJPAServiceFactory {

    private static final Logger logger = Logger.getLogger(JPAServiceFactory.class);

    public static final String DEFAULT_ENTITY_UNIT_NAME = "Model";
    public static final String ENTITY_MANAGER_FACTORY_ID = "entityManagerFactory";
    private static final String MAPPING_MODEL = "SalesOrderProcessingMappingModel.xml";

    // Error set
    private static final String SHOW_DETAIL_ERROR = "showDetailError";
    private static final String CONFIG = "serviceConfig";

    @Autowired
    private ApplicationContext applicationContext;
    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public ODataJPAContext initializeODataJPAContext() throws ODataJPARuntimeException {

        // prende il contesto JPA
        CustomOdataJpaContextBase oDataJPAContext = new CustomOdataJpaContextBase(getODataJPAContext().getODataContext());

        //oDataJPAContext.setSecurityContext(SecurityContextHolder.getContext());
        oDataJPAContext.setEntityManagerFactory(entityManager.getEntityManagerFactory());
        oDataJPAContext.setPersistenceUnitName(DEFAULT_ENTITY_UNIT_NAME);

        // mapping model????
        // oDataJPAContext.setJPAEdmMappingModel(MAPPING_MODEL);

        // normalmente un contesto odataJPA ha associato un oggetto di tipo JPAEdmExtension_NU standard
        // settando un nuovo oggetto di una classe custom che estende JPAEdmExtension_NU posso ottenere
        // dei comportamenti customizzati
        oDataJPAContext.setJPAEdmExtension(applicationContext.getBean(CustomProcessingEdmExtension.class));

        // CustomOdataJpaProcessor customOdataJpaProcessor =applicationContext.getBean(CustomOdataJpaProcessor.class,oDataJPAContext,getJpaProcessor(oDataJPAContext));
        // oDataJPAContext.setODataProcessor(customOdataJpaProcessor);
        setErrorLevel();
        ODataContextUtil.setODataContext(oDataJPAContext.getODataContext());

        //Questo parametro da alle proprietà di odata lo stesso nome della proprietà java
        oDataJPAContext.setDefaultNaming(false);

        return oDataJPAContext;

    }

    private JPAProcessor getJpaProcessor(ODataJPAContext oDataJPAContext) {
        JPAProcessor customJpaProcessor = applicationContext.getBean(CustomJpaProcessorImpl.class, oDataJPAContext);
        return customJpaProcessor;
    }

    @Override
    public ODataSingleProcessor createCustomODataProcessor(ODataJPAContext oDataJPAContext) {
        ODataSingleProcessor customOdataJpaProcessor = applicationContext.getBean(CustomOdataJpaSingleProcessor.class, oDataJPAContext, getJpaProcessor(oDataJPAContext));
        return customOdataJpaProcessor;
    }


    private void setErrorLevel() {
        setDetailErrors(true);
    }


    @Override
    public <T extends ODataCallback> T getCallback(final Class<T> callbackInterface) {
        if (callbackInterface.isAssignableFrom(CustomOdataDebugCallback.class))
            return (T) new CustomOdataDebugCallback();
        return null;
    }


}
