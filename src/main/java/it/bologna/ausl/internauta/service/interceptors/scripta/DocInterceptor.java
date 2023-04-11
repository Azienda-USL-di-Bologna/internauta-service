package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AttoreDocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.MezzoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PersonaVedenteRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import it.bologna.ausl.internauta.service.shpeck.utils.ManageMessageRegistrationUtils;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi;
import it.bologna.ausl.internauta.service.utils.ScriptaUtils;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.versatore.VersatoreServiceUtils;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.MessageDoc;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QMezzo;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.scripta.Spedizione.IndirizzoSpedizione;
import it.bologna.ausl.model.entities.shpeck.Address;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageAddress;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataRegistration;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataTagComponent;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author Chiara
 */
@Component
@NextSdrInterceptor(name = "doc-interceptor")
public class DocInterceptor extends InternautaBaseInterceptor {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MezzoRepository mezzoRepository;

    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Autowired
    private KrintScriptaService krintScriptaService;

    @Autowired
    private ManageMessageRegistrationUtils manageMessageRegistrationUtils;

    @Autowired
    private ShpeckUtils shpeckUtils;

    @Autowired
    private ScriptaUtils scriptaUtils;

    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private AttoreDocRepository attoreDocRepository;
    
    @Autowired
    private ParametriAziendeReader parametriAziendaReader;
    
    @Autowired
    private PersonaVedenteRepository personaVedenteRepository;
    
    @Autowired
    private PersonaRepository personaRepository;
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;

    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;

    @Override
    public Class getTargetEntityClass() {
        return Doc.class;
    }

    private Message retrieveMessageFromAdditionalData(Map<String, String> additionalData) {
        Message message = null;
        Integer idMessage = Integer.parseInt(additionalData.get(AdditionalData.Keys.idMessage.toString()));
        Optional<Message> messageOp = this.messageRepository.findById(idMessage);
        if (messageOp.isPresent()) {
            message = messageOp.get();
        }
        return message;
    }

    private List<AdditionalData.OperationsRequested> retriveRequestedOperations(Map<String, String> additionalData) {
        return AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
    }

    private boolean additionalDatacointainThisOperation(
            Map<String, String> additionalData,
            AdditionalData.OperationsRequested requested
    ) {
        List<AdditionalData.OperationsRequested> operationsRequested = retriveRequestedOperations(additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operation : operationsRequested) {
                if (operation.equals(requested)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override

    public Object beforeCreateEntityInterceptor(Object entity,
            Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Doc doc = (Doc) entity;
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = authenticatedSessionData.getPerson();
        persona = personaRepository.getById(persona.getId());

        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case CreateDocPerMessageRegistration:
                        // L'utente ha avviato la protocollazione da Pec. 
                        // In ingresso mi viene detta l'azienda e il messaggio che si vuole protocollare
                        if (StringUtils.hasText(additionalData.get(AdditionalData.Keys.idMessage.toString()))
                                && StringUtils.hasText(additionalData.get(AdditionalData.Keys.codiceAzienda.toString()))) {

                            // Setto l'azienda
                            String codiceAzienda = additionalData.get(AdditionalData.Keys.codiceAzienda.toString());
                            Azienda azienda = aziendaRepository.findByCodice(codiceAzienda);
                            if (azienda == null) {
                                throw new AbortSaveInterceptorException("Azienda non riconosciuta");
                            }
                            doc.setIdAzienda(azienda);
                            doc.setTipologia(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA);
                   
                            if (!userInfoService.userHasPermissionOnAzienda(
                                    Permessi.Predicati.REDIGE,
                                    user,
                                    azienda,
                                    Permessi.Ambiti.PICO,
                                    Permessi.Tipi.FLUSSO)) {
                                throw new AbortSaveInterceptorException("L'utente non ha il permesso di Redige sull'azienda");
                            }

                            // Setto i dati del messaggio
                            Message message = retrieveMessageFromAdditionalData(additionalData);
                            if (message != null) {

                                // Controllo che l'utente abbia i permessi sull casella pec del messaggio
                                try {
                                    if (!shpeckUtils.userHasPermissionOnThisPec(
                                            message.getIdPec(),
                                            Arrays.asList(new String[]{Permessi.Predicati.RISPONDE.toString(),
                                        Permessi.Predicati.ELIMINA.toString()}),
                                            authenticatedSessionData.getPerson()
                                    )) {
                                        throw new AbortSaveInterceptorException("Errore, l'utente non ha il permesso di protocollare i messaggi di questa casella pec");
                                    }
                                } catch (Exception ex) {
                                    Logger.getLogger(DocInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                                    throw new AbortSaveInterceptorException("Errore nel controllo permessi dell'utente");
                                }

                                doc.setOggetto(message.getSubject());
                                Address addressMittente = message.getMessageAddressList()
                                        .stream().filter(messageAddress
                                                -> messageAddress
                                                .getAddressRole() == MessageAddress.AddressRoleType.FROM)
                                        .findFirst()
                                        .get()
                                        .getIdAddress();
                                Related mittenteDoc = new Related();
                                mittenteDoc.setDataInserimento(ZonedDateTime.now());
                                if (StringUtils.hasText(addressMittente.getOriginalAddress())) {
                                    mittenteDoc.setDescrizione(addressMittente.getOriginalAddress());
                                } else {
                                    mittenteDoc.setDescrizione(addressMittente.getMailAddress());
                                }
                                mittenteDoc.setIdPersonaInserente(authenticatedSessionData.getPerson());
                                mittenteDoc.setOrigine(Related.OrigineRelated.ESTERNO);
                                mittenteDoc.setTipo(Related.TipoRelated.MITTENTE);
                                mittenteDoc.setIdDoc(doc);
                                Spedizione spedizione = new Spedizione();
                                spedizione.setData(message.getReceiveTime());
                                spedizione.setIdMessage(message);
                                Mezzo mezzo = mezzoRepository.findOne(QMezzo.mezzo.codice.eq(Mezzo.CodiciMezzo.MAIL.toString())).get();
                                spedizione.setIdMezzo(mezzo);
                                spedizione.setIdRelated(mittenteDoc);
                                IndirizzoSpedizione indirizzoSpedizione = new IndirizzoSpedizione();
                                indirizzoSpedizione.setCompleto(addressMittente.getMailAddress());
                                spedizione.setIndirizzo(indirizzoSpedizione);
                                mittenteDoc.setSpedizioneList(Arrays.asList(new Spedizione[]{spedizione}));
                                List<Related> relatedList = new ArrayList();
                                relatedList.add(mittenteDoc);
                                doc.setRelated(relatedList);
                                
                                // Inserisco la relazione messaggio-doc nella tabella messagesDocs
                                MessageDoc messageDoc = new MessageDoc();
                                messageDoc.setIdDoc(doc);
                                messageDoc.setIdMessage(message);
                                messageDoc.setTipo(MessageDoc.TipoMessageDoc.IN);
                                List<MessageDoc> messageDocList = new ArrayList();
                                messageDocList.add(messageDoc);
                                doc.setMessageDocList(messageDocList);
                                
                                // Setto il tag in registraion sul messaggio
                                // Prima era gestito attraverso le mappe,ora gli additionaldata hanno una loro classe
//                                Map<String, Object> inRegistrationAdditionalData = new HashMap();
                                AdditionalDataRegistration inRegistrationAdditionalData = new AdditionalDataRegistration();
                                AdditionalDataTagComponent.idDocumento documentoAdditionalData = new AdditionalDataTagComponent.idDocumento();
//                                Map<String, Object> idDocumento = new HashMap();
                                documentoAdditionalData.setNumeroProposta("PEIS_CAMPO_DA_AGGIORNARE");
                                documentoAdditionalData.setOggetto(message.getSubject());
                                documentoAdditionalData.setCodiceRegistro("PEIS");
                                documentoAdditionalData.setDataProposta(ZonedDateTime.now().toString());
                                inRegistrationAdditionalData.setIdDocumento(documentoAdditionalData);

                                try {
                                    manageMessageRegistrationUtils.manageMessageRegistration(
                                            message.getUuidMessage(),
                                            InternautaConstants.Shpeck.MessageRegistrationOperation.ADD_IN_REGISTRATION,
                                            message.getId(),
                                            inRegistrationAdditionalData,
                                            true,
                                            azienda
                                    );
                                } catch (Throwable ex) {
                                    Logger.getLogger(DocInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                                    throw new AbortSaveInterceptorException("Errore nella gestione del tag in registration");
                                }
                                
                                List<AttoreDoc> attori = new ArrayList();
                                AttoreDoc creatore = new AttoreDoc();
                                creatore.setIdDoc(doc);
                                creatore.setIdPersona(persona);
                                creatore.setRuolo(AttoreDoc.RuoloAttoreDoc.RICEZIONE);
                                attori.add(creatore);
                                
                                doc.setAttoriList(attori);
                                
                                
                            } else {
                                throw new AbortSaveInterceptorException("Messaggio non trovato");
                            }
                        } else {
                            throw new AbortSaveInterceptorException("Parametri per avviare la protocollazione da pec insufficienti");
                        }
                        break;
                }
            }
        }
        doc.setDataCreazione(ZonedDateTime.now());

        return doc;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity,
            Map<String, String> additionalData,
            HttpServletRequest request,
            boolean mainEntity,
            Class projectionClass) throws AbortSaveInterceptorException {
        Doc doc = (Doc) entity;
        personaVedenteRepository.calcolaPersoneVedenti(doc.getId());
        try {
            if (additionalDatacointainThisOperation(additionalData,
                    AdditionalData.OperationsRequested.CreateDocPerMessageRegistration)) {
                Message pecMittenteMessage = retrieveMessageFromAdditionalData(additionalData);
                doc = scriptaUtils.protocollaMessaggio(doc, pecMittenteMessage);
            }
        } catch (Throwable ex) {
            throw new AbortSaveInterceptorException("Errore nell'allegare la pec", ex);
        }
        if (krintUtils.doIHaveToKrint(request)) {
            krintScriptaService.writeDoc(doc, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_DOC_BY_DI);
        }
        return doc;
    }

    
    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = authenticatedSessionData.getPerson();
        persona = personaRepository.getById(persona.getId());
        
        Doc doc = (Doc) entity;
        Doc docOld = null;

        List<Doc> listDocOld = new ArrayList<>();
        try {
            beforeUpdateEntityApplier.beforeUpdateApply(oldEntity -> {
                Doc docVecchio = (Doc) oldEntity;
                listDocOld.add(docVecchio);
            });
        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity di Doc", ex);
        }
        docOld = listDocOld.get(0);
        
        Versamento.StatoVersamento statoVersamentoNuovo = doc.getStatoVersamento();
        Versamento.StatoVersamento statoVersamentoVecchio = docOld.getStatoVersamento();
        
        if (!statoVersamentoNuovo.equals(statoVersamentoVecchio)) {
            AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
            Map<Integer, Map<String, Object>> aziendeAttiveConParametri = VersatoreServiceUtils.getAziendeAttiveConParametri(parametriAziendaReader, cachedEntities);
            Map<String, Object> versatoreConfigAziendaValue = aziendeAttiveConParametri.get(doc.getIdAzienda().getId());
            String hostId = (String) versatoreConfigAziendaValue.get("hostId");
            Integer threadPoolSize = (Integer) versatoreConfigAziendaValue.get("threadPoolSize");
            Map<String,Object> params = (Map<String,Object>) versatoreConfigAziendaValue.get("params");
            
            switch (statoVersamentoNuovo) {
                case FORZARE:
                {
                    try {
                        accodatoreVeloce.accodaVersatore(
                                Arrays.asList(doc.getId()),
                                doc.getIdAzienda().getId(),
                                hostId,
                                SessioneVersamento.TipologiaVersamento.FORZATURA,
                                persona.getId(),
                                threadPoolSize,
                                params
                        );
                    } catch (MasterjobsWorkerException ex) {
                        throw new AbortSaveInterceptorException("Errore nell'accodamento del job del versamento");
                    }
                }
                    break;

                case ERRORE_RITENTABILE:
                    try {
                        accodatoreVeloce.accodaVersatore(
                                Arrays.asList(doc.getId()),
                                doc.getIdAzienda().getId(),
                                hostId,
                                SessioneVersamento.TipologiaVersamento.RITENTA,
                                persona.getId(),
                                threadPoolSize,
                                params
                        );
                    } catch (MasterjobsWorkerException ex) {
                        throw new AbortSaveInterceptorException("Errore nell'accodamento del job del versamento");
                    }
                    break;
            }
        }
        
        return entity;
    }
}
