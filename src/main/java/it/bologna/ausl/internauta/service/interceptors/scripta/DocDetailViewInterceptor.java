package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.views.DocDetailView;
import it.bologna.ausl.model.entities.scripta.views.QDocDetailView;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "docdetailview-interceptor")
public class DocDetailViewInterceptor extends InternautaBaseInterceptor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocDetailViewInterceptor.class);
    
    @Override
    public Class getTargetEntityClass() {
        return DocDetailView.class;
    }
    
    @Autowired
    UserInfoService userInfoService;

    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    InternautaUtils internautaUtils;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    DocDetailInterceptorUtils docDetailInterceptorUtils;

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        initialPredicate = safetyFilters().and(initialPredicate);
        return super.beforeSelectQueryInterceptor(initialPredicate, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        List<Object> entities = new ArrayList();
        entities.add(entity);
        try {
            AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
            BiFunction<Object,Persona,Boolean> fnPienaVisibilita = (o, p) -> pienaVisibilita((DocDetailView)o, p);
            docDetailInterceptorUtils.manageAfterCollection(entities, authenticatedSessionData, fnPienaVisibilita);
        } catch (IOException ex) {
            throw new AbortLoadInterceptorException("Errore nella generazione dell'url", ex);
        }
        return entity;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        try {
            AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
            BiFunction<Object,Persona,Boolean> fnPienaVisibilita = (o, p) -> pienaVisibilita((DocDetailView)o, p);
            docDetailInterceptorUtils.manageAfterCollection(entities, authenticatedSessionData, fnPienaVisibilita);
        } catch (IOException ex) {
            throw new AbortLoadInterceptorException("Errore nella generazione dell'url", ex);
        }
        return entities;
    }
    
    /**
     * Controlla se l'utente connesso ha pienaVisbilita: true nella colonna
     * personeVedenti del doc passato.
     * @param doc
     * @return
     */
    private Boolean pienaVisibilita(DocDetailView doc, Persona persona) {
        return doc.getIdPersona().getId().equals(persona.getId()) && doc.getPienaVisibilita();
    }

    /**
     * Questa funzione si occupa di generare un predicato che contenga tutti i
     * filtri di sicurezza che riguardano docDetailView Essi sono: 1- Se demiurgo vede
     * tutto 2- Gli altri vedono solo documenti delle aziende su cui sono attivi
     * 3- Se osservatore vede tutto delle aziende su cui è osservatore tranne i riservati 4- Se
     * utente generico vede solo le sue proposte 5- Se segretario vede anche
     * proposte non sue purché dei suoi "superiori" 6- Se utente sta cercando
     * per campi sensibili e non ha piena visibilità non vede riservati/vis lim
     */
    private BooleanExpression safetyFilters() {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();
        QDocDetailView qdocdetailview = QDocDetailView.docDetailView;
        BooleanExpression filter = Expressions.TRUE.eq(true);

        if (!userInfoService.isSD(user)) { // Filtro 1
            String[] visLimFields = {"firmatari", "fascicolazioni", "fascicolazioniTscol", "tscol"};
            String[] reservedFields = {"oggetto", "oggettoTscol", "destinatari", "destinatariTscol", "tscol", "firmatari", "idPersonaRedattrice", "fascicolazioni", "fascicolazioniTscol"};
            List<Integer> listaIdAziendaUtenteAttivo = userInfoService.getAziendePersona(persona).stream().map(aziendaPersona -> aziendaPersona.getId()).collect(Collectors.toList());
            List<Integer> listaIdAziendaOsservatore = userInfoService.getListaIdAziendaOsservatore(persona);
            Integer[] idStruttureSegretario = userInfoService.getStruttureDelSegretario(persona);
            BooleanExpression pienaVisibilita = qdocdetailview.idPersona.id.eq(persona.getId()).and(qdocdetailview.pienaVisibilita.eq(Expressions.TRUE));
            BooleanExpression personaVedente = qdocdetailview.idPersona.id.eq(persona.getId());
            BooleanExpression sonoSegretario = null;
            if (idStruttureSegretario != null && idStruttureSegretario.length > 0) {
                sonoSegretario = Expressions.booleanTemplate(
                        String.format("FUNCTION('array_operation', '%s', '%s', {0}, '%s')= true", StringUtils.join(idStruttureSegretario, ","), "integer[]", "&&"),
                        qdocdetailview.idStruttureSegreteria
                );
            } else {
                sonoSegretario = Expressions.FALSE.eq(true);
            }

            BooleanExpression filtroStandard = qdocdetailview.numeroRegistrazione.isNotNull()
                    .or(personaVedente) // Filtro 4
                    .or(sonoSegretario); // Filtro 5

            filtroStandard = filtroStandard.and(
                    qdocdetailview.riservato.eq(Boolean.FALSE) // Filtro 6 Riservato
                            .or(Expressions.FALSE.eq(docDetailInterceptorUtils.isFilteringSpecialFields(reservedFields)))
                            .or(pienaVisibilita)
            );

            filtroStandard = filtroStandard.and(
                    qdocdetailview.visibilitaLimitata.eq(Boolean.FALSE) // Filtro 6 Visibilità limitata
                            .or(Expressions.FALSE.eq(docDetailInterceptorUtils.isFilteringSpecialFields(visLimFields)))
                            .or(pienaVisibilita)
            );
            
            BooleanExpression filtroOsservatore = qdocdetailview.idAzienda.id.in(listaIdAziendaOsservatore)
                    .and(qdocdetailview.riservato.eq(Boolean.FALSE)); // Filtro 3

            filter = qdocdetailview.idAzienda.id.in(listaIdAziendaUtenteAttivo); // Filtro 2
            filter = filter.and(filtroOsservatore.or(filtroStandard));
        }

        return filter;
    }
}
