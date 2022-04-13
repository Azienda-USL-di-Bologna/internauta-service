/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.interceptors.rubrica;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.ContattoInterface;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.bologna.ausl.model.entities.rubrica.views.ContattoConDettaglioPrincipale;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author mido
 */
@Component
public class RubricaInterceptorUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RubricaInterceptorUtils.class);

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


    public Predicate addFilterVisibilita(
                AuthenticatedSessionData authenticatedSessionData, 
                Predicate initialPredicate, 
                Class<? extends ContattoInterface> contattoClass, 
                List<Persona> personeDiCuiVedoIProtoconattiList) throws AbortLoadInterceptorException {
        LOGGER.info("authenticatedSessionData");
        LOGGER.info(authenticatedSessionData.getPerson().getDescrizione());
        LOGGER.info(authenticatedSessionData.getUser().getUsername());
        LOGGER.info("initialPredicate");
        LOGGER.info(initialPredicate.toString());
        LOGGER.info(contattoClass.getCanonicalName());
        LOGGER.info("personeDiCuiVedoIProtoconattiList");
        for (Persona persona : personeDiCuiVedoIProtoconattiList) {
            LOGGER.info(persona.getDescrizione());
            
        }
                
        Utente loggedUser = authenticatedSessionData.getUser();
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(loggedUser.getIdPersona());
        PathBuilder<ContattoInterface> contatto = getQObjectFromClass(contattoClass);
        // QUESTO E' IL FILTRO PER FAR SI CHE UNO VEDA SOLO I CONTATTI DELLE SUE AZIENDE
        BooleanExpression permessoAziendaleFilter = contatto.get("idAziende",Integer[].class).isNull().or(
                Expressions.booleanTemplate("tools.array_overlap({0}, tools.string_to_integer_array({1}, ','))=true",
                        contatto.get("idAziende",Integer[].class), org.apache.commons.lang3.StringUtils.join(aziendePersona.stream().map(a -> a.getId()).collect(Collectors.toList()), ",")
                ).or(
                        Expressions.booleanTemplate("cardinality({0}) = 0",
                                contatto.get("idAziende",Integer[].class)
                        )
                ));
        initialPredicate = permessoAziendaleFilter.and(initialPredicate);

        // QUESTO E' IL FILTRO PER FAR SI CHE UNO VEDA SOLO I CONTATTI RISERVATI SU CUI HA UN PERMESSO UTENTE
        List<Integer> idContattiRiservatiVisbili = getIdContattiRiservatiVisbili(authenticatedSessionData);
        BooleanExpression contactFilter = contatto.get("id",Integer.class).in(idContattiRiservatiVisbili)
                .or(contatto.get("idPersonaCreazione",Persona.class).getNumber("id", Integer.class).eq(loggedUser.getIdPersona().getId())
                        .or(contatto.getBoolean("riservato").eq(false)));
        initialPredicate = contactFilter.and(initialPredicate);

        // QUESTO E' IL FILTRO PER I PROTOCONTATTI. Un protocontatto lo può vedeere solo un CA/CI o le persone dentro personeDiCuiVedoIProtoconattiList
        BooleanExpression sonoCAoCI = (userInfoService.isCI(loggedUser) || userInfoService.isCA(loggedUser)) ? Expressions.TRUE : Expressions.FALSE;
        BooleanExpression protocontattoFilters = contatto.getBoolean("protocontatto").eq(false)
                .or(contatto.get("idPersonaCreazione",Persona.class).in(personeDiCuiVedoIProtoconattiList)
                        .or(sonoCAoCI.isTrue()));
        initialPredicate = protocontattoFilters.and(initialPredicate);

        return initialPredicate;
    }

    public List<Integer> getIdContattiRiservatiVisbili(AuthenticatedSessionData authenticatedSessionData) throws AbortLoadInterceptorException {

        List<PermessoEntitaStoredProcedure> contattiWithStandardPermissions;
        try {
            List<Object> struttureUtente = userInfoService.getUtenteStrutturaList(authenticatedSessionData.getUser(), true).stream().map(us -> us.getIdStruttura()).collect(Collectors.toList());
            contattiWithStandardPermissions = permissionManager.getPermissionsOfSubjectAdvanced(
                    authenticatedSessionData.getPerson(),
                    null,
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.ACCESSO.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.RUBRICA.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.CONTATTO.toString()}), true, null, null, struttureUtente, BlackBoxConstants.Direzione.PRESENTE);
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("Errore nel caricamento dei contatti accessibili dalla BlackBox", ex);
            throw new AbortLoadInterceptorException("Errore nel caricamento dei contatti accessibili dalla BlackBox", ex);
        }

        return contattiWithStandardPermissions
                .stream()
                .map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList());
    }

    public List<Persona> personeDiCuiVedoIProtocontatti(AuthenticatedSessionData authenticatedSessionData) throws AbortLoadInterceptorException {
        List<Persona> personeDiCuiVedoIProtoconattiList;
        try {
            personeDiCuiVedoIProtoconattiList = userInfoService
                    .getPersoneDiStruttureDiCuiPersonaIsSegretario(authenticatedSessionData.getPerson());
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error(ex.toString());
            throw new AbortLoadInterceptorException("Errore nel caricamento delle persone di cui si è segretario dalla BlackBox", ex);
        }

        personeDiCuiVedoIProtoconattiList.add(authenticatedSessionData.getUser().getIdPersona());
        return personeDiCuiVedoIProtoconattiList;
    }

    public Predicate addFiltriPerContattiChePossoVedere(AuthenticatedSessionData authenticatedSessionData, List<Persona> personeDiCuiVedoIProtoconattiList,Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass,Class<ContattoInterface> tipoContattoIgnoto) throws AbortLoadInterceptorException {
        // AGGIUNGO I FILTRI DI SICUREZZA PER GARANTIRE CHE L'UTENTE NON VEDA CONTATTI CHE NON PUO' VEDERE
        initialPredicate = addFilterVisibilita(
                authenticatedSessionData,
                initialPredicate,  
                tipoContattoIgnoto,  
                personeDiCuiVedoIProtoconattiList);
        PathBuilder<ContattoInterface> qContatto = getQObjectFromClass(tipoContattoIgnoto);
        // CONTOLLIAMO EVENTUALI ADDITIONAL DATA.
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    // QUESTO E' IL FILTRO CHE RIGUARDA I PROTOCONTATTI E I CONTATTI DA VERIFICARE
                    // Serve a mettere in OR i due booleani (cosa che dal front-end non si può fare))
                    case FilterContattiDaVerificareOProtocontatti:
                        LOGGER.info("Devo cercare i PROTOCONTATTI "
                                + "creati dalle persone che fanno parte "
                                + "delle strutture di cui AuthenticatedUser e' responsabile...");

                        if (personeDiCuiVedoIProtoconattiList != null && personeDiCuiVedoIProtoconattiList.size() > 0) {
                            LOGGER.info("Trovate "
                                    + personeDiCuiVedoIProtoconattiList.size()
                                    + " persone: aggiungo una AND condition "
                                    + "all' initialPredicate...");
                            BooleanExpression daVerfificareOrProtocontattoVisbile
                                    = qContatto.getBoolean("daVerificare").eq(true).or(
                                            qContatto.getBoolean("protocontatto").eq(true)
                                                    .and(qContatto.get("idPersonaCreazione",Persona.class)
                                                            .in(personeDiCuiVedoIProtoconattiList)));
                            initialPredicate = (daVerfificareOrProtocontattoVisbile).and(initialPredicate);
                        } else {
                            LOGGER.info("AuthenticatedUser non e' segretario "
                                    + "oppure non ci sono persone nelle "
                                    + "strutture di cui lui e' segretario");
                        }

                        break;
                    case CercaContattiCustomFilterPico:
                        String cercaAncheGruppiString = additionalData.get(InternautaConstants.AdditionalData.Keys.cercaAncheGruppi.toString());
                        boolean cercaAncheGruppi = false;
                        if (cercaAncheGruppiString != null) {
                            cercaAncheGruppi = Boolean.parseBoolean(cercaAncheGruppiString);
                        }
                        BooleanExpression picoCustomFilter
                                = qContatto.getString("categoria").eq(Contatto.CategoriaContatto.ESTERNO.toString()).or(
                                        ((qContatto.getString("categoria").eq(Contatto.CategoriaContatto.PERSONA.toString())
                                                .or(qContatto.getString("categoria").eq(Contatto.CategoriaContatto.STRUTTURA.toString())))
                                                .and(
                                                        qContatto.getString("tipo").eq(Contatto.TipoContatto.ORGANIGRAMMA.toString())
                                                ))
                                );
                        if (cercaAncheGruppi) {
                            picoCustomFilter = picoCustomFilter.or(qContatto.getString("categoria").eq(Contatto.CategoriaContatto.GRUPPO.toString()));
//                            picoCustomFilter = (QContatto.contatto.categoria.eq(Contatto.CategoriaContatto.GRUPPO.toString()));
                        }
                        initialPredicate = (picoCustomFilter).and(initialPredicate);
                        break;
                }
            }
        }
        LOGGER.info("query: " + initialPredicate.toString());
        return initialPredicate;
    }

    PathBuilder<ContattoInterface> getQObjectFromClass(Class<? extends ContattoInterface> targetEntityClass) {
        
        return new PathBuilder(targetEntityClass, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,targetEntityClass.getSimpleName()));
    }
}
