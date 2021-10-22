package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import static com.querydsl.jpa.JPAExpressions.select;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.PersonaVedente;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.scripta.QPersonaVedente;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "docdetail-interceptor")
public class DocDetailInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocDetailInterceptor.class);

    @Override
    public Class getTargetEntityClass() {
        return DocDetail.class;
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
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();
        QDocDetail qdoclist = QDocDetail.docDetail;
  
        initialPredicate = safetyFilters().and(initialPredicate);
        
        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case VisualizzaTabIFirmario:
                        initialPredicate = buildFilterPerStruttureDelSegretario(persona).and(initialPredicate);
                        initialPredicate = qdoclist.numeroRegistrazione.isNull().and(initialPredicate);
                        initialPredicate = qdoclist.annullato.isFalse().and(initialPredicate);
                        initialPredicate = qdoclist.stato.in(
                                Arrays.asList(new String[]{
                                    DocDetail.StatoDoc.CONTROLLO_SEGRETERIA.toString(),
                                    DocDetail.StatoDoc.PARERE.toString(),
                                    DocDetail.StatoDoc.FIRMA.toString()
                                })).and(initialPredicate);
                        break;
                    case VisualizzaTabIFirmato:
                        initialPredicate = buildFilterPerStruttureDelSegretario(persona).and(initialPredicate);
                        initialPredicate = qdoclist.numeroRegistrazione.isNotNull().and(initialPredicate);
                        break;
                    case VisualizzaTabRegistrazioni:
                        if (!userInfoService.isSD(user)) {
                            List<Integer> codiceAziendaListDoveSonoOS = userInfoService.getIdAziendaListDovePersonaHaRuolo(persona, Ruolo.CodiciRuolo.OS);
                            List<Integer> codiceAziendaListDoveSonoMOS = userInfoService.getIdAziendaListDovePersonaHaRuolo(persona, Ruolo.CodiciRuolo.MOS);
                            List<Integer> idAziendaOSoMOS = Stream.concat(codiceAziendaListDoveSonoOS.stream(), codiceAziendaListDoveSonoMOS.stream()).collect(Collectors.toList());
                            initialPredicate = qdoclist.idAzienda.id.in(idAziendaOSoMOS).and(initialPredicate);
                        }
                        initialPredicate = qdoclist.numeroRegistrazione.isNotNull().and(initialPredicate);
                        break;
                }
            }
        }
        
        return super.beforeSelectQueryInterceptor(initialPredicate, additionalData, request, mainEntity, projectionClass);
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        try {
            AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
            BiFunction<Object,Persona,Boolean> fnPienaVisibilita = (o, p) -> pienaVisibilita((DocDetail)o, p);
            docDetailInterceptorUtils.manageAfterCollection(entities, authenticatedSessionData, fnPienaVisibilita);
        } catch (IOException ex) {
            throw new AbortLoadInterceptorException("Errore nella generazione dell'url", ex);
        }
        return entities;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        List<Object> entities = new ArrayList();
        entities.add(entity);
        try {
            AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
            BiFunction<Object,Persona,Boolean> fnPienaVisibilita = (o, p) -> pienaVisibilita((DocDetail)o, p);
            docDetailInterceptorUtils.manageAfterCollection(entities, authenticatedSessionData, fnPienaVisibilita);
        } catch (IOException ex) {
            throw new AbortLoadInterceptorException("Errore nella generazione dell'url", ex);
        }
        return entity;
    }
    
    /**
     * Mi ritorna il filtro per controllare che il doc sia del segretario
     * per quanto riguarda i tab ifirmario/ifirmato
     * @param persona
     * @return 
     */
    private BooleanExpression buildFilterPerStruttureDelSegretario(Persona persona) {
        QDocDetail qdoclist = QDocDetail.docDetail;
        Integer[] idStruttureSegretario = userInfoService.getStruttureDelSegretario(persona);
        BooleanExpression sonoSegretario = Expressions.booleanTemplate(
                String.format("FUNCTION('array_operation', '%s', '%s', {0}, '%s')= true", StringUtils.join(idStruttureSegretario, ","), "integer[]", "&&"),
                qdoclist.idStruttureSegreteria
        );
        return sonoSegretario;
    }

    /**
     * Questa funzione si occupa di generare un predicato che contenga tutti i
     * filtri di sicurezza che riguardano docDetail Essi sono: 1- Se demiurgo vede
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
        QDocDetail qdoclist = QDocDetail.docDetail;
        BooleanExpression filter = Expressions.TRUE.eq(true);

        if (!userInfoService.isSD(user)) { // Filtro 1
            String[] visLimFields = {"firmatari", "fascicolazioni", "fascicolazioniTscol", "tscol"};
            String[] reservedFields = {"oggetto", "oggettoTscol", "destinatari", "destinatariTscol", "tscol", "firmatari", "idPersonaRedattrice", "fascicolazioni", "fascicolazioniTscol"};
            List<Integer> listaIdAziendaUtenteAttivo = userInfoService.getAziendePersona(persona).stream().map(aziendaPersona -> aziendaPersona.getId()).collect(Collectors.toList());
            List<Integer> listaIdAziendaOsservatore = userInfoService.getListaIdAziendaOsservatore(persona);
            Integer[] idStruttureSegretario = userInfoService.getStruttureDelSegretario(persona);

            QPersonaVedente qPersonaVedente = QPersonaVedente.personaVedente;
            SubQueryExpression<Long> queryPersoneVedenteConPienaVisibilita = 
                    select(qPersonaVedente.id)
                    .from(qPersonaVedente)
                    .where(
                        qPersonaVedente.idPersona.id.eq(persona.getId()),
                        qPersonaVedente.pienaVisibilita.eq(Expressions.TRUE)
                    );
            BooleanExpression pienaVisibilita = qdoclist.personeVedentiList.any().id.eq(queryPersoneVedenteConPienaVisibilita);
            BooleanExpression personaVedente = qdoclist.personeVedentiList.any().idPersona.id.eq(persona.getId());
            
            BooleanExpression sonoSegretario = null;
            if (idStruttureSegretario != null && idStruttureSegretario.length > 0) {
                sonoSegretario = Expressions.booleanTemplate(
                        String.format("FUNCTION('array_operation', '%s', '%s', {0}, '%s')= true", StringUtils.join(idStruttureSegretario, ","), "integer[]", "&&"),
                        qdoclist.idStruttureSegreteria
                );
            } else {
                sonoSegretario = Expressions.FALSE.eq(true);
            }

            BooleanExpression filtroStandard = qdoclist.numeroRegistrazione.isNotNull()
                    .or(personaVedente) // Filtro 4
                    .or(sonoSegretario); // Filtro 5

            filtroStandard = filtroStandard.and(
                    qdoclist.riservato.eq(Boolean.FALSE) // Filtro 6 Riservato
                            .or(Expressions.FALSE.eq(docDetailInterceptorUtils.isFilteringSpecialFields(reservedFields)))
                            .or(pienaVisibilita)
            );

            filtroStandard = filtroStandard.and(
                    qdoclist.visibilitaLimitata.eq(Boolean.FALSE) // Filtro 6 Visibilità limitata
                            .or(Expressions.FALSE.eq(docDetailInterceptorUtils.isFilteringSpecialFields(visLimFields)))
                            .or(pienaVisibilita)
            );
            
            BooleanExpression filtroOsservatore = qdoclist.idAzienda.id.in(listaIdAziendaOsservatore)
                    .and(qdoclist.riservato.eq(Boolean.FALSE)); // Filtro 3

            filter = qdoclist.idAzienda.id.in(listaIdAziendaUtenteAttivo); // Filtro 2
            filter = filter.and(filtroOsservatore.or(filtroStandard));
        }

        return filter;
    }


    /**
     * Controlla se l'utente connesso ha pienaVisbilita: true nella colonna
     * personeVedenti del doc passato.
     * @param doc
     * @return
     */
    private Boolean pienaVisibilita(DocDetail doc, Persona persona) {
        for (PersonaVedente personaVedente : doc.getPersoneVedentiList()) {
            if (personaVedente.getIdPersona() != null && personaVedente.getIdPersona().getId().equals(persona.getId())) {
                if (personaVedente.getPienaVisibilita()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

}
