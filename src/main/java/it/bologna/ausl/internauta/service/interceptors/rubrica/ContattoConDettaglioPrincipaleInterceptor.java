/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.interceptors.rubrica;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.ContattoInterface;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.bologna.ausl.model.entities.rubrica.views.ContattoConDettaglioPrincipale;
import it.bologna.ausl.model.entities.rubrica.views.QContattoConDettaglioPrincipale;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
@NextSdrInterceptor(name = "contattocondettaglioprincipale-interceptor")
public class ContattoConDettaglioPrincipaleInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContattoConDettaglioPrincipaleInterceptor.class);
    
    @Autowired
    private ContattoRepository contattoRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private KrintRubricaService krintRubricaService;

    @Autowired
    private PermissionManager permissionManager;

    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Autowired
    private RubricaInterceptorUtils rubricaInterceptorUtils;

    @Override
    public Class getTargetEntityClass() {
        return ContattoConDettaglioPrincipale.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        List<Persona> personeDiCuiVedoIProtoconattiList = rubricaInterceptorUtils.personeDiCuiVedoIProtocontatti(authenticatedSessionData) ;
        //PathBuilder<ContattoInterface> qContattoConDettaglioPrincipale = rubricaInterceptorUtils.getQObjectFromClass(getTargetEntityClass());
//        try {
//            Path<?> a = QContattoConDettaglioPrincipale.contattoConDettaglioPrincipale; 
//            Class<ContattoInterface> forName = (Class<ContattoInterface>) Class.forName(ContattoConDettaglioPrincipale.class.getName());
//            PathBuilder<ContattoInterface> qContatto = new PathBuilder(forName, "minchia");
////            new PathBuilder(a.getAnnotatedElement().getClass(),a.getAnnotatedElement().getClass().getName());
//            BooleanExpression eq = qContatto.get("daVerificare", Boolean.class).eq(true);
//            if(true){return eq.and(initialPredicate);}
//        } catch (ClassNotFoundException ex) {
//            
//        }
        return rubricaInterceptorUtils.addFiltriPerContattiChePossoVedere(authenticatedSessionData, personeDiCuiVedoIProtoconattiList,initialPredicate,additionalData,request,mainEntity,projectionClass,getTargetEntityClass());
    }
    
    
}
