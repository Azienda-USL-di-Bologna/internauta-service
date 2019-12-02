package it.bologna.ausl.internauta.service.interceptors.rubrica;

import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.rubrica.GruppiContattiRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
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
    
    @Autowired
    GruppiContattiRepository gruppiContattiRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Override
    public Class getTargetEntityClass() {
        return GruppiContatti.class;
    }

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
    
    
}
    
