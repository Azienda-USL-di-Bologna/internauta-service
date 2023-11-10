package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView;
import it.bologna.ausl.model.entities.scripta.views.QArchivioDetailView;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
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
 * L'ArchivioDetailView viene usata quando si vogliono trovati archivi su
 * cui si ha il permesso.
 * 
 * Questo interceptor si occupa quindi solo di aggiungere il controllo di 
 * sicurezza tale per cui l'utente loggato abbia permesso sull'archivio cercato.
 * 
 * Il controllo di sicurezza non viene inserito nel caso che l'utente reale sia
 * un demiurgo.
 * 
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "archiviodetailview-interceptor")
public class ArchivioDetailViewInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchivioDetailViewInterceptor.class);
    
    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private ScriptaInterceptorUtils scriptaInterceptorUtils;

    @Override
    public Class getTargetEntityClass() {
        return ArchivioDetailView.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        QArchivioDetailView archivioDetailView = QArchivioDetailView.archivioDetailView;        
        initialPredicate = safetyFilters().and(initialPredicate);
                
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = 
                InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case FilterBitPermessoMinimo:
                        String bit = additionalData.get(InternautaConstants.AdditionalData.Keys.BitPermessoMinimo.toString());
                        BooleanExpression filter = archivioDetailView.bit.goe(Integer.parseInt(bit));
                        initialPredicate = filter.and(initialPredicate);
                        break;
                    case FilterBitGOEModifica:
                        BooleanExpression bitFilter = archivioDetailView.bit.goe(PermessoArchivio.DecimalePredicato.MODIFICA.getValue());
                        initialPredicate = bitFilter.and(initialPredicate);
                        break;
                }
            }
        }
        initialPredicate = scriptaInterceptorUtils.duplicateFiltersPerPartition(ArchivioDetailView.class, "dataCreazioneArchivio").and(initialPredicate);
        return initialPredicate;
    }

    /**
     * Questa funzione si occupa di generare il predicato di sicurezza per far 
     * si che l'utente trovi solo archivi su cui ha permesso.
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
        QArchivioDetailView archivioDetailView = QArchivioDetailView.archivioDetailView;
        
        if (!userInfoService.isSD(user)) {
            List<Integer> listaIdAziendaUtenteAttivo = userInfoService.getAziendePersona(persona).stream().map(aziendaPersona -> aziendaPersona.getId()).collect(Collectors.toList());
            BooleanExpression personaConPermesso = 
                    archivioDetailView.idPersona.id.eq(persona.getId());
//            BooleanExpression mieBozze = 
//                    archivioDetailView.stato.eq(Archivio.StatoArchivio.BOZZA.toString())
//                    .and(archivioDetailView.idPersonaCreazione.id.eq(persona.getId()));
            BooleanExpression soloMieAziende = 
                    archivioDetailView.idAzienda.id.in(listaIdAziendaUtenteAttivo);
            
            filter = filter.and(personaConPermesso);
            filter = filter.and(soloMieAziende);
        }

        return filter;
    }
}
