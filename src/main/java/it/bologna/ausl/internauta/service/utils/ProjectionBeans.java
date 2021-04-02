package it.bologna.ausl.internauta.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.repositories.EntitaRepository;
import it.bologna.ausl.blackbox.repositories.TipoEntitaRepository;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteLogin;
import it.bologna.ausl.model.entities.baborg.projections.RibaltoneDaLanciareCustom;
import it.bologna.ausl.model.entities.baborg.projections.UtenteStrutturaWithIdAfferenzaStrutturaCustom;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecAziendaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.configuration.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configuration.projections.generated.ImpostazioniApplicazioniWithPlainFields;
import it.bologna.ausl.model.entities.scrivania.projections.generated.AttivitaWithIdPersona;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageAddress;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageAddressWithIdAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageFolderWithIdFolder;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageTagWithIdTag;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;
import it.bologna.ausl.model.entities.baborg.projections.CustomPersonaLogin;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.baborg.AziendaInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StoricoRelazioneRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.permessi.PredicatoAmbitoRepository;
import it.bologna.ausl.internauta.service.repositories.permessi.PredicatoRepository;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteStrutturaWithIdStrutturaAndIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.PersonaWithUtentiAndStruttureAndAfferenzeCustom;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import java.util.HashMap;
import java.util.Map;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckPec;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.rubrica.Indirizzo;
import it.bologna.ausl.model.entities.rubrica.Telefono;
import it.bologna.ausl.model.entities.rubrica.projections.generated.EmailWithIdDettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdDettaglioContattoAndIdGruppo;
import it.bologna.ausl.model.entities.rubrica.projections.generated.IndirizzoWithIdDettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.TelefonoWithIdDettaglioContatto;
import java.util.ArrayList;
import org.slf4j.Logger;
import it.bologna.ausl.model.entities.baborg.projections.UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom;
import it.bologna.ausl.model.entities.baborg.projections.StrutturaWithUtentiResponsabiliCustom;
import it.bologna.ausl.model.entities.baborg.projections.UtenteWithIdPersonaAndPermessiCustom;
import it.bologna.ausl.model.entities.baborg.projections.UtenteWithStruttureAndResponsabiliCustom;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithAttributiStrutturaAndIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaContatto;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaDettaglioContatto;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaGruppoContatto;
import it.bologna.ausl.model.entities.permessi.Entita;
import it.bologna.ausl.model.entities.permessi.Permesso;
import it.bologna.ausl.model.entities.permessi.PredicatoAmbito;
import it.bologna.ausl.model.entities.permessi.QEntita;
import it.bologna.ausl.model.entities.permessi.QTipoEntita;
import it.bologna.ausl.model.entities.permessi.TipoEntita;
import it.bologna.ausl.model.entities.permessi.projections.PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded;
import it.bologna.ausl.model.entities.permessi.projections.generated.EntitaWithPlainFields;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.CustomContattoWithIdStrutturaAndIdPersona;
import it.bologna.ausl.model.entities.rubrica.projections.CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda;
import it.bologna.ausl.model.entities.rubrica.projections.CustomGruppiContattiWithIdContattoAndIdDettaglioContatto;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.scripta.projections.CustomRelatedWithSpedizioneList;
import it.bologna.ausl.model.entities.scripta.projections.generated.RelatedWithSpedizioneList;
import it.bologna.ausl.model.entities.scripta.projections.generated.SpedizioneWithIdMezzo;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.persistence.Table;
import org.json.JSONArray;
import org.springframework.cache.annotation.Cacheable;

/**
 *
 * @author guido
 */
@Component
public class ProjectionBeans {

    public Object o = new Object();

    @Autowired
    protected ProjectionFactory factory;

    @Autowired
    protected CachedEntities cachedEntities;

    @Autowired
    protected EntitaRepository entitaRepository;

    @Autowired
    protected TipoEntitaRepository tipoEntitaRepository;

    @Autowired
    protected ImpostazioniApplicazioniRepository impostazioniApplicazioniRepository;

    @Autowired
    protected AziendaRepository aziendaRepository;

    @Autowired
    protected UtenteRepository utenteRepository;

    @Autowired
    protected StrutturaRepository strutturaRepository;

    @Autowired
    protected UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    protected PersonaRepository personaRepository;

    @Autowired
    protected StoricoRelazioneRepository storicoRelazioneRepository;

    @Autowired
    protected PredicatoAmbitoRepository predicatoAmbitoRepository;

    @Autowired
    protected PredicatoRepository predicatoRepository;

    @Autowired
    ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    InternautaUtils internautaUtils;

    @Autowired
    HttpSessionData httpSessionData;

    @Autowired
    AziendaInterceptor aziendaInterceptor;

    @Autowired
    ParametriAziende parametriAziende;

    @Autowired
    AdditionalDataParamsExtractor additionalDataParamsExtractor;

    @Autowired
    ObjectMapper objectMapper;

//    protected Utente user, realUser;
//    protected Persona person, realPerson;
//    protected Applicazione.Applicazioni applicazione;
//    protected int idSessionLog;
    final String APP_URL_PICO = "/Procton/Procton.htm";
    final String APP_URL_BABEL = "/Babel/Babel.htm";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectionBeans.class);

//    protected void setAuthenticatedUserProperties() throws BlackBoxPermissionException {
//        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
//        user = authenticatedSessionData.getUser();
//        realUser = authenticatedSessionData.getRealUser();
//        idSessionLog = authenticatedSessionData.getIdSessionLog();
//        person = authenticatedSessionData.getPerson();
//        realPerson = authenticatedSessionData.getRealPerson();
//        applicazione = authenticatedSessionData.getApplicazione();
//    }
//    public UtenteWithIdPersona getUtenteConPersona(Utente utente) {
//        if (utente != null) {
//            return factory.createProjection(UtenteWithIdPersona.class, utente);
//        } else {
//            return null;
//        }
//    }
    public UtenteStrutturaWithIdAfferenzaStrutturaCustom
            getUtenteStrutturaWithIdAfferenzaStrutturaCustom(UtenteStruttura utenteStruttura) {
        return factory.createProjection(UtenteStrutturaWithIdAfferenzaStrutturaCustom.class, utenteStruttura);
    }

//    public StrutturaWithAttributiStrutturaAndIdAzienda getStrutturaConAzienda(Struttura struttura) {
//        return factory.createProjection(StrutturaWithAttributiStrutturaAndIdAzienda.class, struttura);
//    }
    public StrutturaWithAttributiStrutturaAndIdAzienda getStrutturaWithIdAzienda(Contatto contatto) {
        StrutturaWithAttributiStrutturaAndIdAzienda res = null;
        Struttura struttura = contatto.getIdStruttura();
        if (struttura != null) {
            res = factory.createProjection(StrutturaWithAttributiStrutturaAndIdAzienda.class, struttura);
        }
        return res;
    }

    public CustomUtenteStrutturaWithIdStrutturaAndIdAzienda getUtenteStrutturaWithIdStrutturaAndIdAzienda(DettaglioContatto dettaglioContatto) {
        CustomUtenteStrutturaWithIdStrutturaAndIdAzienda res = null;
        UtenteStruttura utenteStruttura = dettaglioContatto.getUtenteStruttura();
        if (utenteStruttura != null) {
            res = factory.createProjection(CustomUtenteStrutturaWithIdStrutturaAndIdAzienda.class, utenteStruttura);
        }
        return res;
    }

    public List<AttivitaWithIdPersona> getAttivitaWithIdPersona(Azienda azienda) {
        return azienda.getAttivitaList().stream().map(
                a -> {
                    return factory.createProjection(AttivitaWithIdPersona.class, a);
                }
        ).collect(Collectors.toList());
    }

    public CustomUtenteLogin getUtenteRealeWithIdPersonaImpostazioniApplicazioniList(Utente utente) {
        //Utente refreshedUtente = utenteRepository.getOne(utente.getId());
//        Persona persona = utente.getIdPersona();
        if (utente.getUtenteReale() != null) {
            return factory.createProjection(CustomUtenteLogin.class, utente.getUtenteReale());
        } else {
            return null;
        }
//
//            if (impostazioniApplicazioniList != null && !impostazioniApplicazioniList.isEmpty()) {
//            return impostazioniApplicazioniList.stream().map(
//                        imp -> factory.createProjection(ImpostazioniApplicazioniWithPlainFields.class, imp)
//                    ).collect(Collectors.toList());
//        } else
//            return null;
    }

    public CustomPersonaLogin getIdPersonaWithImpostazioniApplicazioniList(Utente utente) {
        return factory.createProjection(CustomPersonaLogin.class, utente.getIdPersona());
    }

    public AziendaWithPlainFields getAziendaWithPlainFields(Utente utente) {
        return factory.createProjection(AziendaWithPlainFields.class, utente.getIdAzienda());
    }

    public List<ImpostazioniApplicazioniWithPlainFields> getImpostazioniApplicazioniListWithPlainFields(Persona persona) throws BlackBoxPermissionException {
//        setAuthenticatedUserProperties();
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Applicazione.Applicazioni applicazione = authenticatedSessionData.getApplicazione();
        List<ImpostazioniApplicazioni> impostazioniApplicazioniList = persona.getImpostazioniApplicazioniList();
        if (impostazioniApplicazioniList != null && !impostazioniApplicazioniList.isEmpty()) {
            return impostazioniApplicazioniList.stream().filter(imp -> imp.getIdApplicazione().getId().equals(applicazione.toString())).
                    map(
                            imp -> factory.createProjection(ImpostazioniApplicazioniWithPlainFields.class, imp)
                    ).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public List<PecAziendaWithIdAzienda> getPecAziendaListWithIdAzienda(List<PecAzienda> pecAziendaList) {
        if (pecAziendaList != null && !pecAziendaList.isEmpty()) {
            return pecAziendaList.stream().map(pecAzienda -> factory.createProjection(PecAziendaWithIdAzienda.class, pecAzienda))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public List<MessageAddressWithIdAddress> getMessageAddressListWithIdAddress(Message message) {
        if (message != null) {
            List<MessageAddress> messageAddresssList = message.getMessageAddressList();
            if (messageAddresssList != null && !messageAddresssList.isEmpty()) {
                return messageAddresssList.stream().map(messageAddress -> factory.createProjection(MessageAddressWithIdAddress.class, messageAddress))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<MessageTagWithIdTag> getMessageTagListWithIdTag(Message message) {
        if (message != null) {
            List<MessageTag> messageTagList = message.getMessageTagList();
            if (messageTagList != null && !messageTagList.isEmpty()) {
                return messageTagList.stream().map(messageTag -> factory.createProjection(MessageTagWithIdTag.class, messageTag))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<MessageFolderWithIdFolder> getMessageFolderListWithIdFolder(Message message) {
        try {
            return (List<MessageFolderWithIdFolder>) projectionsInterceptorLauncher.lanciaInterceptorCollection(message, "getMessageFolderList", MessageFolderWithIdFolder.class.getSimpleName());
        } catch (EntityReflectionException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | NoSuchFieldException | InterceptorException | AbortLoadInterceptorException ex) {
            LOGGER.error("errore nell'estrazione di getRibaltoneDaLanciareList", ex);
            return null;
        }
    }

    public List<RibaltoneDaLanciareCustom> getRibaltoneDaLanciareListWithIdUtente(Azienda a) {
        try {
            return (List<RibaltoneDaLanciareCustom>) projectionsInterceptorLauncher.lanciaInterceptorCollection(a, "getRibaltoneDaLanciareList", RibaltoneDaLanciareCustom.class.getSimpleName());
        } catch (EntityReflectionException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | NoSuchFieldException | InterceptorException | AbortLoadInterceptorException ex) {
            LOGGER.error("errore nell'estrazione di getRibaltoneDaLanciareList", ex);
            return null;
        }
    }

    /**
     * Restituisce gli url da mettere nelle aziende dell'utente, per chiamare le
     * funzioni dell'onCommand sulle applicazioni Inde
     *
     * @param aziendaTarget
     * @return
     * @throws IOException
     * @throws it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException
     */
    public Map<String, String> getUrlCommands(Azienda aziendaTarget) throws IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();

        Map<String, String> result = new HashMap<>();

//        Utente utente = (Utente) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);
//        Integer idSessionLog = (Integer) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.IdSessionLog);
////        crossLoginUrlTemplate = "http://localhost:8080/Procton/Procton.htm?CMD=[encoded-params]";  // TODO: REMOVE, ONLY FOR LOCAL TESTS
//        Persona realPerson = null;
//        if (utente.getUtenteReale() != null) {
//            realPerson = utente.getUtenteReale().getIdPersona();
//        }
//        Persona person = utente.getIdPersona();
//        Azienda aziendaLogin = utente.getIdAzienda();
        result.put(InternautaConstants.UrlCommand.Keys.PROTOCOLLA_PEC_NEW.toString(),
                internautaUtils.getUrl(authenticatedSessionData, "?CMD=ricevi_from_pec;[id_message]&id_sorgente=[id_sorgente]&pec_ricezione=[pec_ricezione]", "procton", aziendaTarget));
        result.put(InternautaConstants.UrlCommand.Keys.PROTOCOLLA_PEC_ADD.toString(),
                internautaUtils.getUrl(authenticatedSessionData, "?CMD=add_from_pec;[id_message]&id_sorgente=[id_sorgente]&pec_ricezione=[pec_ricezione]", "procton", aziendaTarget));
        result.put(InternautaConstants.UrlCommand.Keys.ARCHIVE_MESSAGE.toString(),
                internautaUtils.getUrl(authenticatedSessionData, "?CMD=fascicola_shpeck;[id_message]", "babel", aziendaTarget));
        return result;
    }

    /**
     * Data un azienda torna il baseUrl corretto a secondo se si è fatto il
     * login da internet oppure no
     *
     * @param azienda
     * @return
     * @throws IOException
     * @throws BlackBoxPermissionException
     */
    public String getBaseUrl(Azienda azienda) throws IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        String baseUrl;
        AziendaParametriJson aziendaParams = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        if (authenticatedSessionData.isFromInternet()) {
            baseUrl = aziendaParams.getInternetBasePath();
        } else {
            baseUrl = aziendaParams.getBasePath();
        }
        return baseUrl;
    }

    /**
     * restituisce i parametri dell'azienda che servono al front end e non
     * contengono informazioni sensibili
     *
     * @return
     */
    public Map<String, String> getParametriAziendaFrontEnd() throws IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        LOGGER.info("getParametriAziendaFrontEnd authenticatedSessionData.isFromInternet(): " + authenticatedSessionData.isFromInternet());
        final String LOGOUT_URL_KEY = "logoutUrl";

        Map<String, String> result = new HashMap<>();

        Utente utente = (Utente) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);

        AziendaParametriJson parametri = AziendaParametriJson.parse(objectMapper, utente.getIdAzienda().getParametri());
        if (authenticatedSessionData.isFromInternet()) {
            try {
                parametri.setBasePath(parametri.getInternetBasePath());
                parametri.setLogoutUrl(parametri.getInternetLogoutUrl());
            } catch (Exception ex) {
                LOGGER.error("errore nel reperimento di isFromInternet", ex);
            }
        }

        result.put(LOGOUT_URL_KEY, parametri.getLogoutUrl());

        return result;
    }

    public KrintShpeckPec getPecKrint(Message message) {
        return factory.createProjection(KrintShpeckPec.class, message.getIdPec());
    }

//    public List<GruppiContattiWithIdContatto> getContattiDelGruppoWithIdContatto(Contatto contatto) {
//        if (contatto != null) {
//            List<GruppiContatti> contattiDelGruppoList = contatto.getContattiDelGruppoList();
//            if (contattiDelGruppoList != null && !contattiDelGruppoList.isEmpty()) {
//                return contattiDelGruppoList.stream().map(
//                        gruppoContatto -> factory.createProjection(GruppiContattiWithIdContatto.class, gruppoContatto))
//                        .collect(Collectors.toList());
//            } else {
//                return null;
//            }
//        } else {
//            return null;
//        }
//    }
//
//    public List<GruppiContattiWithIdGruppo> getGruppiDelContattoWithIdGruppo(Contatto contatto) {
//        if (contatto != null) {
//            List<GruppiContatti> gruppiDelContattoList = contatto.getGruppiDelContattoList();
//            if (gruppiDelContattoList != null && !gruppiDelContattoList.isEmpty()) {
//                return gruppiDelContattoList.stream().map(
//                        gruppoContatto -> factory.createProjection(GruppiContattiWithIdGruppo.class, gruppoContatto))
//                        .collect(Collectors.toList());
//            } else {
//                return null;
//            }
//        } else {
//            return null;
//        }
//    }
//    public List<DettaglioContattoWithEmailListAndGruppiDelDettaglioListAndIndirizzoListAndTelefonoList> getDettaglioContattoExpanded(Contatto contatto) {
//        if (contatto != null) {
//            List<DettaglioContatto> dettaglioContattoList = contatto.getDettaglioContattoList();
//            if (dettaglioContattoList != null && !dettaglioContattoList.isEmpty()) {
//                return dettaglioContattoList.stream().map(
//                        dettaglioContatto -> factory.createProjection(DettaglioContattoWithEmailListAndGruppiDelDettaglioListAndIndirizzoListAndTelefonoList.class, dettaglioContatto))
//                        .collect(Collectors.toList());
//            } else {
//                return null;
//            }
//        } else {
//            return null;
//        }
//    }
    public List<EmailWithIdDettaglioContatto> getEmailWithIdDettaglioContatto(Contatto contatto) {
        if (contatto != null) {
            List<Email> emailList = contatto.getEmailList();
            if (emailList != null && !emailList.isEmpty()) {
                return emailList.stream().map(
                        email -> factory.createProjection(EmailWithIdDettaglioContatto.class, email))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<IndirizzoWithIdDettaglioContatto> getIndirizzoWithIdDettaglioContatto(Contatto contatto) {
        if (contatto != null) {
            List<Indirizzo> indirizziList = contatto.getIndirizziList();
            if (indirizziList != null && !indirizziList.isEmpty()) {
                return indirizziList.stream().map(
                        indirizzo -> factory.createProjection(IndirizzoWithIdDettaglioContatto.class, indirizzo))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<TelefonoWithIdDettaglioContatto> getTelefonoWithIdDettaglioContatto(Contatto contatto) {
        if (contatto != null) {
            List<Telefono> telefonoList = contatto.getTelefonoList();
            if (telefonoList != null && !telefonoList.isEmpty()) {
                return telefonoList.stream().map(
                        telefono -> factory.createProjection(TelefonoWithIdDettaglioContatto.class, telefono))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<CustomGruppiContattiWithIdContattoAndIdDettaglioContatto> getGruppiContattiWithIdContattoAndIdDettaglioContatto(Contatto contatto) {
        if (contatto != null) {
            List<GruppiContatti> contattiDelGruppoList = contatto.getContattiDelGruppoList();
            if (contattiDelGruppoList != null && !contattiDelGruppoList.isEmpty()) {
                return contattiDelGruppoList.stream().map(
                        gruppoContatto -> factory.createProjection(CustomGruppiContattiWithIdContattoAndIdDettaglioContatto.class, gruppoContatto))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<GruppiContattiWithIdDettaglioContattoAndIdGruppo> getGruppiContattiWithIdDettaglioContattoAndIdGruppo(Contatto contatto) {
        if (contatto != null) {
            List<GruppiContatti> gruppiDelContattoList = contatto.getGruppiDelContattoList();
            if (gruppiDelContattoList != null && !gruppiDelContattoList.isEmpty()) {
                return gruppiDelContattoList.stream().map(
                        gruppoContatto -> factory.createProjection(GruppiContattiWithIdDettaglioContattoAndIdGruppo.class, gruppoContatto))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

//    public List<PredicatoAmbitoWithIdPredicato> expandPredicatiAmbiti(Integer[] idPredicatiAmbiti) {
//        List<PredicatoAmbitoWithIdPredicato> res = new ArrayList();
//        if (idPredicatiAmbiti != null) {
//            for (Integer idPredicatoAmbito : idPredicatiAmbiti) {
//                PredicatoAmbito predicatoAmbito = this.predicatoAmbitoRepository.getOne(idPredicatoAmbito);
//                res.add(factory.createProjection(PredicatoAmbitoWithIdPredicato.class, predicatoAmbito));
//            }
//        }
//        return res;
//    }
//    @Cacheable(value = "permessi__ribaltorg__", key = "{#idPredicatiAmbiti.toString()}")
    public List<PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded> expandPredicatiAmbiti(Integer[] idPredicatiAmbiti) {
        List<PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded> res = new ArrayList();
        if (idPredicatiAmbiti != null) {
            for (Integer idPredicatoAmbito : idPredicatiAmbiti) {
                PredicatoAmbito predicatoAmbito = this.predicatoAmbitoRepository.getOne(idPredicatoAmbito);
                res.add(factory.createProjection(PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded.class, predicatoAmbito));
            }
        }
        return res;
    }

    public List<UtenteWithIdPersona> getResposabiliStruttura(Struttura struttura) {
        List<UtenteWithIdPersona> res = null;
        String idUtentiResponsabiliArray = strutturaRepository.getResponsabili(struttura.getId());
        if (idUtentiResponsabiliArray != null) {
            JSONArray array = new JSONArray(idUtentiResponsabiliArray);
            List<Integer> idUtentiResponsabili = new ArrayList();
            for (int i = 0; i < array.length(); ++i) {
                idUtentiResponsabili.add(array.optInt(i));
            }
            if (!idUtentiResponsabili.isEmpty()) {
                res = idUtentiResponsabili.stream().map(idUtenteResponsabile -> {
                    Utente utenteResposabile = utenteRepository.findById(idUtenteResponsabile).get();
                    return factory.createProjection(UtenteWithIdPersona.class, utenteResposabile);
                }).collect(Collectors.toList());
            }
        }
        return res;
    }

    public StrutturaWithUtentiResponsabiliCustom getStrutturaWithUtentiReponsabili(UtenteStruttura utenteStruttura) {
        StrutturaWithUtentiResponsabiliCustom res = null;
        Struttura idStruttura = utenteStruttura.getIdStruttura();
        if (idStruttura != null) {
            res = factory.createProjection(StrutturaWithUtentiResponsabiliCustom.class, idStruttura);
        }
        return res;
    }

    public List<UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom> getStruttureUtenteWithAfferenzaAndReponsabili(Utente utente) {
        List<UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom> res = null;
        List<UtenteStruttura> utenteStrutturaList = utente.getUtenteStrutturaList();
        if (utenteStrutturaList != null && !utenteStrutturaList.isEmpty()) {
            res = utenteStrutturaList.stream().map(utenteStruttura -> {
                return factory.createProjection(UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom.class, utenteStruttura);
            }).collect(Collectors.toList());
        }
        return res;
    }

    public UtenteWithIdPersonaAndPermessiCustom getUtenteWithIdPersonaAndPermessiCustom(UtenteStruttura utenteStruttura) {
        UtenteWithIdPersonaAndPermessiCustom res = null;
        Utente idUtente = utenteStruttura.getIdUtente();
        if (idUtente != null) {
            res = factory.createProjection(UtenteWithIdPersonaAndPermessiCustom.class, idUtente);
        }
        return res;
    }

    public PersonaWithUtentiAndStruttureAndAfferenzeCustom getPersonaWithUtentiAndStruttureAndAfferenzeCustom(Contatto contatto) {
        PersonaWithUtentiAndStruttureAndAfferenzeCustom res = null;
        Persona idPersona = contatto.getIdPersona();
        if (idPersona != null) {
            res = factory.createProjection(PersonaWithUtentiAndStruttureAndAfferenzeCustom.class, idPersona);
        }
        return res;
    }

    public List<UtenteWithStruttureAndResponsabiliCustom> getUtenteWithStruttureAndResponsabiliCustom(Persona persona) {
        List<UtenteWithStruttureAndResponsabiliCustom> res = null;
        List<Utente> utenteList = persona.getUtenteList();
        if (utenteList != null && !utenteList.isEmpty()) {
            res = utenteList.stream().map(utente -> {
                return factory.createProjection(UtenteWithStruttureAndResponsabiliCustom.class, utente);
            }).collect(Collectors.toList());
        }
        return res;
    }

    public List<CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda(Contatto contatto) {
        List<CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> res = null;
        List<DettaglioContatto> dettaglioContattoList = contatto.getDettaglioContattoList();
        if (dettaglioContattoList != null && !dettaglioContattoList.isEmpty()) {
            res = dettaglioContattoList.stream().map(dettaglioContatto -> {
                return factory.createProjection(CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda.class, dettaglioContatto);
            }).collect(Collectors.toList());
        }
        return res;
    }

    public CustomContattoWithIdStrutturaAndIdPersona getContattoWithIdStrutturaAndIdPersonaByGruppoContatto(GruppiContatti gruppoContatto) {
        Contatto idContatto = gruppoContatto.getIdContatto();
        if (idContatto != null) {
            return factory.createProjection(CustomContattoWithIdStrutturaAndIdPersona.class, idContatto);
        }
        return null;
    }

    public CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAziendaByGruppoContatto(GruppiContatti gruppoContatto) {
        DettaglioContatto dettaglioContatto = gruppoContatto.getIdDettaglioContatto();
        if (dettaglioContatto != null) {
            return factory.createProjection(CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda.class, dettaglioContatto);
        }
        return null;
    }

    public StrutturaWithPlainFields getStrutturaFigliaWithFogliaCalcolata(StoricoRelazione storicoRelazione) {
        Struttura idStrutturaFiglia = storicoRelazione.getIdStrutturaFiglia();
        if (idStrutturaFiglia != null) {
            LocalDateTime dataRiferimento = additionalDataParamsExtractor.getDataRiferimento().truncatedTo(ChronoUnit.DAYS);
            if (dataRiferimento == null) {
                dataRiferimento = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
            }
            QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
            BooleanExpression filter = qStoricoRelazione.idStrutturaPadre.id.eq(idStrutturaFiglia.getId()).and(qStoricoRelazione.attivaDal.loe(dataRiferimento)
                    .and((qStoricoRelazione.attivaAl.isNull()).or(qStoricoRelazione.attivaAl.goe(dataRiferimento))));
            boolean isLeaf = !storicoRelazioneRepository.exists(filter);
//                List<Struttura> struttureFiglieDellaStrutturaFiglia = new ArrayList<>();
//                struttureFiglieDellaStrutturaFigliaInStoricoRelazione.forEach(storicoRelazioneFiglie -> {
//                    struttureFiglieDellaStrutturaFiglia.add(storicoRelazioneFiglie.getIdStrutturaFiglia());
//                });
            idStrutturaFiglia.setFogliaCalcolata(isLeaf);

            return factory.createProjection(StrutturaWithPlainFields.class, idStrutturaFiglia);
        }
        return null;
    }

    public List<KrintRubricaDettaglioContatto> getCustomKrintDettaglioContattoList(Contatto contatto) {
        List<DettaglioContatto> dettaglioContattoList = contatto.getDettaglioContattoList();
        List<KrintRubricaDettaglioContatto> res = null;
        if (dettaglioContattoList != null && !dettaglioContattoList.isEmpty()) {
            res = dettaglioContattoList.stream().map(dettaglioContatto -> {
                return factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
            }).collect(Collectors.toList());
        }
        return res;
    }

    public List<KrintRubricaGruppoContatto> getCustomKrintContattiDelGruppoList(Contatto gruppo) {
        List<GruppiContatti> contattiDelGruppoList = gruppo.getContattiDelGruppoList();
        List<KrintRubricaGruppoContatto> res = null;
        if (contattiDelGruppoList != null && !contattiDelGruppoList.isEmpty()) {
            res = contattiDelGruppoList.stream().map(gruppoContatto -> {
                return factory.createProjection(KrintRubricaGruppoContatto.class, gruppoContatto);
            }).collect(Collectors.toList());
        }
        return res;
    }

    public KrintRubricaContatto getCustomKrintContatto(Contatto contatto) {
        if (contatto != null) {
            return factory.createNullableProjection(KrintRubricaContatto.class, contatto);
        }
        return null;
    }

    public KrintRubricaDettaglioContatto getCustomKrintDettaglioContatto(DettaglioContatto dettaglioContatto) {
        if (dettaglioContatto != null) {
            return factory.createNullableProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
        }
        return null;
    }

    public Object getEntita(Object object) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class entityClass = EntityReflectionUtils.getEntityFromProxyObject(object);
        Table tableAnnotation = (Table) entityClass.getAnnotation(Table.class);
        String targetSchema = tableAnnotation.schema();
        String targetTable = tableAnnotation.name();
        TipoEntita tipoEntita = tipoEntitaRepository.findOne(QTipoEntita.tipoEntita.targetSchema.eq(targetSchema)
                .and(QTipoEntita.tipoEntita.targetTable.eq(targetTable))).get();
        Method primaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(object);
        Object idEsterno = primaryKeyGetMethod.invoke(object);
        Entita entita = entitaRepository.findOne(
                QEntita.entita.idProvenienza.eq(Integer.parseInt(idEsterno.toString()))
                        .and(QEntita.entita.idTipoEntita.id.eq(tipoEntita.getId()))).get();
        return factory.createProjection(EntitaWithPlainFields.class, entita);
    }

    public Object getPredicato(Permesso permesso) {
        return predicatoRepository.findById(permesso.getIdPredicato().getId()).get();
    }

    private String getElencoCodiciAziendeAttualiPersona(Persona persona) {
        String codiciAziende = "";
        List<Utente> utenteList = persona.getUtenteList();
        if (utenteList != null) {
            for (Utente utente : utenteList) {
                utente = utenteRepository.findById(utente.getId()).get();
                if (utente.getAttivo()) {
                    Azienda azienda = aziendaRepository.findById(utente.getIdAzienda().getId()).get();
                    codiciAziende = codiciAziende + (codiciAziende.length() == 0 ? "" : ", ") + azienda.getNome();
                }
            }
        }
        return codiciAziende;
    }

    public List<PermessoEntitaStoredProcedure> getPermessiContatto(Contatto contatto) throws BlackBoxPermissionException {

        List<String> predicati = new ArrayList<>();
        predicati.add("ACCESSO");
        List<String> ambiti = new ArrayList<>();
        ambiti.add("RUBRICA");
        List<String> tipi = new ArrayList<>();
        tipi.add("CONTATTO");

        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = new ArrayList<>();
        subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(contatto, predicati, ambiti, tipi, Boolean.FALSE);
        if (subjectsWithPermissionsOnObject != null) {
            for (PermessoEntitaStoredProcedure permessoEntitaStoredProcedure : subjectsWithPermissionsOnObject) {
                if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.strutture.toString())) {
                    Struttura strutturaSoggetto = strutturaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(strutturaSoggetto.getNome()
                            + " [ " + strutturaSoggetto.getIdAzienda().getNome() + (strutturaSoggetto.getCodice() != null ? " - " + strutturaSoggetto.getCodice() : "") + " ]");
                    permessoEntitaStoredProcedure.getSoggetto().setAdditionalData(
                            strutturaRepository.getCountUtentiStruttura(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza())
                    );
                } else if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.persone.toString())) {
                    Persona personaSoggetto = personaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
                    //permessoEntitaStoredProcedure.getSoggetto().setDescrizione(personaSoggetto.getDescrizione() + " [" + personaSoggetto.getCodiceFiscale() + "]");
                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(personaSoggetto.getDescrizione() + " [ " + getElencoCodiciAziendeAttualiPersona(personaSoggetto) + " ]");
                }
            }
        }

        return subjectsWithPermissionsOnObject;
    }

    public List<PermessoEntitaStoredProcedure> getStruttureConnesseAUfficio(Struttura struttura) throws BlackBoxPermissionException {

        List<String> predicati = new ArrayList<>();
        predicati.add("CONNESSO");
        List<String> ambiti = new ArrayList<>();
        ambiti.add("BABORG");
        List<String> tipi = new ArrayList<>();
        tipi.add("UFFICIO");

        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = new ArrayList<>();
        subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(struttura, predicati, ambiti, tipi, Boolean.FALSE);
        if (subjectsWithPermissionsOnObject != null) {
            for (PermessoEntitaStoredProcedure permessoEntitaStoredProcedure : subjectsWithPermissionsOnObject) {
                if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.strutture.toString())) {
                    Struttura strutturaSoggetto = strutturaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(
                            strutturaSoggetto.getNome() + (strutturaSoggetto.getCodice() != null ? " [" + strutturaSoggetto.getCodice() + "]" : ""));
                }
            }
        }

        return subjectsWithPermissionsOnObject;
    }

    public String getCountUtentiStruttura(Struttura struttura) {
        return strutturaRepository.getCountUtentiStruttura(struttura.getId());
    }

    @Cacheable(value = "getParametriAzienda", key = "{#azienda.getId()}")
    public Map<String, Object> getParametriAzienda(Azienda azienda) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Applicazione.Applicazioni applicazione = authenticatedSessionData.getApplicazione();

        Map<String, Object> parametri = parametriAziende.getAllAziendaApplicazioneParameters(applicazione, azienda.getId());

        return parametri;
    }
    
    public List<Related> filterRelated(List<Related> related, String tipo) {
        return related.stream().filter(r -> r.getTipo().toString().equals(tipo)).collect(Collectors.toList());
    }
    
    public List<CustomRelatedWithSpedizioneList> filterRelatedWithSpedizioneList(List<Related> related, String tipo) {
        List<CustomRelatedWithSpedizioneList> res = null;
        List<Related> relatedList = related.stream().filter(r -> r.getTipo().toString().equals(tipo)).collect(Collectors.toList());
        if (relatedList != null && !relatedList.isEmpty()) {
            res = relatedList.stream().map(r -> {
                return factory.createProjection(CustomRelatedWithSpedizioneList.class, r);
            }).collect(Collectors.toList());
        }
        return res;
    }
    
    public List<SpedizioneWithIdMezzo> getSpedizioneWithIdMezzo(List<Spedizione> spedizioneList) {
        List<SpedizioneWithIdMezzo> res = null;
        if (spedizioneList != null && !spedizioneList.isEmpty()) {
            res = spedizioneList.stream().map(spediz -> {
                return factory.createProjection(SpedizioneWithIdMezzo.class, spediz);
            }).collect(Collectors.toList());
        }
        return res;
    }
}
