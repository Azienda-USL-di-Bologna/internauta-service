package it.bologna.ausl.internauta.service.interceptors.shpeck;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Template;
import com.querydsl.core.types.TemplateExpression;
import com.querydsl.core.types.TemplateFactory;
import com.querydsl.core.types.Templates;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanOperation;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.StringTemplate;
import edu.emory.mathcs.backport.java.util.Arrays;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.*;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Ambiti;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Predicati;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Tipi;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it> with GDM and Gus collaboration
 */
@Component
@NextSdrInterceptor(name = "message-interceptor")
public class MessageInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageInterceptor.class);
    
    @Autowired
    PermissionManager permissionManager;
    
    @Override
    public Class getTargetEntityClass() {
        return Message.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity) throws AbortLoadInterceptorException {

        Template a = TemplateFactory.DEFAULT.create("to_tsquery('italian', '$${0}:*$$') {1} tscol");
        String value = "middleware";
        String field = "tscol";
        BooleanExpression booleanTemplate = Expressions.booleanTemplate("FUNCTION('fts_match', italian, {0}, {1})= true", Expressions.stringPath(value), QMessage.message.tscol); 
        return initialPredicate;
        
//        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
//        if (operationsRequested != null && !operationsRequested.isEmpty()) {
//            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
//                switch (operationRequested) {
//                    case GetPermessiGestoriPec: 
//                        /* Nel caso di GetPermessiGestoriPec in Data avremo l'id della PEC della quale si chiedono i permessi */
//                        String idPec = additionalData.get(AdditionalData.Keys.idPec.toString());
//                        Pec pec = new Pec(Integer.parseInt(idPec));
//                        String idAzienda = additionalData.get(AdditionalData.Keys.idAzienda.toString());
//                        if(StringUtils.hasText(idAzienda)){                           
//                            BooleanExpression aziendaFilter = QPersona.persona.utenteList.any().idAzienda.id.eq(Integer.parseInt(idAzienda));
//                            initialPredicate = aziendaFilter.and(initialPredicate);
//                        }
//                        try {
//                            List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(
//                                Arrays.asList(new Pec[]{pec}),
//                                Arrays.asList(new String[]{Predicati.ELIMINA.toString(), Predicati.LEGGE.toString(), Predicati.RISPONDE.toString()}),
//                                Arrays.asList(new String[]{Ambiti.PECG.toString()}),
//                                Arrays.asList(new String[]{Tipi.PEC.toString()}), false);
//                            if (subjectsWithPermissionsOnObject == null){
//                                initialPredicate = Expressions.FALSE.eq(true);
//                            }
//                            else {
//                                BooleanExpression permessoFilter = QPersona.persona.id.in(
//                                    subjectsWithPermissionsOnObject
//                                        .stream()
//                                        .map(p -> p.getSoggetto().getIdProvenienza()).collect(Collectors.toList()))
//                                        .and(initialPredicate);
//                                initialPredicate = permessoFilter.and(initialPredicate);
//                            }                            
//                            /* Conserviamo i dati estratti dalla BlackBox */
//                            this.httpSessionData.putData(HttpSessionData.Keys.PersoneWithPecPermissions, subjectsWithPermissionsOnObject);
//                        } catch (BlackBoxPermissionException ex) {
//                            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
//                            throw new AbortLoadInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
//                        }
//                        break;
//                }
//            }
//        }
    }
}