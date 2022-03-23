package it.bologna.ausl.internauta.service.interceptors.rubrica;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.rubrica.utils.similarity.SqlSimilarityResults;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "contatto-interceptor")
public class ContattoInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContattoInterceptor.class);

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

    @Override
    public Class getTargetEntityClass() {
        return Contatto.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente loggedUser = authenticatedSessionData.getUser();
        List<Persona> personeDiCuiVedoIProtoconattiList;
        try {
            personeDiCuiVedoIProtoconattiList = userInfoService
                    .getPersoneDiStruttureDiCuiPersonaIsSegretario(getAuthenticatedUserProperties().getPerson());
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error(ex.toString());
            throw new AbortLoadInterceptorException("Errore nel caricamento delle persone di cui si è segretario dalla BlackBox", ex);
        }
        personeDiCuiVedoIProtoconattiList.add(loggedUser.getIdPersona());

        // AGGIUNGO I FILTRI DI SICUREZZA PER GARANTIRE CHE L'UTENTE NON VEDA CONTATTI CHE NON PUO' VEDERE
        initialPredicate = addFilterVisibilita(initialPredicate, QContatto.contatto, personeDiCuiVedoIProtoconattiList);

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
                                    = QContatto.contatto.daVerificare.eq(true).or(
                                            QContatto.contatto.protocontatto.eq(true)
                                                    .and(QContatto.contatto.idPersonaCreazione
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
                                = QContatto.contatto.categoria.eq(Contatto.CategoriaContatto.ESTERNO.toString()).or(
                                        ((QContatto.contatto.categoria.eq(Contatto.CategoriaContatto.PERSONA.toString())
                                                .or(QContatto.contatto.categoria.eq(Contatto.CategoriaContatto.STRUTTURA.toString())))
                                                .and(
                                                        QContatto.contatto.tipo.eq(Contatto.TipoContatto.ORGANIGRAMMA.toString())
                                                ))
                                );
                        if (cercaAncheGruppi) {
                            picoCustomFilter = picoCustomFilter.or(QContatto.contatto.categoria.eq(Contatto.CategoriaContatto.GRUPPO.toString()));
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

    public List<Integer> getIdContattiRiservatiVisbili() throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
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

    public Predicate addFilterVisibilita(Predicate initialPredicate, QContatto contatto, List<Persona> personeDiCuiVedoIProtoconattiList) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente loggedUser = authenticatedSessionData.getUser();
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(loggedUser.getIdPersona());

        // QUESTO E' IL FILTRO PER FAR SI CHE UNO VEDA SOLO I CONTATTI DELLE SUE AZIENDE
        BooleanExpression permessoAziendaleFilter = contatto.idAziende.isNull().or(
                Expressions.booleanTemplate("tools.array_overlap({0}, tools.string_to_integer_array({1}, ','))=true",
                        contatto.idAziende, org.apache.commons.lang3.StringUtils.join(aziendePersona.stream().map(a -> a.getId()).collect(Collectors.toList()), ",")
                ).or(
                        Expressions.booleanTemplate("cardinality({0}) = 0",
                                contatto.idAziende
                        )
                ));
        initialPredicate = permessoAziendaleFilter.and(initialPredicate);

        // QUESTO E' IL FILTRO PER FAR SI CHE UNO VEDA SOLO I CONTATTI RISERVATI SU CUI HA UN PERMESSO UTENTE
        List<Integer> idContattiRiservatiVisbili = getIdContattiRiservatiVisbili();
        BooleanExpression contactFilter = contatto.id.in(idContattiRiservatiVisbili)
                .or(contatto.idPersonaCreazione.id.eq(loggedUser.getIdPersona().getId())
                        .or(contatto.riservato.eq(false)));
        initialPredicate = contactFilter.and(initialPredicate);

        // QUESTO E' IL FILTRO PER I PROTOCONTATTI. Un protocontatto lo può vedeere solo un CA/CI o le persone dentro personeDiCuiVedoIProtoconattiList
        BooleanExpression sonoCAoCI = (userInfoService.isCI(loggedUser) || userInfoService.isCA(loggedUser)) ? Expressions.TRUE : Expressions.FALSE;
        BooleanExpression protocontattoFilters = contatto.protocontatto.eq(false)
                .or(contatto.idPersonaCreazione.in(personeDiCuiVedoIProtoconattiList)
                        .or(sonoCAoCI.isTrue()));
        initialPredicate = protocontattoFilters.and(initialPredicate);

        return initialPredicate;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Contatto contatto = (Contatto) entity;
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
        Integer idAzienda = authenticatedUserProperties.getUser().getIdAzienda().getId();
        if (krintUtils.doIHaveToKrint(request)) {
            if (contatto.getCategoria().equals(Contatto.CategoriaContatto.GRUPPO)) {
                // TODO chiamare writeGroupCreation
                this.httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.ContattoGruppoAppenaCreato, contatto);
                krintRubricaService.writeGroupCreation(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_CREATION);
            } else if (contatto.getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                String mergeStr = "";
                if (additionalData != null && !additionalData.isEmpty()) {
                    mergeStr = additionalData.get(InternautaConstants.AdditionalData.Keys.Merge.toString());
                }
                if (!"".equals(mergeStr)) {
//                    try {
//                        Map<String,Contatto> mergeMap = objectMapper.readValue(mergeStr,HashMap.class);
//                    } catch (JsonProcessingException ex) {
//                        throw new AbortSaveInterceptorException("errore nella lettura dei dati del merge");
//                    }
                    krintRubricaService.writeContactMerge(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_MERGE, mergeStr);
                } else {
                    krintRubricaService.writeContactCreation(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_CREATION);
                }
            }
        }

        // Setto il flag da verificare a true se il contatto che sto creando ha dei simili. 
        // Anche i simili li setto da verificare dato che lo sono diventati.
        if (contatto.getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
            try {
                manageFlagDaVerificarePerCreate(contatto, idAzienda);
            } catch (JsonProcessingException ex) {
                throw new AbortSaveInterceptorException("Errore nella gestione del flag da verificare", ex);
            }
        }

        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Contatto contatto = (Contatto) entity;
        Contatto contattoOld;
        try {
            contattoOld = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, Contatto.class);

        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }
        boolean isEliminato = (contatto.getEliminato() && (contattoOld.getEliminato() == false));
        boolean isModificato = isContactModified(contatto, contattoOld);
        if (krintUtils.doIHaveToKrint(request)) {
            if (contatto.getCategoria().equals(Contatto.CategoriaContatto.GRUPPO)) {
                if (isModificato) {
                    krintRubricaService.writeGroupUpdate(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_UPDATE);
                }
                if (isEliminato) {
                    krintRubricaService.writeGroupDelete(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_DELETE);
                }
            } else if (contatto.getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                if (isModificato) {
                    krintRubricaService.writeContactUpdate(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_UPDATE);
                }
                if (isEliminato) {
                    krintRubricaService.writeContactDelete(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DELETE);
                }
            }
        }

        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean isContactModified(Contatto contatto, Contatto contattoOld) {
        if (contatto.getDescrizione() == null ? contattoOld.getDescrizione() != null : !contatto.getDescrizione().equals(contattoOld.getDescrizione())) {
            return true;
        }
        if (contatto.getNome() == null ? contattoOld.getNome() != null : !contatto.getNome().equals(contattoOld.getNome())) {
            return true;
        }
        if (contatto.getCognome() == null ? contattoOld.getCognome() != null : !contatto.getCognome().equals(contattoOld.getCognome())) {
            return true;
        }
        if (contatto.getCodiceFiscale() == null ? contattoOld.getCodiceFiscale() != null : !contatto.getCodiceFiscale().equals(contattoOld.getCodiceFiscale())) {
            return true;
        }
        if (contatto.getPartitaIva() == null ? contattoOld.getPartitaIva() != null : !contatto.getPartitaIva().equals(contattoOld.getPartitaIva())) {
            return true;
        }
        if (contatto.getRagioneSociale() == null ? contattoOld.getRagioneSociale() != null : !contatto.getRagioneSociale().equals(contattoOld.getRagioneSociale())) {
            return true;
        }
        return false;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Contatto contatto = (Contatto) entity;

        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
//      String contattoString = objectMapper.writeValueAsString(contatto);
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(authenticatedUserProperties.getPerson());
        List<Integer> collect = aziendePersona.stream().map(p -> p.getId()).collect(Collectors.toList());
//      String idAziendeStr = UtilityFunctions.getArrayString(objectMapper, collect);

        List<ParametroAziende> parameters = parametriAziende.getParameters("protocontatti", new Integer[]{authenticatedUserProperties.getUser().getIdAzienda().getId()}, new String[]{Applicazione.Applicazioni.rubrica.toString()});
//        contatto.setProtocontatto(false); // malo, sembra sbagliato
        if (parameters != null && !parameters.isEmpty() && parametriAziende.getValue(parameters.get(0), Boolean.class) == true) {
            contatto.setProtocontatto(true);
        }
        if (contatto.getIdUtenteCreazione() == null) {
            Utente one = utenteRepository.getOne(authenticatedUserProperties.getUser().getId());
            contatto.setIdUtenteCreazione(one);
        }
        if (contatto.getIdPersonaCreazione() == null) {
            Persona one = personaRepository.getOne(authenticatedUserProperties.getPerson().getId());
            contatto.setIdPersonaCreazione(one);
        }
        Integer[] idAziende = userInfoService.getAziendePersona(authenticatedUserProperties.getPerson()).stream().map(a -> a.getId()).toArray(Integer[]::new);
        if (contatto.getIdAziende() == null) {
            contatto.setIdAziende(idAziende);
        }

        // Se la descrizione è nulla provo a riempirla con un euristica.
        if (contatto.getDescrizione() == null || contatto.getDescrizione().equals("")) {
            if (contatto.getTipo().toString().equals(Contatto.TipoContatto.PERSONA_FISICA.toString())) {
                contatto.setDescrizione((contatto.getCognome() + " " + contatto.getNome()).trim());
            } else if (contatto.getTipo().toString().equals(Contatto.TipoContatto.AZIENDA.toString())) {
                contatto.setDescrizione((contatto.getRagioneSociale()).trim());
            }
        }

        return contatto;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
        Contatto contatto = (Contatto) entity;

        Contatto oldContatto;
        try {
            oldContatto = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, Contatto.class);

        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }

        Integer idAzienda = authenticatedUserProperties.getUser().getIdAzienda().getId();
        try {
            manageFlagDaVerificarePerUpdate(contatto, beforeUpdateEntityApplier, idAzienda);
        } catch (JsonProcessingException | BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("Errore nella gestione del flag da verificare", ex);
        }

        return super.beforeUpdateEntityInterceptor(entity, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    public void manageFlagDaVerificarePerUpdate(Contatto contatto, BeforeUpdateEntityApplier oldContattoApplier, Integer idAzienda) throws JsonProcessingException, BeforeUpdateEntityApplierException, AbortSaveInterceptorException {
        /*  
            PASSI DELLA GESTIONE DEL FLAG DA VERIFICARE
            Dall'oldContatto voglio prendere i contatti simili. contattiSimiliBefore.
            Dal contatto voglio prendere i contatti simili. contattiSimiliNow.
            Se contattiSimiliNow è vuoto allora il contatto non è da verificare altirementi lo è.
            Se contattiSimiliNow è pieno allora ciclo i contatti e li setto da_verificare.
                Se uno di loro è presente nella lista contattiSimiliBefore allora lo rimuovo.
            Infine guardo se ci sono similarità su contattiSimiliBefore e:
                - se hanno similarità e non sono da verificare li setto da verificare 
                - se non hanno similarità e sono da verificare li setto non da verificare
         */
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();

        String contattoString = objectMapper.writeValueAsString(contatto);

        List<String> oldContattoListTmp = new ArrayList();
        oldContattoApplier.beforeUpdateApply(oldContatto -> {
            try {
                oldContattoListTmp.add(objectMapper.writeValueAsString(oldContatto));
            } catch (JsonProcessingException ex) {
                oldContattoListTmp.add(null);
            }
        });
        String oldContattoString = oldContattoListTmp.get(0);
        if (oldContattoString == null) {
            throw new AbortSaveInterceptorException("Errore nella lettura del oldContatto");
        }
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(authenticatedUserProperties.getPerson());
        List<Integer> collect = aziendePersona.stream().map(p -> p.getId()).collect(Collectors.toList());
        String idAziendeStr = UtilityFunctions.getArrayString(objectMapper, collect);

        String res = contattoRepository.getSimilarContacts(contattoString, idAziendeStr);
        List<Contatto> contattiSimiliNow = objectMapper.readValue(res, SqlSimilarityResults.class).getContatti(SqlSimilarityResults.ContactListInclude.ALL);

        res = contattoRepository.getSimilarContacts(oldContattoString, idAziendeStr);
        List<Contatto> contattiSimiliBefore = objectMapper.readValue(res, SqlSimilarityResults.class).getContatti(SqlSimilarityResults.ContactListInclude.ALL);

        if (contattiSimiliNow.isEmpty()) {
            contatto.setDaVerificare(Boolean.FALSE);
        } else {
//            contatto.setDaVerificare(Boolean.TRUE);
            setDaVerificareByProvenienza(contatto, idAzienda);
            for (Contatto c : contattiSimiliNow) {
                Contatto cSimile = contattoRepository.getOne(c.getId());
                if (!cSimile.getDaVerificare()) {
//                    cSimile.setDaVerificare(Boolean.TRUE);
                    setDaVerificareByProvenienza(cSimile, idAzienda);
                    contattoRepository.save(cSimile);
                }
                // Rimuovo, se presente, dalla lista contattiSimiliBefore il contatto con id = c.getId()
                contattiSimiliBefore.removeIf(e -> e.getId().equals(cSimile.getId()));
            }
        }

        for (Contatto c : contattiSimiliBefore) {
            Contatto cSimileBefore = contattoRepository.getOne(c.getId());
            String cSimileBeforeString = objectMapper.writeValueAsString(cSimileBefore);
            res = contattoRepository.getSimilarContacts(cSimileBeforeString, idAziendeStr);
            SqlSimilarityResults similiTrovati = objectMapper.readValue(res, SqlSimilarityResults.class);

            /* Qui c'è un problema. Per qualche motivo sembra che la getSimilarContacts non trovi la roba della transazione.
                Per questo motivo se il contatto su cui sto lavorando non è più simile al contatto viene comunque trovato come simile.
                Cioè se contattiSimiliNow.isEmpty() == true lo stesso la getSimilarContacts su contattiSimiliBefore troverà il contatto come simile.
                Dato che da contattiSimiliBefore ho tolto tutti gli eventuali contattiSimiliNow, mi aspetto che non venga trovata come somigliazna il mio contatto.
                Per il bug detto invece viene trovata e allora gliela tolgo a mano.
             */
            similiTrovati.removeSimileById(contatto.getId());
            Integer similaritiesNumber = similiTrovati.similaritiesNumber();

            if (similaritiesNumber.equals(0) && cSimileBefore.getDaVerificare()) {
                cSimileBefore.setDaVerificare(Boolean.FALSE);
            } else if (!similaritiesNumber.equals(0) && !cSimileBefore.getDaVerificare()) {
//                cSimileBefore.setDaVerificare(Boolean.TRUE);
                setDaVerificareByProvenienza(cSimileBefore, idAzienda);

            }
            contattoRepository.save(cSimileBefore);
        }

    }

    /**
     * questa funzione setta il parametro daVerificare false per le fonti
     * esterne IPA e GAAC a prescindere da tutto. se la provenienza non è una
     * fonte esterna allora setto il parametro daVerificare a TRUE
     *
     * @param contatto
     */
    private void setDaVerificareByProvenienza(Contatto contatto, Integer idAzienda) {
        List<ParametroAziende> parameters;
        List<String> fontiEsterne = null;
        parameters = parametriAziende.getParameters("fonti_esterne", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.rubrica.toString()});
        if (parameters != null && !parameters.isEmpty()) {
            fontiEsterne = parametriAziende.getValue(parameters.get(0), new TypeReference<List<String>>() {
            });
        }

        if (fontiEsterne != null && fontiEsterne.contains(contatto.getProvenienza())) {
            contatto.setDaVerificare(Boolean.FALSE);
        } else {
            contatto.setDaVerificare(Boolean.TRUE);
        }
    }

    public void manageFlagDaVerificarePerCreate(Contatto contatto, Integer idAzienda) throws JsonProcessingException {
        /*
            Prendo i contatti simili se ce ne sono. Se ce ne sono setto da verificare true.
            Se ce ne sono, li ciclo e se non sono settati da verificare true li  setto escludendo quelli provenienti da ipa o gaac.
         */
        AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();

        String contattoString = objectMapper.writeValueAsString(contatto);
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(authenticatedUserProperties.getPerson());
        List<Integer> collect = aziendePersona.stream().map(p -> p.getId()).collect(Collectors.toList());
        String idAziendeStr = UtilityFunctions.getArrayString(objectMapper, collect);

        String res = contattoRepository.getSimilarContacts(contattoString, idAziendeStr);
        SqlSimilarityResults contattiSimiliResults = objectMapper.readValue(res, SqlSimilarityResults.class);

        if (contattiSimiliResults.similaritiesNumber() > 0) {
            List<Contatto> contattiSimili = contattiSimiliResults.getContatti(SqlSimilarityResults.ContactListInclude.ALL);
//            contatto.setDaVerificare(Boolean.TRUE);
            setDaVerificareByProvenienza(contatto, idAzienda);
            for (Contatto c : contattiSimili) {
                Contatto cSimile = contattoRepository.getOne(c.getId());
                if (!cSimile.getDaVerificare()) {
//                    cSimile.setDaVerificare(Boolean.TRUE);
                    setDaVerificareByProvenienza(cSimile, idAzienda);
                    contattoRepository.save(cSimile);
                }
            }
        }
    }
}
