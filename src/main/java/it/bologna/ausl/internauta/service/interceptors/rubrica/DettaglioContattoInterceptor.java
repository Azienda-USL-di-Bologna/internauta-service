package it.bologna.ausl.internauta.service.interceptors.rubrica;

import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "dettagliocontatto-interceptor")
public class DettaglioContattoInterceptor extends InternautaBaseInterceptor{
    private static final Logger LOGGER = LoggerFactory.getLogger(DettaglioContattoInterceptor.class);
    
    @Autowired
    ContattoRepository contattoRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    KrintRubricaService krintRubricaService;
    
    @Override
    public Class getTargetEntityClass() {
        return DettaglioContatto.class;
    }
    
    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        DettaglioContatto dettaglioContatto = (DettaglioContatto)entity;
        if (KrintUtils.doIHaveToKrint(request)) {
            if (dettaglioContatto.getIdContatto().getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactDetailCreation(dettaglioContatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_CREATION);
            }
        }
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        DettaglioContatto dettaglioContatto = (DettaglioContatto)entity;
        DettaglioContatto dettaglioContattoOld = (DettaglioContatto)beforeUpdateEntity;
        if (KrintUtils.doIHaveToKrint(request)) {
            if (dettaglioContatto.getIdContatto().getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactDetailUpdate(dettaglioContatto, dettaglioContattoOld, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_UPDATE);
            }
        }
        
        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        DettaglioContatto dettaglioContatto = (DettaglioContatto)entity;
        if (KrintUtils.doIHaveToKrint(request)) {
            if (dettaglioContatto.getIdContatto().getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactDetailDelete(dettaglioContatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_DELETE);
            }
        }
        
        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
    
    

    @Override
    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        DettaglioContatto dettaglioContatto = (DettaglioContatto)entity;
        if (KrintUtils.doIHaveToKrint(request)) {
            if (dettaglioContatto.getIdContatto().getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactDetailDelete(dettaglioContatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_DELETE);
            }
        }
        
        super.afterDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
    

