package it.bologna.ausl.internauta.service.interceptors.shpeck;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Template;
import com.querydsl.core.types.TemplateFactory;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.KrintInformazioniUtente;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.bologna.ausl.model.entities.shpeck.projections.KrintPecMessage;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

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
    
    @Autowired
    ProjectionFactory factory;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    ObjectMapper objectMapper;
    
    
    
    @Override
    public Class getTargetEntityClass() {
        return Message.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {

        try {
            Template a = TemplateFactory.DEFAULT.create("to_tsquery('italian', '$${0}:*$$') {1} tscol");
            String value = "middleware";
            String field = "tscol";
            BooleanExpression booleanTemplate = Expressions.booleanTemplate("FUNCTION('fts_match', italian, {0}, {1})= true", Expressions.stringPath(value), QMessage.message.tscol);
            super.getAuthenticatedUserProperties();
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
        } catch (Exception ex) {
            throw new AbortLoadInterceptorException(ex);
        }
    }

    // PROVA PER KRINT
    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        
        Message message = (Message) entity;
        Message mBefore = (Message) beforeUpdateEntity;
        // becco un evento a caso
        if(mBefore.getSeen() != message.getSeen()){
            
            
            AuthenticatedSessionData authenticatedUserProperties;
            try {
 
 
                Utente utente = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getUser();
                // vediamo se mi crea l'oggetto da passare al krint
                Integer idSessione = 5; // TODO: mettere idSessione corretto
                KrintInformazioniUtente krintInformazioniUtente = factory.createProjection(KrintInformazioniUtente.class, utente);
                String jsonKrintInformazioniUtente = objectMapper.writeValueAsString(krintInformazioniUtente);                
                KrintPecMessage krintPecMessage = factory.createProjection(KrintPecMessage.class, message);                                
                String jsonKrintMessage = objectMapper.writeValueAsString(krintPecMessage);
                OperazioneKrint operazioneKrint = cachedEntities.getOperazioneKrint(OperazioneKrint.CodiceOperazione.PEC_MESSAGE_RISPOSTA); // TODO: mettere codiceOperazione corretto
                
                Krint krint = new Krint(idSessione, utente.getId(), utente.getIdPersona().getDescrizione(), jsonKrintInformazioniUtente);
                
                krint.setIdOggetto(message.getId().toString());
                krint.setTipoOggetto(Krint.TipoOggettoKrint.PEC_MESSAGE);
                krint.setInformazioniOggetto(jsonKrintMessage);
                krint.setIdOperazione(operazioneKrint);

                httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.KRINT, krint);
                
                System.out.println("ciaociaociao");
                
                
            } catch (BlackBoxPermissionException ex) {
                java.util.logging.Logger.getLogger(MessageInterceptor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JsonProcessingException ex) {
                java.util.logging.Logger.getLogger(MessageInterceptor.class.getName()).log(Level.SEVERE, null, ex);
            }

            
            //super.httpSessionData
        }
        return entity;
               
    }
    
    
}
