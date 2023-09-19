package it.bologna.ausl.internauta.service.controllers.scripta;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.logs.MassiveActionLogRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.logs.MassiveActionLog;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ScriptaGestioneAbilitazioniMassiveArchiviUtils {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ScriptaGestioneAbilitazioniMassiveArchiviUtils.class);
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @PersistenceContext
    private EntityManager em;
    
    @Autowired
    private PersonaRepository personaRepository;
    
    @Autowired
    private ApplicazioneRepository applicazioneRepository;
    
    @Autowired
    private MassiveActionLogRepository massiveActionLogRepository;
    
    public Integer[] getFilteredIdsArchivi(Integer idAzienda, Predicate predicate, Integer[] ids, Integer[] notIds) {
        // Preparo gli ids archivi su cui andremo ad agire. Li filtro per livello 1 e idAzienda
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
        QArchivioDetail qArchivioDetail = QArchivioDetail.archivioDetail;
        BooleanExpression aziendaCorretta = qArchivioDetail.idAzienda.id.eq(idAzienda);
        BooleanExpression livelloUno = qArchivioDetail.livello.eq(1);
        BooleanExpression noBozze = qArchivioDetail.stato.ne(Archivio.StatoArchivio.BOZZA.toString());
        Integer[] idsArchivi;
        
        if (ids == null || ids.length == 0) {
            // Caso in cui gli ids li devo ricavare dal predicato
            BooleanExpression notTheseArchivi = Expressions.asBoolean(true).isTrue();
            if (notIds != null) {
                notTheseArchivi = qArchivioDetail.id.notIn(notIds);
            }
            
            List<Integer> idsCalcolati = jPAQueryFactory
                    .select(qArchivioDetail.id)
                    .from(qArchivioDetail)
                    .where(aziendaCorretta.and(livelloUno).and(noBozze).and(notTheseArchivi).and(predicate))
                    .fetch();
            idsArchivi = idsCalcolati.toArray(new Integer[idsCalcolati.size()]);
        } else {
            // Caso in cui gli ids mi sono già stati passati, li filtro per livello 1 e idAzienda per sicurezza
            List<Integer> idsCalcolati = jPAQueryFactory
                    .select(qArchivioDetail.id)
                    .from(qArchivioDetail)
                    .where(aziendaCorretta.and(livelloUno).and(noBozze).and(qArchivioDetail.id.in(ids)))
                    .fetch();
            idsArchivi = idsCalcolati.toArray(new Integer[idsCalcolati.size()]);
        }
        return idsArchivi;
    }
    
    public Integer writeMassiveActionLog(
            Integer[] idsArchivi,
            Map<String, Object> parameters,
            MassiveActionLog.OperationType operationType
    ) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = personaRepository.findById(authenticatedUserProperties.getPerson().getId()).get();
        Applicazione app = applicazioneRepository.findById(Applicazione.Applicazioni.scripta.name()).get();
        
        MassiveActionLog massiveActionLog = new MassiveActionLog();
        massiveActionLog.setIdApp(app);
        massiveActionLog.setIdExecutingPerson(persona);
        massiveActionLog.setIdObjectsInvolved(idsArchivi);
        massiveActionLog.setInsertionDate(ZonedDateTime.now());
        massiveActionLog.setOperationType(operationType);
        massiveActionLog.setParameters(parameters);
        // log.setAdditionalData(null); TODO: mettere qui eventuali altre cose..
        MassiveActionLog m = massiveActionLogRepository.save(massiveActionLog);
        massiveActionLogRepository.flush();
        
        return m.getId();
    }
}
