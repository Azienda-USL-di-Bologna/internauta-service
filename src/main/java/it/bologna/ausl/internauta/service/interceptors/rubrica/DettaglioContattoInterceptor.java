package it.bologna.ausl.internauta.service.interceptors.rubrica;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.ContattoInterface;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.QDettaglioContatto;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.List;
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
public class DettaglioContattoInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DettaglioContattoInterceptor.class);

    @Autowired
    private ContattoRepository contattoRepository;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private KrintRubricaService krintRubricaService;

    @Autowired
    private ContattoInterceptor contattoInterceptor;
    
    @Autowired
    private RubricaInterceptorUtils rubricaInterceptorUtils;
    
    @Autowired
    private KrintUtils krintUtils;

    @Override
    public Class getTargetEntityClass() {
        return DettaglioContatto.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente loggedUser = authenticatedSessionData.getUser();
        if (mainEntity) {
            List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
            String stringDaCercare;
            if (operationsRequested != null && !operationsRequested.isEmpty()) {
                for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                    switch (operationRequested) {
                        case CercaAncheInContatto:
                            LOGGER.info("sono dentro CercaAncheInContatto");
                            stringDaCercare = additionalData.get(InternautaConstants.AdditionalData.Keys.CercaAncheInContatto.toString());
                            BooleanExpression booleanTemplateDettaglioContatto = Expressions.booleanTemplate(
                                    String.format("FUNCTION('fts_match', italian, {0}, '%s')= true", stringDaCercare.replace("'", "''")),
                                    QDettaglioContatto.dettaglioContatto.tscol
                            );
                            QDettaglioContatto.dettaglioContatto.descrizione.containsIgnoreCase(stringDaCercare);

                            BooleanExpression booleanTemplateContatto = Expressions.booleanTemplate(
                                    String.format("FUNCTION('fts_match', italian, {0}, '%s')= true", stringDaCercare.replace("'", "''")),
                                    QDettaglioContatto.dettaglioContatto.idContatto.tscol
                            );

                            initialPredicate = (booleanTemplateDettaglioContatto.or(booleanTemplateContatto)).and(initialPredicate);

//                            initialPredicate = booleanTemplateContatto.and(QDettaglioContatto.dettaglioContatto.descrizione.containsIgnoreCase(stringDaCercare)).and(initialPredicate);
                            break;

                        case CercaAncheInContattoNoTScol:
                            LOGGER.info("sono dentro CercaAncheInContattoNoTScol");
                            stringDaCercare = additionalData.get(InternautaConstants.AdditionalData.Keys.CercaAncheInContattoNoTScol.toString());
                            String stringheDaCercare[] = stringDaCercare.split(" ");
                            BooleanExpression primoPredicate = QDettaglioContatto.dettaglioContatto.descrizione.containsIgnoreCase(stringheDaCercare[0]);
                            BooleanExpression secondoPredicate = QDettaglioContatto.dettaglioContatto.idContatto.descrizione.containsIgnoreCase(stringheDaCercare[0]);
                            
                            for (String string : stringheDaCercare) {
                                primoPredicate = QDettaglioContatto.dettaglioContatto.descrizione.containsIgnoreCase(string).and(primoPredicate);
                            }
                            for (String string : stringheDaCercare) {
                                secondoPredicate = QDettaglioContatto.dettaglioContatto.idContatto.descrizione.containsIgnoreCase(string).and(secondoPredicate);
                            }
                            
                            initialPredicate = ((primoPredicate).or((secondoPredicate))).and(initialPredicate);
                            break;

                    }
                }
            }
            LOGGER.info("sono fuori dallo switch");
            // Se la chiamata viene fatta direttamente sui dettagli contatti mi assicuro che siano dettagli di contatti che l'utente può vedere.           
            // AGGIUNGO I FILTRI DI SICUREZZA PER GARANTIRE CHE L'UTENTE NON VEDA CONTATTI CHE NON PUO' VEDERE
            List<Persona> personeDiCuiVedoIProtoconattiList;
            try {
                personeDiCuiVedoIProtoconattiList = userInfoService
                        .getPersoneDiStruttureDiCuiPersonaIsSegretario(getAuthenticatedUserProperties().getPerson());
            } catch (BlackBoxPermissionException ex) {
                LOGGER.error(ex.toString());
                throw new AbortLoadInterceptorException("Errore nel caricamento delle persone di cui si è segretario dalla BlackBox", ex);
            }
            personeDiCuiVedoIProtoconattiList.add(loggedUser.getIdPersona());
            initialPredicate = rubricaInterceptorUtils.addFilterVisibilita(authenticatedSessionData, initialPredicate, Contatto.class, personeDiCuiVedoIProtoconattiList);
        }

        return initialPredicate;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        DettaglioContatto dettaglioContatto = (DettaglioContatto) entity;
        if (krintUtils.doIHaveToKrint(request)) {
            if (dettaglioContatto.getIdContatto().getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactDetailCreation(dettaglioContatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_CREATION);
            }
        }
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        DettaglioContatto dettaglioContatto = (DettaglioContatto) entity;
        DettaglioContatto dettaglioContattoOld;
        try {
            dettaglioContattoOld = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, DettaglioContatto.class);

        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }
        if (krintUtils.doIHaveToKrint(request)) {
            if (dettaglioContatto.getIdContatto().getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactDetailUpdate(dettaglioContatto, dettaglioContattoOld, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_UPDATE);
            }
        }

        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        DettaglioContatto dettaglioContatto = (DettaglioContatto) entity;
        if (krintUtils.doIHaveToKrint(request)) {
            if (dettaglioContatto.getIdContatto().getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactDetailDelete(dettaglioContatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_DELETE);
            }
        }

        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        DettaglioContatto dettaglioContatto = (DettaglioContatto) entity;
        if (krintUtils.doIHaveToKrint(request)) {
            if (dettaglioContatto.getIdContatto().getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactDetailDelete(dettaglioContatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_DELETE);
            }
        }

        super.afterDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

}
