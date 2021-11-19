package it.bologna.ausl.internauta.service.interceptors.rubrica;

import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
//import it.bologna.ausl.internauta.service.repositories.rubrica.GruppiContattiRepository;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
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
@NextSdrInterceptor(name = "gruppi-contatti-interceptor")
public class GruppiContattiInterceptor extends InternautaBaseInterceptor{
    private static final Logger LOGGER = LoggerFactory.getLogger(GruppiContattiInterceptor.class);
    
//    @Autowired
//    GruppiContattiRepository gruppiContattiRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    KrintRubricaService krintRubricaService;
    
    @Override
    public Class getTargetEntityClass() {
        return GruppiContatti.class;
    }
//
//    @Override
//    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
//        GruppiContatti gc = (GruppiContatti) entity;
//        if (gc.getIdGruppo() == null) {
//            Object data = this.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.ContattoGruppoAppenaCreato);
//            if (data != null) {
//                gc.setIdGruppo((Contatto) data);
//            }
//        }
//        return super.beforeCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
//    }

//    @Override
//    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
//        GruppiContatti gc = (GruppiContatti) entity;
//        if(gc.getIdGruppo() != null){
//            krintRubricaService.writeGroupContactCreation(gc, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_CONTACT_CREATION);
//        }
//        return super.beforeCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
//    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        GruppiContatti gc = (GruppiContatti) entity;
        if(gc.getIdGruppo() != null){
            if (KrintUtils.doIHaveToKrint(request)) {
                krintRubricaService.writeGroupContactCreation(gc, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_CONTACT_CREATION);
//                Object data = this.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.ContattoGruppoAppenaCreato);
//                if (data == null) {
//                    
//                }
            }
        }
        
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        GruppiContatti gc = (GruppiContatti) entity;
        if(gc.getIdGruppo() != null){
            if (KrintUtils.doIHaveToKrint(request)) {
                krintRubricaService.writeGroupContactDelete(gc, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_CONTACT_DELETE);
            }
        }
        
        super.afterDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        GruppiContatti gc = (GruppiContatti) entity;
        if(gc.getIdGruppo() != null){
            if (KrintUtils.doIHaveToKrint(request)) {
                krintRubricaService.writeGroupContactUpdate(gc, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_CONTACT_UPDATE);
            }
        }
        
        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
    

