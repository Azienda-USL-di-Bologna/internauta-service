package it.bologna.ausl.internauta.service.interceptors.rubrica;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
}
    

