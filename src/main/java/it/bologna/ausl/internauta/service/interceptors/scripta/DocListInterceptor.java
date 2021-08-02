package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.DocList;
import it.bologna.ausl.model.entities.scripta.QDocList;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrControllerInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
@NextSdrInterceptor(name = "doclist-interceptor")
public class DocListInterceptor extends InternautaBaseInterceptor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocListInterceptor.class);

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    ObjectMapper objectMapper;

    @Override
    public Class getTargetEntityClass() {
        return DocList.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();
        QDocList qdoclist = QDocList.docList;
                        
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
                                    DocList.StatoDoc.CONTROLLO_SEGRETERIA.toString(),
                                    DocList.StatoDoc.PARERE.toString(),
                                    DocList.StatoDoc.FIRMA.toString()
                                })).and(initialPredicate);
                        break;
                    case VisualizzaTabIFirmato:
                        initialPredicate = buildFilterPerStruttureDelSegretario(persona).and(initialPredicate);
                        initialPredicate = qdoclist.numeroRegistrazione.isNotNull().and(initialPredicate);
                        break;
                    case VisualizzaTabRegistrazioni:
                        if (!userInfoService.isSD(user)) {
                            List<String> codiceAziendaListDoveSonoOS = userInfoService.getCodiciAziendaListDovePersonaHaRuolo(persona, Ruolo.CodiciRuolo.OS);
                            List<String> codiceAziendaListDoveSonoMOS = userInfoService.getCodiciAziendaListDovePersonaHaRuolo(persona, Ruolo.CodiciRuolo.MOS);
                            List<String> codicAziendaOSoMOS = Stream.concat(codiceAziendaListDoveSonoOS.stream(), codiceAziendaListDoveSonoMOS.stream()).collect(Collectors.toList());
                            initialPredicate = qdoclist.idAzienda.codice.in(codicAziendaOSoMOS).and(initialPredicate);
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
        manageAfterCollection(entities);
        return entities;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        List<Object> entities = new ArrayList();
        entities.add(entity);
        manageAfterCollection(entities);
        return entity;
    }
    
    /**
     * Mi ritorna il filtro per controllare che il doc sia del segretario
     * per quanto riguarda i tab ifirmario/ifirmato
     * @param persona
     * @return 
     */
    private BooleanExpression buildFilterPerStruttureDelSegretario(Persona persona) {
        QDocList qdoclist = QDocList.docList;
        Integer[] idStruttureSegretario = userInfoService.getStruttureDelSegretario(persona);
        BooleanExpression sonoSegretario = Expressions.booleanTemplate(
                String.format("FUNCTION('array_operation', '%s', '%s', {0}, '%s')= true", StringUtils.join(idStruttureSegretario, ","), "integer[]", "&&"),
                qdoclist.idStruttureFirmatari
        );
        return sonoSegretario;
    }

    /**
     * Questa funzione si occupa di generare un predicato che contenga tutti i
     * filtri di sicurezza che riguardano docList Essi sono: 1- Se demiurgo vede
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
        QDocList qdoclist = QDocList.docList;
        BooleanExpression filter = Expressions.TRUE.eq(true);

        if (!userInfoService.isSD(user)) { // Filtro 1
            String[] visLimFields = {"firmatari", "fascicolazioni", "fascicolazioniTscol"}; // Nella tscol non ci sono i firmatari quindi non serve che li aggiungo
            String[] reservedFields = {"oggetto", "oggettoTscol", "destinatari", "destinatariTscol", "tscol", "firmatari", "idPersonaRedattrice", "fascicolazioni", "fascicolazioniTscol"};
            List<String> listaCodiciAziendaUtenteAttivo = userInfoService.getAziendePersona(persona).stream().map(aziendaPersona -> aziendaPersona.getCodice()).collect(Collectors.toList());
            List<String> listaCodiciAziendaOsservatore = userInfoService.getListaCodiciAziendaOsservatore(persona);
            Integer[] idStruttureSegretario = userInfoService.getStruttureDelSegretario(persona);
//            Integer[] idStruttureSegretario = personaRepository.getStruttureDelSegretario(persona.getId());
            BooleanExpression pienaVisibilita = Expressions.booleanTemplate(
                    String.format("FUNCTION('jsonb_contains', {0}, '[{\"idPersona\": %d, \"pienaVisibilita\": true}]') = true", persona.getId()),
                    qdoclist.personeVedenti
            );
            BooleanExpression personaVedente = Expressions.booleanTemplate(
                    String.format("FUNCTION('jsonb_contains', {0}, '[{\"idPersona\": %d}]') = true", persona.getId()),
                    qdoclist.personeVedenti
            );
            BooleanExpression sonoSegretario = null;
            if (idStruttureSegretario != null && idStruttureSegretario.length > 0) {
                sonoSegretario = Expressions.booleanTemplate(
                        String.format("FUNCTION('array_operation', '%s', '%s', {0}, '%s')= true", StringUtils.join(idStruttureSegretario, ","), "integer[]", "&&"),
                        qdoclist.idStruttureFirmatari
                );
            } else {
                sonoSegretario = Expressions.FALSE.eq(true);
            }

            BooleanExpression filtroStandard = qdoclist.numeroRegistrazione.isNotNull()
                    .or(personaVedente) // Filtro 4
                    .or(sonoSegretario); // Filtro 5

            filtroStandard = filtroStandard.and(
                    qdoclist.riservato.eq(Boolean.FALSE) // Filtro 6 Riservato
                            .or(Expressions.FALSE.eq(isFilteringSpecialFields(reservedFields)))
                            .or(pienaVisibilita)
            );

            filtroStandard = filtroStandard.and(
                    qdoclist.visibilitaLimitata.eq(Boolean.FALSE) // Filtro 6 Visibilità limitata
                            .or(Expressions.FALSE.eq(isFilteringSpecialFields(visLimFields)))
                            .or(pienaVisibilita)
            );
            
            BooleanExpression filtroOsservatore = qdoclist.idAzienda.codice.in(listaCodiciAziendaOsservatore)
                    .and(qdoclist.riservato.eq(Boolean.FALSE)); // Filtro 3

            filter = qdoclist.idAzienda.codice.in(listaCodiciAziendaUtenteAttivo); // Filtro 2
            filter = filter.and(filtroOsservatore.or(filtroStandard));
        }

        return filter;
    }

    /**
     * La variabile threadlocal filterDescriptor è una mappa. Le sue chiavi sono
     * tutti i fields filtrati dal frontend. La funzione torna true se almeno
     * uno dei fields in esame è ritenuto un campo sensisbile. L'elenco dei
     * campi sensibili è passatto come parametro.
     * @param specialFields
     * @return
     */
    private Boolean isFilteringSpecialFields(String[] specialFields) {
        Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
        if (!filterDescriptorMap.isEmpty()) {
            Pattern pattern = Pattern.compile("\\.(.*?)(\\.|$)");
            Set<Path<?>> pathSet = filterDescriptorMap.keySet();
            for (Path<?> path : pathSet) {
                Matcher matcher = pattern.matcher(path.toString());
                matcher.find();
                String fieldName = matcher.group(1);
                if (Arrays.stream(specialFields).anyMatch(fieldName::equals)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Controlla se l'utente connesso ha pienaVisbilita: true nella colonna
     * personeVedenti del doc passato.
     * @param doc
     * @return
     */
    private Boolean pienaVisibilita(DocList doc, Persona persona) {
        for (DocList.PersonaVedente personaVedente : doc.getPersoneVedenti()) {
            if (personaVedente.getIdPersona() != null && personaVedente.getIdPersona().equals(persona.getId())) {
                if (personaVedente.getPienaVisibilita()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Metodo chiamato in after select. Si occupa di fare dei controlli sul
     * risultato, eventualmente nascondendo dei campi.
     * @param entities 
     */
    private void manageAfterCollection(Collection<Object> entities) {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();
        List<String> listaCodiciAziendaOsservatore = userInfoService.getListaCodiciAziendaOsservatore(persona);
        Boolean isSuperDemiurgo = userInfoService.isSD(user);
        for (Object entity : entities) {
            DocList doc = (DocList) entity;
            securityHiding(doc, persona, isSuperDemiurgo, listaCodiciAziendaOsservatore);
        }
    }

    /**
     * Metodo chiamato a seguito di una select. Se il doc è riservato e l'utente
     * connesso non è autorizzato nascondo i campi sensibili.
     * @param doc
     */
    private void securityHiding(DocList doc, Persona persona, Boolean isSuperDemiurgo, List<String> listaCodiciAziendaOsservatore) {
        if ((doc.getRiservato() || (doc.getVisibilitaLimitata() && !listaCodiciAziendaOsservatore.contains(doc.getIdAzienda().getCodice())))
                && !isSuperDemiurgo
                && !pienaVisibilita(doc, persona)) {
            
            doc.setFirmatari(null);
            doc.setFascicolazioni(null);
            doc.setFascicolazioniTscol(null);
            doc.setTscol(null);
            
            if (doc.getRiservato()) {
                doc.setOggetto("[RISERVATO]");
                doc.setOggettoTscol(null);
                doc.setDestinatari(null);
                doc.setDestinatariTscol(null);
                doc.setIdPersonaRedattrice(null);
            }
        }
    }
}
