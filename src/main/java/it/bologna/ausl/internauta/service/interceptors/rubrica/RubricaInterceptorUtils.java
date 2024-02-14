/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.interceptors.rubrica;

import com.google.common.base.CaseFormat;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadException;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadManager;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.ContattoInterface;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private UserInfoService userInfoService;

    @Autowired
    private PermissionManager permissionManager;
    
    @Autowired
    private InadManager inadManager;

    public Predicate addFilterVisibilita(
                AuthenticatedSessionData authenticatedSessionData, 
                Predicate initialPredicate, 
                Class<? extends ContattoInterface> contattoClass,
                Boolean isDettaglioContatto,
                List<Persona> personeDiCuiVedoIProtoconattiList) throws AbortLoadInterceptorException {
        LOGGER.info("scattato interceptor addFilterVisibilita");
        
        Utente loggedUser = authenticatedSessionData.getUser();
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(loggedUser.getIdPersona());
        PathBuilder<?> contatto;
        // QUESTO E' IL FILTRO PER FAR SI CHE UNO VEDA SOLO I CONTATTI DELLE SUE AZIENDE
        if (isDettaglioContatto) {
            contatto = getQObjectFromClass(DettaglioContatto.class);    
            contatto = contatto.get("idContatto");
        } else {
            contatto = getQObjectFromClass(contattoClass);
        }
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
        LOGGER.info("scattato interceptor getIdContattiRiservatiVisbili");
        List<PermessoEntitaStoredProcedure> contattiWithStandardPermissions;
        try {
            List<Object> struttureUtente = userInfoService.getUtenteStrutturaList(authenticatedSessionData.getUser(), true).stream().map(us -> us.getIdStruttura()).collect(Collectors.toList());
            contattiWithStandardPermissions = permissionManager.getPermissionsOfSubjectAdvanced(
                    authenticatedSessionData.getPerson(),
                    null,
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.ACCESSO.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.RUBRICA.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.CONTATTO.toString()}), 
                    true, 
                    null, 
                    null, 
                    struttureUtente, 
                    BlackBoxConstants.Direzione.PRESENTE);
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("Errore nel caricamento dei contatti accessibili dalla BlackBox", ex);
            throw new AbortLoadInterceptorException("Errore nel caricamento dei contatti accessibili dalla BlackBox", ex);
        }

        return contattiWithStandardPermissions
                .stream()
                .map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList());
    }

    public List<Persona> personeDiCuiVedoIProtocontatti(AuthenticatedSessionData authenticatedSessionData) throws AbortLoadInterceptorException {
        LOGGER.info("scattato interceptor personeDiCuiVedoIProtocontatti");
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

        // AGGIUNGO I FILTRI DI SICUREZZA PER GARANTIRE CHE L'UTENTE NON VEDA CONTATTI CHE NON PUO' VEDERE
    public Predicate addFiltriPerContattiChePossoVedere(AuthenticatedSessionData authenticatedSessionData, List<Persona> personeDiCuiVedoIProtoconattiList,Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass,Class<ContattoInterface> tipoContattoIgnoto) throws AbortLoadInterceptorException {
        LOGGER.info("scattato interceptor addFiltriPerContattiChePossoVedere");
        initialPredicate = addFilterVisibilita(
                authenticatedSessionData,
                initialPredicate,  
                tipoContattoIgnoto,
                false,
                personeDiCuiVedoIProtoconattiList);
        PathBuilder<?> qContatto = getQObjectFromClass(tipoContattoIgnoto);
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
        //LOGGER.info("query: " + initialPredicate.toString());
        return initialPredicate;
    }

    PathBuilder<?> getQObjectFromClass(Class<?> targetEntityClass) {
        
        return new PathBuilder(targetEntityClass, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,targetEntityClass.getSimpleName()));
    }
    
    
    public List<Contatto> getContattiConDDdelGruppo(Contatto idGruppo) {
        List<Contatto> contattiDaTornare = new ArrayList<>(); 
        List<GruppiContatti> contattiDelGruppoList = idGruppo.getContattiDelGruppoList();
        if (contattiDelGruppoList != null) {
            for (GruppiContatti gruppiContatti : contattiDelGruppoList) {
                List<DettaglioContatto> dettaglioContattoList = gruppiContatti.getIdContatto().getDettaglioContattoList();
                for (DettaglioContatto dettaglioContatto : dettaglioContattoList) {
                    if (dettaglioContatto.getDomicilioDigitale()){
                        contattiDaTornare.add(gruppiContatti.getIdContatto());
                    }
                }
            }
        }
    return contattiDaTornare;
    }
    
    /**
     * funzione che dato un contatto di tipo gruppo cicla tutti i contatti e i 
     * dettagli contenuti e inserisce dove possibile i domicili digitali
     * @param contatto di tipo gruppo
     * @return contatto contatto modificato con campo transient valorizzato
     */
    public Contatto setDomiciliDigitaliInGruppo(Contatto contatto) throws AuthorizationUtilsException, AuthorizationUtilsException, AuthorizationUtilsException, InadException{
        if (contatto.getCategoria().equals(Contatto.CategoriaContatto.GRUPPO)){
            List<GruppiContatti> contattiDelGruppoList = contatto.getContattiDelGruppoList();
            List<Contatto> contattiConDomiciliDigitaliModificati = new ArrayList();
            for (GruppiContatti gruppoContatto : contattiDelGruppoList) {
                try {
                    Contatto idContatto = gruppoContatto.getIdContatto();
                    Email emailDomicilioDigitale = inadManager.getAlwaysAndSaveDomicilioDigitale(idContatto.getId());
                    if (emailDomicilioDigitale != null){
                        if (!Objects.equals(gruppoContatto.getIdDettaglioContatto().getId(), 
                                emailDomicilioDigitale.getIdDettaglioContatto().getId()) &&
                            !gruppoContatto.getIdDettaglioContatto().getDescrizione()
                                    .equals(emailDomicilioDigitale.getIdDettaglioContatto().getDescrizione())
                           ){
                            gruppoContatto.setIdDettaglioContatto(emailDomicilioDigitale.getIdDettaglioContatto());
                            contattiConDomiciliDigitaliModificati.add(emailDomicilioDigitale.getIdContatto());
                        }
                    }
                } catch (BlackBoxPermissionException ex) {
                    LOGGER.error("errore nella getAlwaysAndSaveDomicilioDigitale", ex);
                    return null;
                }
            }
            contatto.setContattiConDomiciliDigitaliModificati(contattiConDomiciliDigitaliModificati);
        }
        return contatto;
    }
    
}
