//package it.bologna.ausl.internauta.service.utils;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.querydsl.core.types.dsl.BooleanExpression;
//import it.bologna.ausl.blackbox.PermissionManager;
//import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
//import it.bologna.ausl.blackbox.repositories.EntitaRepository;
//import it.bologna.ausl.blackbox.repositories.TipoEntitaRepository;
//import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
//import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
//import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
//import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
//import it.bologna.ausl.model.entities.baborg.Azienda;
//import it.bologna.ausl.model.entities.baborg.Persona;
//import it.bologna.ausl.model.entities.baborg.Struttura;
//import it.bologna.ausl.model.entities.baborg.Utente;
//import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
//import it.bologna.ausl.model.entities.baborg.projections.ribaltone.RibaltoneDaLanciareCustom;
//import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
//import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
//import it.bologna.ausl.model.entities.configurazione.ImpostazioniApplicazioni;
//import it.bologna.ausl.model.entities.configurazione.projections.generated.ImpostazioniApplicazioniWithPlainFields;
//import it.bologna.ausl.model.entities.shpeck.Message;
//import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
//import it.nextsw.common.interceptors.exceptions.InterceptorException;
//import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
//import it.nextsw.common.utils.exceptions.EntityReflectionException;
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.util.List;
//import java.util.stream.Collectors;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.projection.ProjectionFactory;
//import org.springframework.stereotype.Component;
//import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
//import it.bologna.ausl.internauta.service.authorization.UserInfoService;
//import it.bologna.ausl.internauta.service.interceptors.baborg.AziendaInterceptor;
//import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
//import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
//import it.bologna.ausl.internauta.service.repositories.baborg.StoricoRelazioneRepository;
//import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
//import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaUnificataRepository;
//import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
//import it.bologna.ausl.internauta.service.repositories.permessi.PredicatoAmbitoRepository;
//import it.bologna.ausl.internauta.service.repositories.permessi.PredicatoRepository;
//import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
//import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
//import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
//import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
//import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
//import it.bologna.ausl.model.entities.baborg.projections.persona.PersonaWithUtentiAndStruttureAndAfferenzeCustom;
//import it.bologna.ausl.model.entities.baborg.projections.strutturaunificata.StrutturaUnificataCustom;
//import it.bologna.ausl.model.entities.baborg.projections.struttura.StrutturaWithReplicheCustom;
//import it.bologna.ausl.model.entities.configurazione.Applicazione;
//import java.util.HashMap;
//import java.util.Map;
//import it.bologna.ausl.model.entities.logs.projections.KrintShpeckPec;
//import it.bologna.ausl.model.entities.rubrica.Contatto;
//import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
//import java.util.ArrayList;
//import org.slf4j.Logger;
//import it.bologna.ausl.model.entities.baborg.projections.utentestruttura.UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom;
//import it.bologna.ausl.model.entities.baborg.projections.struttura.StrutturaWithUtentiResponsabiliCustom;
//import it.bologna.ausl.model.entities.baborg.projections.utente.UtenteWithStruttureAndResponsabiliCustom;
//import it.bologna.ausl.model.entities.logs.projections.KrintRubricaContatto;
//import it.bologna.ausl.model.entities.logs.projections.KrintRubricaDettaglioContatto;
//import it.bologna.ausl.model.entities.logs.projections.KrintRubricaGruppoContatto;
//import it.bologna.ausl.model.entities.permessi.Entita;
//import it.bologna.ausl.model.entities.permessi.Permesso;
//import it.bologna.ausl.model.entities.permessi.PredicatoAmbito;
//import it.bologna.ausl.model.entities.permessi.QEntita;
//import it.bologna.ausl.model.entities.permessi.QTipoEntita;
//import it.bologna.ausl.model.entities.permessi.TipoEntita;
//import it.bologna.ausl.model.entities.permessi.projections.PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded;
//import it.bologna.ausl.model.entities.permessi.projections.generated.EntitaWithPlainFields;
//import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
//import it.bologna.ausl.model.entities.rubrica.projections.CustomContattoWithIdStrutturaAndIdPersona;
//import it.bologna.ausl.model.entities.rubrica.projections.CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda;
//import it.bologna.ausl.model.entities.scripta.Related;
//import it.bologna.ausl.model.entities.scripta.projections.CustomRelatedWithSpedizioneList;
//import it.nextsw.common.utils.EntityReflectionUtils;
//import java.lang.reflect.Method;
//import java.time.ZonedDateTime;
//import java.time.temporal.ChronoUnit;
//import javax.persistence.Table;
//import org.json.JSONArray;
//import org.springframework.cache.annotation.Cacheable;
//import it.bologna.ausl.model.entities.baborg.projections.utente.UtenteLoginCustom;
//import it.bologna.ausl.model.entities.baborg.projections.persona.CustomPersonaLogin;
//
///**
// *
// * @author guido
// */
//@Component
//public class ProjectionBeans {
//
//    public Object o = new Object();
//
//    @Autowired
//    protected ProjectionFactory factory;
//
//    @Autowired
//    protected CachedEntities cachedEntities;
//
//    @Autowired
//    protected EntitaRepository entitaRepository;
//
//    @Autowired
//    protected TipoEntitaRepository tipoEntitaRepository;
//
//    @Autowired
//    protected ImpostazioniApplicazioniRepository impostazioniApplicazioniRepository;
//
//    @Autowired
//    protected AziendaRepository aziendaRepository;
//
//    @Autowired
//    protected UtenteRepository utenteRepository;
//
//    @Autowired
//    protected StrutturaRepository strutturaRepository;
//
//    @Autowired
//    protected UtenteStrutturaRepository utenteStrutturaRepository;
//
//    @Autowired
//    protected PersonaRepository personaRepository;
//
//    @Autowired
//    protected StoricoRelazioneRepository storicoRelazioneRepository;
//    
//    @Autowired
//    protected StrutturaUnificataRepository strutturaUnificataRepository;
//
//    @Autowired
//    protected PredicatoAmbitoRepository predicatoAmbitoRepository;
//
//    @Autowired
//    protected PredicatoRepository predicatoRepository;
//
//    @Autowired
//    ProjectionsInterceptorLauncher projectionsInterceptorLauncher;
//
//    @Autowired
//    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
//
//    @Autowired
//    UserInfoService userInfoService;
//
//    @Autowired
//    PermissionManager permissionManager;
//
//    @Autowired
//    InternautaUtils internautaUtils;
//
//    @Autowired
//    HttpSessionData httpSessionData;
//
//    @Autowired
//    AziendaInterceptor aziendaInterceptor;
//
//    @Autowired
//    ParametriAziendeReader parametriAziende;
//
//    @Autowired
//    AdditionalDataParamsExtractor additionalDataParamsExtractor;
//
//    @Autowired
//    ObjectMapper objectMapper;
//
//    final String APP_URL_PICO = "/Procton/Procton.htm";
//    final String APP_URL_BABEL = "/Babel/Babel.htm";
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectionBeans.class);
//
//    public UtenteLoginCustom getUtenteRealeWithIdPersonaImpostazioniApplicazioniList(Utente utente) {
//        if (utente.getUtenteReale() != null) {
//            return factory.createProjection(UtenteLoginCustom.class, utente.getUtenteReale());
//        } else {
//            return null;
//        }
//    }
//
//    public CustomPersonaLogin getIdPersonaWithImpostazioniApplicazioniList(Utente utente) {
//        return factory.createProjection(CustomPersonaLogin.class, utente.getIdPersona());
//    }
//
//    public AziendaWithPlainFields getAziendaWithPlainFields(Utente utente) {
//        return factory.createProjection(AziendaWithPlainFields.class, utente.getIdAzienda());
//    }
//
//    public List<ImpostazioniApplicazioniWithPlainFields> getImpostazioniApplicazioniListWithPlainFields(Persona persona) throws BlackBoxPermissionException {
////        setAuthenticatedUserProperties();
//        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
//        Applicazione.Applicazioni applicazione = authenticatedSessionData.getApplicazione();
//        List<ImpostazioniApplicazioni> impostazioniApplicazioniList = persona.getImpostazioniApplicazioniList();
//        if (impostazioniApplicazioniList != null && !impostazioniApplicazioniList.isEmpty()) {
//            return impostazioniApplicazioniList.stream().filter(imp -> imp.getIdApplicazione().getId().equals(applicazione.toString())).
//                    map(
//                            imp -> factory.createProjection(ImpostazioniApplicazioniWithPlainFields.class, imp)
//                    ).collect(Collectors.toList());
//        } else {
//            return null;
//        }
//    }
//
//    public List<RibaltoneDaLanciareCustom> getRibaltoneDaLanciareListWithIdUtente(Azienda a) {
//        try {
//            return (List<RibaltoneDaLanciareCustom>) projectionsInterceptorLauncher.lanciaInterceptorCollection(a, "getRibaltoneDaLanciareList", RibaltoneDaLanciareCustom.class.getSimpleName());
//        } catch (EntityReflectionException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | NoSuchFieldException | InterceptorException | AbortLoadInterceptorException ex) {
//            LOGGER.error("errore nell'estrazione di getRibaltoneDaLanciareList", ex);
//            return null;
//        }
//    }
//
//    /**
//     * Restituisce gli url da mettere nelle aziende dell'utente, per chiamare le
//     * funzioni dell'onCommand sulle applicazioni Inde
//     *
//     * @param aziendaTarget
//     * @return
//     * @throws IOException
//     * @throws it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException
//     */
//    public Map<String, String> getUrlCommands(Azienda aziendaTarget) throws IOException, BlackBoxPermissionException {
//        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
//
//        Map<String, String> result = new HashMap<>();
//
////        Utente utente = (Utente) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);
////        Integer idSessionLog = (Integer) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.IdSessionLog);
//////        crossLoginUrlTemplate = "http://localhost:8080/Procton/Procton.htm?CMD=[encoded-params]";  // TODO: REMOVE, ONLY FOR LOCAL TESTS
////        Persona realPerson = null;
////        if (utente.getUtenteReale() != null) {
////            realPerson = utente.getUtenteReale().getIdPersona();
////        }
////        Persona person = utente.getIdPersona();
////        Azienda aziendaLogin = utente.getIdAzienda();
//        result.put(InternautaConstants.UrlCommand.Keys.PROTOCOLLA_PEC_NEW.toString(),
//                internautaUtils.getUrl(authenticatedSessionData, "?CMD=ricevi_from_pec;[id_message]&id_sorgente=[id_sorgente]&pec_ricezione=[pec_ricezione]", "procton", aziendaTarget));
//        result.put(InternautaConstants.UrlCommand.Keys.PROTOCOLLA_PEC_ADD.toString(),
//                internautaUtils.getUrl(authenticatedSessionData, "?CMD=add_from_pec;[id_message]&id_sorgente=[id_sorgente]&pec_ricezione=[pec_ricezione]", "procton", aziendaTarget));
//        result.put(InternautaConstants.UrlCommand.Keys.ARCHIVE_MESSAGE.toString(),
//                internautaUtils.getUrl(authenticatedSessionData, "?CMD=fascicola_shpeck;[id_message]", "babel", aziendaTarget));
//        return result;
//    }
//
//    /**
//     * Data un azienda torna il baseUrl corretto a secondo se si è fatto il
//     * login da internet oppure no
//     *
//     * @param azienda
//     * @return
//     * @throws IOException
//     * @throws BlackBoxPermissionException
//     */
//    public String getBaseUrl(Azienda azienda) throws IOException, BlackBoxPermissionException {
//        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
//        String baseUrl;
//        AziendaParametriJson aziendaParams = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
//        if (authenticatedSessionData.isFromInternet()) {
//            baseUrl = aziendaParams.getInternetBasePath();
//        } else {
//            baseUrl = aziendaParams.getBasePath();
//        }
//        return baseUrl;
//    }
//
//    /**
//     * restituisce i parametri dell'azienda che servono al front end e non
//     * contengono informazioni sensibili
//     *
//     * @return
//     */
//    public Map<String, String> getParametriAziendaFrontEnd() throws IOException, BlackBoxPermissionException {
//        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
//        LOGGER.info("getParametriAziendaFrontEnd authenticatedSessionData.isFromInternet(): " + authenticatedSessionData.isFromInternet());
//        final String LOGOUT_URL_KEY = "logoutUrl";
//
//        Map<String, String> result = new HashMap<>();
//
//        Utente utente = (Utente) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);
//
//        AziendaParametriJson parametri = AziendaParametriJson.parse(objectMapper, utente.getIdAzienda().getParametri());
//        if (authenticatedSessionData.isFromInternet()) {
//            try {
//                parametri.setBasePath(parametri.getInternetBasePath());
//                parametri.setLogoutUrl(parametri.getInternetLogoutUrl());
//            } catch (Exception ex) {
//                LOGGER.error("errore nel reperimento di isFromInternet", ex);
//            }
//        }
//
//        result.put(LOGOUT_URL_KEY, parametri.getLogoutUrl());
//
//        return result;
//    }
//
//    public KrintShpeckPec getPecKrint(Message message) {
//        return factory.createProjection(KrintShpeckPec.class, message.getIdPec());
//    }
//
//    public List<PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded> expandPredicatiAmbiti(Integer[] idPredicatiAmbiti) {
//        List<PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded> res = new ArrayList();
//        if (idPredicatiAmbiti != null) {
//            for (Integer idPredicatoAmbito : idPredicatiAmbiti) {
//                PredicatoAmbito predicatoAmbito = this.predicatoAmbitoRepository.getOne(idPredicatoAmbito);
//                res.add(factory.createProjection(PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded.class, predicatoAmbito));
//            }
//        }
//        return res;
//    }
//
//    public List<UtenteWithIdPersona> getResposabiliStruttura(Struttura struttura) {
//        List<UtenteWithIdPersona> res = null;
//        String idUtentiResponsabiliArray = strutturaRepository.getResponsabili(struttura.getId());
//        if (idUtentiResponsabiliArray != null) {
//            JSONArray array = new JSONArray(idUtentiResponsabiliArray);
//            List<Integer> idUtentiResponsabili = new ArrayList();
//            for (int i = 0; i < array.length(); ++i) {
//                idUtentiResponsabili.add(array.optInt(i));
//            }
//            if (!idUtentiResponsabili.isEmpty()) {
//                res = idUtentiResponsabili.stream().map(idUtenteResponsabile -> {
//                    Utente utenteResposabile = utenteRepository.findById(idUtenteResponsabile).get();
//                    return factory.createProjection(UtenteWithIdPersona.class, utenteResposabile);
//                }).collect(Collectors.toList());
//            }
//        }
//        return res;
//    }
//
//    public StrutturaWithUtentiResponsabiliCustom getStrutturaWithUtentiReponsabili(UtenteStruttura utenteStruttura) {
//        StrutturaWithUtentiResponsabiliCustom res = null;
//        Struttura idStruttura = utenteStruttura.getIdStruttura();
//        if (idStruttura != null) {
//            res = factory.createProjection(StrutturaWithUtentiResponsabiliCustom.class, idStruttura);
//        }
//        return res;
//    }
//
//    public List<UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom> getStruttureUtenteWithAfferenzaAndReponsabili(Utente utente) {
//        List<UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom> res = null;
//        List<UtenteStruttura> utenteStrutturaList = utente.getUtenteStrutturaList();
//        if (utenteStrutturaList != null && !utenteStrutturaList.isEmpty()) {
//            res = utenteStrutturaList.stream().map(utenteStruttura -> {
//                return factory.createProjection(UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom.class, utenteStruttura);
//            }).collect(Collectors.toList());
//        }
//        return res;
//    }
//
//    public PersonaWithUtentiAndStruttureAndAfferenzeCustom getPersonaWithUtentiAndStruttureAndAfferenzeCustom(Contatto contatto) {
//        PersonaWithUtentiAndStruttureAndAfferenzeCustom res = null;
//        Persona idPersona = contatto.getIdPersona();
//        if (idPersona != null) {
//            res = factory.createProjection(PersonaWithUtentiAndStruttureAndAfferenzeCustom.class, idPersona);
//        }
//        return res;
//    }
//
//    public List<UtenteWithStruttureAndResponsabiliCustom> getUtenteWithStruttureAndResponsabiliCustom(Persona persona) {
//        List<UtenteWithStruttureAndResponsabiliCustom> res = null;
//        List<Utente> utenteList = persona.getUtenteList();
//        if (utenteList != null && !utenteList.isEmpty()) {
//            res = utenteList.stream().map(utente -> {
//                return factory.createProjection(UtenteWithStruttureAndResponsabiliCustom.class, utente);
//            }).collect(Collectors.toList());
//        }
//        return res;
//    }
//
//    public List<CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda(Contatto contatto) {
//        List<CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> res = null;
//        List<DettaglioContatto> dettaglioContattoList = contatto.getDettaglioContattoList();
//        if (dettaglioContattoList != null && !dettaglioContattoList.isEmpty()) {
//            res = dettaglioContattoList.stream().filter(dettaglioContatto -> dettaglioContatto.getEliminato() == false).map(dettaglioContatto -> {
//                return factory.createProjection(CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda.class, dettaglioContatto);                
//            }).collect(Collectors.toList());
//        }
//        return res;
//    }
//
//    public CustomContattoWithIdStrutturaAndIdPersona getContattoWithIdStrutturaAndIdPersonaByGruppoContatto(GruppiContatti gruppoContatto) {
//        Contatto idContatto = gruppoContatto.getIdContatto();
//        if (idContatto != null) {
//            return factory.createProjection(CustomContattoWithIdStrutturaAndIdPersona.class, idContatto);
//        }
//        return null;
//    }
//
//    public CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAziendaByGruppoContatto(GruppiContatti gruppoContatto) {
//        DettaglioContatto dettaglioContatto = gruppoContatto.getIdDettaglioContatto();
//        if (dettaglioContatto != null) {
//            return factory.createProjection(CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda.class, dettaglioContatto);
//        }
//        return null;
//    }
//
//public StrutturaWithReplicheCustom getStrutturaFigliaWithFogliaCalcolata(StoricoRelazione storicoRelazione, boolean showPool) {
//        Struttura idStrutturaFiglia = storicoRelazione.getIdStrutturaFiglia();
//        if (idStrutturaFiglia != null) {
//            
//            // Devo capire se questa struttura è una foglia.
//            ZonedDateTime dataRiferimento = additionalDataParamsExtractor.getDataRiferimentoZoned().truncatedTo(ChronoUnit.DAYS);
//            if (dataRiferimento == null) {
//                dataRiferimento = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
//            }
//            QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
//            BooleanExpression filter = qStoricoRelazione.idStrutturaPadre.id.eq(idStrutturaFiglia.getId()).and(qStoricoRelazione.attivaDal.loe(dataRiferimento)
//                    .and((qStoricoRelazione.attivaAl.isNull()).or(qStoricoRelazione.attivaAl.goe(dataRiferimento))));
//
//            if (showPool == false) {
//                filter = filter.and(qStoricoRelazione.idStrutturaFiglia.ufficio.eq(false));
//            } 
//            boolean isLeaf = !storicoRelazioneRepository.exists(filter);
//            idStrutturaFiglia.setFogliaCalcolata(isLeaf);
//
//            return factory.createProjection(StrutturaWithReplicheCustom.class, idStrutturaFiglia);
//        }
//        return null;
//    }
//    
//    /**
//     * Metedo da chiamare per riempire il campo fusioni di una struttura.
//     * E' necessario che in additionalData ci sia la data per fargli prendere le
//     * fusioni attive in una certa data.
//     * @param struttura
//     * @return 
//     */
//    public List<StrutturaUnificataCustom> getFusioni(Struttura struttura) {
//        ZonedDateTime dataRiferimento = additionalDataParamsExtractor.getDataRiferimentoZoned().truncatedTo(ChronoUnit.DAYS);
//        if (dataRiferimento == null) {
//            dataRiferimento = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
//        }
//        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
//        BooleanExpression filtraFusioni = 
//                qStrutturaUnificata.dataAttivazione.loe(dataRiferimento)
//                .and((qStrutturaUnificata.dataDisattivazione.isNull()).or(qStrutturaUnificata.dataDisattivazione.goe(dataRiferimento)))
//                .and(qStrutturaUnificata.dataAccensioneAttivazione.isNotNull())
//                .and(qStrutturaUnificata.tipoOperazione.eq("FUSIONE"))
//                .and(qStrutturaUnificata.idStrutturaSorgente.id.eq(struttura.getId())
//                        .or(qStrutturaUnificata.idStrutturaDestinazione.id.eq(struttura.getId())));
//        Iterable<StrutturaUnificata> fusioniStruttura = strutturaUnificataRepository.findAll(filtraFusioni);
//        
//        List<StrutturaUnificataCustom> fusioniStrutturaCustom = new ArrayList();
//        
//        if (fusioniStruttura != null) {
//            for (StrutturaUnificata s : fusioniStruttura) {
//                fusioniStrutturaCustom.add(factory.createProjection(StrutturaUnificataCustom.class, s));
//            }
//        }
//
//        return fusioniStrutturaCustom;
//    }
//
//    public List<KrintRubricaDettaglioContatto> getCustomKrintDettaglioContattoList(Contatto contatto) {
//        List<DettaglioContatto> dettaglioContattoList = contatto.getDettaglioContattoList();
//        List<KrintRubricaDettaglioContatto> res = null;
//        if (dettaglioContattoList != null && !dettaglioContattoList.isEmpty()) {
//            res = dettaglioContattoList.stream().map(dettaglioContatto -> {
//                return factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
//            }).collect(Collectors.toList());
//        }
//        return res;
//    }
//
//    public List<KrintRubricaGruppoContatto> getCustomKrintContattiDelGruppoList(Contatto gruppo) {
//        List<GruppiContatti> contattiDelGruppoList = gruppo.getContattiDelGruppoList();
//        List<KrintRubricaGruppoContatto> res = null;
//        if (contattiDelGruppoList != null && !contattiDelGruppoList.isEmpty()) {
//            res = contattiDelGruppoList.stream().map(gruppoContatto -> {
//                return factory.createProjection(KrintRubricaGruppoContatto.class, gruppoContatto);
//            }).collect(Collectors.toList());
//        }
//        return res;
//    }
//
//    public KrintRubricaContatto getCustomKrintContatto(Contatto contatto) {
//        if (contatto != null) {
//            return factory.createNullableProjection(KrintRubricaContatto.class, contatto);
//        }
//        return null;
//    }
//
//    public KrintRubricaDettaglioContatto getCustomKrintDettaglioContatto(DettaglioContatto dettaglioContatto) {
//        if (dettaglioContatto != null) {
//            return factory.createNullableProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
//        }
//        return null;
//    }
//
//    public Object getEntita(Object object) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//        Class entityClass = EntityReflectionUtils.getEntityFromProxyObject(object);
//        Table tableAnnotation = (Table) entityClass.getAnnotation(Table.class);
//        String targetSchema = tableAnnotation.schema();
//        String targetTable = tableAnnotation.name();
//        TipoEntita tipoEntita = tipoEntitaRepository.findOne(QTipoEntita.tipoEntita.targetSchema.eq(targetSchema)
//                .and(QTipoEntita.tipoEntita.targetTable.eq(targetTable))).get();
//        Method primaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(object);
//        Object idEsterno = primaryKeyGetMethod.invoke(object);
//        Entita entita = entitaRepository.findOne(
//                QEntita.entita.idProvenienza.eq(Integer.parseInt(idEsterno.toString()))
//                        .and(QEntita.entita.idTipoEntita.id.eq(tipoEntita.getId()))).get();
//        return factory.createProjection(EntitaWithPlainFields.class, entita);
//    }
//
//    public Object getPredicato(Permesso permesso) {
//        return predicatoRepository.findById(permesso.getIdPredicato().getId()).get();
//    }
//
//    private String getElencoCodiciAziendeAttualiPersona(Persona persona) {
//        String codiciAziende = "";
//        List<Utente> utenteList = persona.getUtenteList();
//        if (utenteList != null) {
//            for (Utente utente : utenteList) {
//                utente = utenteRepository.findById(utente.getId()).get();
//                if (utente.getAttivo()) {
//                    Azienda azienda = aziendaRepository.findById(utente.getIdAzienda().getId()).get();
//                    codiciAziende = codiciAziende + (codiciAziende.length() == 0 ? "" : ", ") + azienda.getNome();
//                }
//            }
//        }
//        return codiciAziende;
//    }
//
//    public List<PermessoEntitaStoredProcedure> getPermessiContatto(Contatto contatto) throws BlackBoxPermissionException {
//
//        List<String> predicati = new ArrayList<>();
//        predicati.add("ACCESSO");
//        List<String> ambiti = new ArrayList<>();
//        ambiti.add("RUBRICA");
//        List<String> tipi = new ArrayList<>();
//        tipi.add("CONTATTO");
//
//        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = new ArrayList<>();
//        subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(contatto, predicati, ambiti, tipi, Boolean.FALSE);
//        if (subjectsWithPermissionsOnObject != null) {
//            for (PermessoEntitaStoredProcedure permessoEntitaStoredProcedure : subjectsWithPermissionsOnObject) {
//                if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.strutture.toString())) {
//                    Struttura strutturaSoggetto = strutturaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
//                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(strutturaSoggetto.getNome()
//                            + " [ " + strutturaSoggetto.getIdAzienda().getNome() + (strutturaSoggetto.getCodice() != null ? " - " + strutturaSoggetto.getCodice() : "") + " ]");
//                    permessoEntitaStoredProcedure.getSoggetto().setAdditionalData(
//                            strutturaRepository.getCountUtentiStruttura(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza())
//                    );
//                } else if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.persone.toString())) {
//                    Persona personaSoggetto = personaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
//                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(personaSoggetto.getDescrizione() + " [ " + getElencoCodiciAziendeAttualiPersona(personaSoggetto) + " ]");
//                }
//            }
//        }
//
//        return subjectsWithPermissionsOnObject;
//    }
//
//    public List<PermessoEntitaStoredProcedure> getStruttureConnesseAUfficio(Struttura struttura) throws BlackBoxPermissionException {
//
//        List<String> predicati = new ArrayList<>();
//        predicati.add("CONNESSO");
//        List<String> ambiti = new ArrayList<>();
//        ambiti.add("BABORG");
//        List<String> tipi = new ArrayList<>();
//        tipi.add("UFFICIO");
//
//        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = new ArrayList<>();
//        subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(struttura, predicati, ambiti, tipi, Boolean.FALSE);
//        if (subjectsWithPermissionsOnObject != null) {
//            for (PermessoEntitaStoredProcedure permessoEntitaStoredProcedure : subjectsWithPermissionsOnObject) {
//                if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.strutture.toString())) {
//                    Struttura strutturaSoggetto = strutturaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
//                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(
//                            strutturaSoggetto.getNome() + (strutturaSoggetto.getCodice() != null ? " [" + strutturaSoggetto.getCodice() + "]" : ""));
//                }
//            }
//        }
//
//        return subjectsWithPermissionsOnObject;
//    }
//
//    public String getCountUtentiStruttura(Struttura struttura) {
//        return strutturaRepository.getCountUtentiStruttura(struttura.getId());
//    }
//
//    @Cacheable(value = "getParametriAzienda", key = "{#azienda.getId()}")
//    public Map<String, Object> getParametriAzienda(Azienda azienda) throws BlackBoxPermissionException {
//        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
//        Applicazione.Applicazioni applicazione = authenticatedSessionData.getApplicazione();
//
//        Map<String, Object> parametri = parametriAziende.getAllAziendaApplicazioneParameters(applicazione, azienda.getId());
//
//        return parametri;
//    }
//
//    public List<Related> filterRelated(List<Related> related, String tipo) {
//        if (related != null) {
//            return related.stream().filter(r -> r.getTipo().toString().equals(tipo)).collect(Collectors.toList());
//        } else {
//            return null;
//        }
//    }
//
//    public List<CustomRelatedWithSpedizioneList> filterRelatedWithSpedizioneList(List<Related> related, String tipo) {
//        List<CustomRelatedWithSpedizioneList> res = null;
//        if (related != null) {
//            List<Related> relatedList = related.stream().filter(r -> r.getTipo().toString().equals(tipo)).collect(Collectors.toList());
//            if (relatedList != null && !relatedList.isEmpty()) {
//                res = relatedList.stream().map(r -> {
//                    return factory.createProjection(CustomRelatedWithSpedizioneList.class, r);
//                }).collect(Collectors.toList());
//            }
//        }
//        return res;
//    }
//}
