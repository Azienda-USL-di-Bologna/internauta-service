package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import static com.querydsl.jpa.JPAExpressions.select;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
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
import org.springframework.stereotype.Component;

/**
 * La visibilità degli archivi è più semplice della visibilità dei documenti.
 * Non abbiamo qui permessi di "non piena visibilità".
 * Anche il cosiddetto "permesso di transito" nell'archivio permette di vedere
 * tutto di quell'archivio, il blocco sta solo nel contenuto dello stesso.
 * 
 * L'ArchivioDetail viene usata quando si vogliono trovare tutti gli archivi,
 * neri compresi. Gli archivi neri sono sempre trovabili a meno che non siano
 * riservati.
 * 
 * Questo interceptor si occupa quindi di:
 * 1- Aggiungere il controllo di sicurezza tale per cui l'utente loggato abbia 
 * permesso sull'archivio cercato qualora quest'ultimo sia riservato.
 * 2- Settare, nell'after select, la proprietà transient forbidden che indica 
 * l'assenza di permessi su quel fascicolo.
 * 
 * Il controllo di sicurezza non viene inserito nel caso che l'utente reale sia
 * un demiurgo. 
 * 
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "archiviodetail-interceptor")
public class ArchivioDetailInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchivioDetailInterceptor.class);
    
    @Autowired
    UserInfoService userInfoService;

    @Override
    public Class getTargetEntityClass() {
        return ArchivioDetail.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        initialPredicate = safetyFilters().and(initialPredicate);
        return initialPredicate;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return super.afterSelectQueryInterceptor(entities, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return super.afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Questa funzione si occupa di generare il predicato di sicurezza per far 
     * si che l'utente trovi solo archivi non riservati o riservati 
     * su cui ha permesso.
     * Inoltre, non voglio vedere le bozze se non sono mie.
     * Inoltre, voglio vedere solo archivi della mia azienda.
     * 
     * Se sono demiurgo non servono filtri si sicurezza.
     */
    private BooleanExpression safetyFilters() {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Utente realUser = authenticatedSessionData.getRealUser();
        Persona persona = user.getIdPersona();
        BooleanExpression filter = Expressions.TRUE.eq(true);
        QArchivioDetail archivioDetail = QArchivioDetail.archivioDetail;
        QPermessoArchivio permessoArchivio = QPermessoArchivio.permessoArchivio;
        
        if (!userInfoService.isSD(user)) {
            List<Integer> listaIdAziendaUtenteAttivo = userInfoService.getAziendePersona(persona).stream().map(aziendaPersona -> aziendaPersona.getId()).collect(Collectors.toList());
            
            SubQueryExpression<Long> queryPersonaConPermesso = 
                    select(permessoArchivio.id)
                    .from(permessoArchivio)
                    .where(
                        permessoArchivio.idPersona.id.eq(persona.getId()),
                        archivioDetail.id.eq(permessoArchivio.idArchivioDetail.id),
                        archivioDetail.idAzienda.id.eq(permessoArchivio.idAzienda.id),
                        archivioDetail.dataCreazione.eq(permessoArchivio.dataCreazione)
                    );
            BooleanExpression personaConPermesso = 
                    archivioDetail.riservato.eq(Expressions.FALSE)
                    .or(archivioDetail.permessiArchivioList.any().id.eq(queryPersonaConPermesso));
//            BooleanExpression mieBozze = 
//                    archivioDetail.stato.eq(Archivio.StatoArchivio.BOZZA.toString())
//                    .and(archivioDetail.idPersonaCreazione.id.eq(persona.getId()));
            BooleanExpression soloMieAziende = 
                    archivioDetail.idAzienda.id.in(listaIdAziendaUtenteAttivo);

            filter = filter.and(personaConPermesso);
            filter = filter.and(soloMieAziende);
        }

        return filter;
    }
}
