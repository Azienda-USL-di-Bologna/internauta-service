package it.bologna.ausl.internauta.service.controllers.tip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vladmihalcea.hibernate.type.range.Range;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.tip.exceptions.TipTransferBadDataException;
import it.bologna.ausl.internauta.service.controllers.tip.exceptions.TipTransferUnexpectedException;
import it.bologna.ausl.internauta.service.controllers.tip.validations.TipDataValidator;
import it.bologna.ausl.internauta.service.utils.FileUtilities;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidoc.CalcolaPersoneVedentiDocJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidoc.CalcolaPersoneVedentiDocJobWorkerData;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPec;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.masterjobs.JobNotified;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocAnnullato;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.DocDoc;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.NotaDoc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.QRegistroDoc;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.scripta.Spedizione.IndirizzoSpedizione;
import it.bologna.ausl.model.entities.tip.DocumentoDaCollegare;
import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.QDocumentoDaCollegare;
import it.bologna.ausl.model.entities.tip.QImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.QSessioneImportazione;
import it.bologna.ausl.model.entities.tip.SessioneImportazione;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneDelibera;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneDetermina;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneProtocolloEntrata;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneProtocolloUscita;
import it.bologna.ausl.model.entities.tip.data.KeyValueEnum;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import it.bologna.ausl.model.entities.versatore.QSessioneVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tika.mime.MimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class TipTransferManager {
    private static final Logger log = LoggerFactory.getLogger(TipTransferManager.class);
    
    private final String CODICE_FISCALE_PERSONA_TIP = "TIP";
    
    private final EntityManager entityManager;
    
    private final ObjectMapper objectMapper;
    
    private final NonCachedEntities nonCachedEntities;
    
    private final ReporitoryConnectionManager repositoryConnectionManager;
    
    private final TransactionTemplate transactionTemplate;
    
    private TipTransferCachedEntities tipTransferCachedEntities;
    
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    private MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    private final ZonedDateTime now = ZonedDateTime.now();

    public TipTransferManager(EntityManager entityManager, ObjectMapper objectMapper, NonCachedEntities nonCachedEntities, 
            ReporitoryConnectionManager reporitoryConnectionManager, TransactionTemplate transactionTemplate, 
            MasterjobsObjectsFactory masterjobsObjectsFactory, MasterjobsJobsQueuer masterjobsJobsQueuer) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.nonCachedEntities = nonCachedEntities;
        this.repositoryConnectionManager = reporitoryConnectionManager;
        this.transactionTemplate = transactionTemplate;
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
        this.tipTransferCachedEntities = new TipTransferCachedEntities(entityManager);
    }
    
    /**
     * Trasferisce la sessione in scripta settando i vari dati nelle tabelle opportune a partire dalle righe di importazione_documenti.
     * Viene assunto che le righe abbiano già eseguito la validazione
     * @param idSessioneImportazione l'id della sessione da trasferire
     * @throws TipTransferUnexpectedException nel caso di un errore imprevisto. In questo caso sarà tutto rollbackato.
     */
    public void transferSessioneDocumento(Long idSessioneImportazione) throws TipTransferUnexpectedException {
        QImportazioneDocumento qImportazioneDocumento = QImportazioneDocumento.importazioneDocumento;
        QSessioneImportazione qSessioneImportazione = QSessioneImportazione.sessioneImportazione;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        transactionTemplate.executeWithoutResult(a -> {
            
            // per prima cosa estraggo le righe ImportazioneDocumento della sessione
            JPAQuery<ImportazioneDocumento> importazioni = queryFactory
                .select(qImportazioneDocumento)
                .from(qImportazioneDocumento)
                .join(qSessioneImportazione).on(qImportazioneDocumento.idSessioneImportazione.id.eq(qSessioneImportazione.id))
                .where(qImportazioneDocumento.idSessioneImportazione.id.eq(idSessioneImportazione).and
                    (qImportazioneDocumento.stato.in(Arrays.asList(ImportazioneDocumento.StatiImportazioneDocumento.IMPORTARE, ImportazioneDocumento.StatiImportazioneDocumento.ANOMALIA_VALIDAZIONE))))
                .fetchAll();
            for (Iterator<ImportazioneDocumento> iterator = importazioni.iterate(); iterator.hasNext();) {
                ImportazioneDocumento importazioneDoc = iterator.next();

                // apro una nuova transazione in modo da salvare lo stato e gli errori dell'importazioneDoc.
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.executeWithoutResult(innerA -> {

                    // apro una ulteriore transazione in modo da salvare il doc se tutto ok, oppure fare il rollback se c'è un errore
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    TipErroriImportazione erroriImportazione = transactionTemplate.execute(innerInnerA -> {
                        
                        SessioneImportazione sessioneImportazione = queryFactory
                            .select(qSessioneImportazione)
                            .from(qSessioneImportazione)
                            .where(qSessioneImportazione.id.eq(importazioneDoc.getIdSessioneImportazione().getId()))
                            .fetchOne();
                        
                        // carico la Persona di sistema che rappresente la persona TIP
                        Persona personaTIP = nonCachedEntities.getPersonaFromCodiceFiscale(CODICE_FISCALE_PERSONA_TIP);
                        Doc doc = new Doc();
                        doc.setIdAzienda(sessioneImportazione.getIdAzienda());
                        doc.setPregresso(true);
                        TipErroriImportazione errori;
                        switch (sessioneImportazione.getTipologia()) {
                            case PROTOCOLLO_IN_ENTRATA:
                                doc.setTipologia(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA);
                                errori = transferPE(doc, personaTIP, sessioneImportazione, importazioneDoc);
                                break;
                            case PROTOCOLLO_IN_USCITA:
                                doc.setTipologia(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA);
                                errori = transferPU(doc, personaTIP, sessioneImportazione, importazioneDoc);
                                break;
                            case DETERMINA:
                                doc.setTipologia(DocDetailInterface.TipologiaDoc.DETERMINA);
                                errori = transferDete(doc, personaTIP, sessioneImportazione, importazioneDoc);
                                break;
                            case DELIBERA:
                                doc.setTipologia(DocDetailInterface.TipologiaDoc.DELIBERA);
                                errori = transferDeli(doc, personaTIP, sessioneImportazione, importazioneDoc);
                                break;
                            default:
                                throw new AssertionError("Funzione non ancora implementata");
                        }
                        
                        // calcolo lo stato di importazione per capirere se posso committare o devo fare il rollback
                        ImportazioneDocumento.StatiImportazioneDocumento statoImportazione = errori.getStatoImportazione(importazioneDoc.getStato());
                        // se lo stato è anomalia o importato committo, altrimenti faccio il rollback
                        if (statoImportazione == ImportazioneDocumento.StatiImportazioneDocumento.ANOMALIA_IMPORTAZIONE || statoImportazione == ImportazioneDocumento.StatiImportazioneDocumento.IMPORTATO) {
                            entityManager.persist(doc);
                            queueJobs(doc.getId());
                        } else {
                            innerInnerA.setRollbackOnly();
                        }
                        return errori;
                    });
                    
                    /*
                    ricalcolo lo stato fuori dalla transazione per poterlo salvare sulla riga di importazioneDoc.
                    Anche se prima ho fatto il rollback sulla riga di importazioneDoc devo scrivere lo stato e gli errori
                    */
                    ImportazioneDocumento.StatiImportazioneDocumento statoImportazione = erroriImportazione.getStatoImportazione(importazioneDoc.getStato());
                    // setto lo stato e gli eventuali errori e warning sulla riga di importazioneDoc
                    importazioneDoc.setStato(statoImportazione);
                    importazioneDoc.setErrori(erroriImportazione);
                    queryFactory
                        .update(qImportazioneDocumento)
                        .set(qImportazioneDocumento.stato, statoImportazione)
                        .set(qImportazioneDocumento.errori, erroriImportazione)
                        .where(qImportazioneDocumento.id.eq(importazioneDoc.getId()))
                        .execute();
                });
            }
        });
    }
    
    /**
     * Accoda il job per il calcolo delle persone vedenti.
     * Il job è accodato tramite la jobs_notify in modo che venga effettivamente accodato dopo il commit
     * @param idDoc 
     */
    private void queueJobs(Integer idDoc) {
        CalcolaPersoneVedentiDocJobWorkerData calcolaPersoneVedentiDocJobWorkerData = new CalcolaPersoneVedentiDocJobWorkerData(idDoc);
        Map calcolaPersoneVedentiDocJobWorkerDataMap = objectMapper.convertValue(calcolaPersoneVedentiDocJobWorkerData, Map.class);
        JobNotified jn = new JobNotified();
        jn.setJobName(CalcolaPersoneVedentiDocJobWorker.class.getSimpleName());
        jn.setJobData(calcolaPersoneVedentiDocJobWorkerDataMap);
        jn.setWaitObject(false);
        entityManager.persist(jn);
    }
    
    /**
     * Trasferisce un PE in scripta.
     * Setta tutti i dati nel doc e torna gli errori di importazione in modo che si possa decidere se committare o fare il rollback.
     * Viene assunto che le righe abbiano già eseguito la validazione
     * @param doc il doc da riempire, va creato fuori e la funzione lo riempie
     * @param persona la persona da usare nei campi in cui è obbligatorio passare una persona e questa non è indicata nel CSV (esempio, personaRegistrante)
     * @param sessioneImportazione la sessioneImportazione che si vuole trasferire
     * @param importazioneDoc la riga ImportazioneDocumento da trasferire
     * @return gli eventuali errori importazione
     */
    private TipErroriImportazione transferPE(Doc doc, Persona persona, SessioneImportazione sessioneImportazione, ImportazioneDocumento importazioneDoc) {

        TipErroriImportazione errori = importazioneDoc.getErrori();
        if (errori == null) {
            errori = new TipErroriImportazione();
        }
        Registro.CodiceRegistro codiceRegistro = getCodiceRegistroDefault(sessioneImportazione.getTipologia());
        
        // controllo se per caso l'ho già trasferito (cercandolo per registro, numero e anno) e nel caso lo indico come waring e lo salto
        if (isDocAlreadyPresent(
                sessioneImportazione.getTipologia(),
                sessioneImportazione.getIdAzienda(),
                Integer.valueOf(importazioneDoc.getNumero()),
                importazioneDoc.getAnno())) {
            importazioneDoc.setStato(ImportazioneDocumento.StatiImportazioneDocumento.GIA_IMPORTATO);
            errori.setWarning(ColonneProtocolloEntrata.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
            errori.setWarning(ColonneProtocolloEntrata.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
            errori.setWarning(ColonneProtocolloEntrata.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
        } else { // se non l'ho già trasferito procedo al trasferimento

            try {
                /* importazione campi della registrazione:
                * registro, numero, anno, dataRegistrazione/dataAdozione e adottatoDa
                */
                transferRegistrazione(doc, ColonneProtocolloEntrata.registro, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdStrutturaDefault(), codiceRegistro, importazioneDoc);
            } catch (TipTransferBadDataException ex) {
                log.error("errore nel trasferimento dei dati di registrazione", ex);
                errori.setError(ColonneProtocolloEntrata.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                errori.setError(ColonneProtocolloEntrata.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                errori.setError(ColonneProtocolloEntrata.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
            }

            // trasferisco i related (mittenti e destinatari)

            // creo la lista di mittenti
            List<Related> mittenti = buildRelated(
                doc,
                persona, 
                sessioneImportazione.getIdAzienda(),
                false, 
                importazioneDoc, 
                Related.TipoRelated.MITTENTE, 
                ColonneProtocolloEntrata.mittente, 
                ColonneProtocolloEntrata.indirizzoMittente, 
                null);
//                    try {
//                    } catch (Throwable ex) {
//                        log.error("errore nel trasferimento del mittente", ex);
//                        errori.setError(ColonneProtocolloEntrata.mittente, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
//                        errori.setError(ColonneProtocolloEntrata.indirizzoMittente, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
////                        errori.setError(ColonneProtocolloEntrata.mezzo, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
//                    }

            // creo la lista di destinatari A (nel caso PE sono solo strutture per cui passo true al parametro soloStrutture)
            List<Related>destinatariA = buildRelated(
                doc,
                persona,
                sessioneImportazione.getIdAzienda(),
                true,
                importazioneDoc,
                Related.TipoRelated.A,
                ColonneProtocolloEntrata.destinatariInterniA,
                null, 
                null);
//                    try {
//                    }  catch (Throwable ex) {
//                        log.error("errore nel trasferimento dei destinatari interni A", ex);
//                        errori.setError(ColonneProtocolloEntrata.destinatariInterniA, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
//                    }
//                  
            // creo la lista di destinatari CC (nel caso PE sono solo strutture per cui passo true al parametro soloStrutture)
            List<Related> destinatariCC = buildRelated(
                doc,
                persona,
                sessioneImportazione.getIdAzienda(),
                true,
                importazioneDoc,
                Related.TipoRelated.CC,
                ColonneProtocolloEntrata.destinatariInterniCC,
                null, 
                null);
//                    try {
//                    } catch (Throwable ex) {
//                        log.error("errore nel trasferimento dei destinatari interni CC", ex);
//                        errori.setError(ColonneProtocolloEntrata.destinatariInterniA, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
//                    }

            // setto i related sul documento
            List<Related> related = doc.getRelated();
            if (mittenti != null || destinatariA != null || destinatariCC != null) {
                if (related == null) {
                    related = new ArrayList<>();
                    doc.setRelated(related);
                }
                doc.setRelated(related);
                if (mittenti != null) {
                    related.addAll(mittenti);
                }
                if (destinatariA != null) {
                    related.addAll(destinatariA);
                }
                if (destinatariCC != null) {
                    related.addAll(destinatariCC);
                }
            }

            addInAdditionalData(doc, ColonneProtocolloEntrata.protocolloEsterno, importazioneDoc.getProtocolloEsterno());
            addInAdditionalData(doc, ColonneProtocolloEntrata.dataProtocolloEsterno, importazioneDoc.getDataProtocolloEsterno());
            transferVisiblita(doc, importazioneDoc);
            doc.setOggetto(importazioneDoc.getOggetto());
            try {
                transferFascicolazione(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdArchivioDefault(), persona);
            } catch (TipTransferBadDataException ex) {
                log.error("errore nel trasferimento delle fascicolazioni", ex);
                errori.setError(ColonneProtocolloEntrata.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
            }
            addInAdditionalData(doc, ColonneProtocolloEntrata.classificazione, importazioneDoc.getClassificazione());
            transferAllegati(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), codiceRegistro);
            transferPrecedente(doc, sessioneImportazione, importazioneDoc, persona);
            transferAnnullamento(doc, importazioneDoc, persona);
            transferVersamento(doc, importazioneDoc, sessioneImportazione.getIdAzienda());
            transferNoteDocumento(doc, importazioneDoc, persona);
        }
        return errori;
    }
    
    /**
     * Trasferisce un PU in scripta.
     * Setta tutti i dati nel doc e torna gli errori di importazione in modo che si possa decidere se committare o fare il rollback.
     * Viene assunto che le righe abbiano già eseguito la validazione
     * @param doc il doc da riempire, va creato fuori e la funzione lo riempie
     * @param persona la persona da usare nei campi in cui è obbligatorio passare una persona e questa non è indicata nel CSV (esempio, personaRegistrante)
     * @param sessioneImportazione la sessioneImportazione che si vuole trasferire
     * @param importazioneDoc la riga ImportazioneDocumento da trasferire
     * @return gli eventuali errori importazione
     */
    private TipErroriImportazione transferPU(Doc doc, Persona persona, SessioneImportazione sessioneImportazione, ImportazioneDocumento importazioneDoc) {

        TipErroriImportazione errori = importazioneDoc.getErrori();
        if (errori == null) {
            errori = new TipErroriImportazione();
        }
        Registro.CodiceRegistro codiceRegistro = getCodiceRegistroDefault(sessioneImportazione.getTipologia());
        
        // controllo se per caso l'ho già trasferito (cercandolo per registro, numero e anno) e nel caso lo indico come waring e lo salto
        if (isDocAlreadyPresent(
                sessioneImportazione.getTipologia(),
                sessioneImportazione.getIdAzienda(),
                Integer.valueOf(importazioneDoc.getNumero()),
                importazioneDoc.getAnno())) {
            errori.setWarning(ColonneProtocolloUscita.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
            errori.setWarning(ColonneProtocolloUscita.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
            errori.setWarning(ColonneProtocolloUscita.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
        } else { // se non l'ho già trasferito procedo al trasferimento

            try {
                /* importazione campi della registrazione:
                * registro, numero, anno, dataRegistrazione/dataAdozione e adottatoDa
                */
                transferRegistrazione(doc, ColonneProtocolloUscita.registro, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdStrutturaDefault(), codiceRegistro, importazioneDoc);
            } catch (TipTransferBadDataException ex) {
                log.error("errore nel trasferimento dei dati di registrazione", ex);
                errori.setError(ColonneProtocolloUscita.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                errori.setError(ColonneProtocolloUscita.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                errori.setError(ColonneProtocolloUscita.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
            }

            // trasferisco i related (destinatari)

            // creo la lista di destinatari A (nel caso PU sono sia strutture che contatti per cui passo false al parametro soloStrutture)
            List<Related>destinatariA = buildRelated(
                doc,
                persona,
                sessioneImportazione.getIdAzienda(),
                false,
                importazioneDoc,
                Related.TipoRelated.A,
                ColonneProtocolloUscita.destinatariPrincipali,
                ColonneProtocolloUscita.indirizziDestinatariPrincipali, 
                ColonneProtocolloUscita.descrizioneIndirizziPrincipali);

            // creo la lista di destinatari CC (nel caso PU sono sia strutture che contatti per cui passo false al parametro soloStrutture)
            List<Related> destinatariCC = buildRelated(
                doc,
                persona,
                sessioneImportazione.getIdAzienda(),
                false,
                importazioneDoc,
                Related.TipoRelated.CC,
                ColonneProtocolloUscita.altriDestinatari,
                ColonneProtocolloUscita.indirizziAltriDestinatari, 
                ColonneProtocolloUscita.descrizioneAltriIndirizzi);
            // setto i related sul documento
            List<Related> related = doc.getRelated();
            if (destinatariA != null || destinatariCC != null) {
                if (related == null) {
                    related = new ArrayList<>();
                    doc.setRelated(related);
                }
                if (destinatariA != null) {
                    related.addAll(destinatariA);
                }
                if (destinatariCC != null) {
                    related.addAll(destinatariCC);
                }
            }

            transferVisiblita(doc, importazioneDoc);
            doc.setOggetto(importazioneDoc.getOggetto());
            try {
                transferFascicolazione(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdArchivioDefault(), persona);
            } catch (TipTransferBadDataException ex) {
                log.error("errore nel trasferimento delle fascicolazioni", ex);
                errori.setError(ColonneProtocolloUscita.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
            }
            addInAdditionalData(doc, ColonneProtocolloUscita.classificazione, importazioneDoc.getClassificazione());
            transferAllegati(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), codiceRegistro);
            transferPrecedente(doc, sessioneImportazione, importazioneDoc, persona);
            transferAnnullamento(doc, importazioneDoc, persona);
            transferVersamento(doc, importazioneDoc, sessioneImportazione.getIdAzienda());
            transferNoteDocumento(doc, importazioneDoc, persona);
            transferAttori(doc, importazioneDoc, sessioneImportazione.getIdAzienda());
            transferPecMittente(doc, importazioneDoc);
        }
        return errori;
    }
    
    /**
     * Trasferisce una determina in scripta.
     * Setta tutti i dati nel doc e torna gli errori di importazione in modo che si possa decidere se committare o fare il rollback.
     * Viene assunto che le righe abbiano già eseguito la validazione
     * @param doc il doc da riempire, va creato fuori e la funzione lo riempie
     * @param persona la persona da usare nei campi in cui è obbligatorio passare una persona e questa non è indicata nel CSV (esempio, personaRegistrante)
     * @param sessioneImportazione la sessioneImportazione che si vuole trasferire
     * @param importazioneDoc la riga ImportazioneDocumento da trasferire
     * @return gli eventuali errori importazione
     */
    private TipErroriImportazione transferDete(Doc doc, Persona persona, SessioneImportazione sessioneImportazione, ImportazioneDocumento importazioneDoc) {

        TipErroriImportazione errori = importazioneDoc.getErrori();
        if (errori == null) {
            errori = new TipErroriImportazione();
        }
        
        Registro.CodiceRegistro codiceRegistro = getCodiceRegistroDefault(sessioneImportazione.getTipologia());
        // controllo se per caso l'ho già trasferito (cercandolo per registro, numero e anno) e nel caso lo indico come waring e lo salto
        if (isDocAlreadyPresent(
                sessioneImportazione.getTipologia(),
                sessioneImportazione.getIdAzienda(),
                Integer.valueOf(importazioneDoc.getNumero()),
                importazioneDoc.getAnno())) {
            errori.setWarning(ColonneDetermina.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
            errori.setWarning(ColonneDetermina.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
            errori.setWarning(ColonneDetermina.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
        } else { // se non l'ho già trasferito procedo al trasferimento

            try {
                /* 
                * importazione campi della registrazione:
                * registro, numero, anno, dataRegistrazione/dataAdozione e adottatoDa
                */
                transferRegistrazione(doc, ColonneDetermina.registro, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdStrutturaDefault(), codiceRegistro, importazioneDoc);
            } catch (TipTransferBadDataException ex) {
                log.error("errore nel trasferimento dei dati di registrazione", ex);
                errori.setError(ColonneDetermina.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                errori.setError(ColonneDetermina.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                errori.setError(ColonneDetermina.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
            }

            // trasferisco i related (destinatari)

            /*
            * creo la lista di destinatari interni.
            * nel caso Dete sono solo strutture,  per cui passo false al parametro soloStrutture, inoltre solo solo A, perché in Dete nonc i sono i CC
            */
            List<Related>destinatariInterni = buildRelated(
                doc,
                persona,
                sessioneImportazione.getIdAzienda(),
                true,
                importazioneDoc,
                Related.TipoRelated.A,
                ColonneDetermina.destinatariInterni,
                null, 
                null);

            /*
            * creo la lista di destinatari esterni.
            * nel caso Dete sono solo contatti,  per cui passo false al parametro soloStrutture, inoltre solo solo A, perché in Dete nonc i sono i CC
            */
            List<Related> destinatariEsterni = buildRelated(
                doc,
                persona,
                sessioneImportazione.getIdAzienda(),
                false,
                importazioneDoc,
                Related.TipoRelated.A,
                ColonneDetermina.nomiDestinatariEsterni,
                ColonneDetermina.indirizziDestinatariEsterni, 
                null);
            // setto i related sul documento
            List<Related> related = doc.getRelated();
            if (destinatariInterni != null || destinatariEsterni != null) {
                if (related == null) {
                    related = new ArrayList<>();
                    doc.setRelated(related);
                }
                if (destinatariInterni != null) {
                    related.addAll(destinatariInterni);
                }
                if (destinatariEsterni != null) {
                    related.addAll(destinatariEsterni);
                }
            }

            transferVisiblita(doc, importazioneDoc);
            doc.setOggetto(importazioneDoc.getOggetto());
            try {
                transferFascicolazione(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdArchivioDefault(), persona);
            } catch (TipTransferBadDataException ex) {
                log.error("errore nel trasferimento delle fascicolazioni", ex);
                errori.setError(ColonneDetermina.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
            }
            addInAdditionalData(doc, ColonneDetermina.classificazione, importazioneDoc.getClassificazione());
            transferAllegati(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), codiceRegistro);
            transferPrecedente(doc, sessioneImportazione, importazioneDoc, persona);
            transferAnnullamento(doc, importazioneDoc, persona);
            transferVersamento(doc, importazioneDoc, sessioneImportazione.getIdAzienda());
            transferNoteDocumento(doc, importazioneDoc, persona);
            transferAttori(doc, importazioneDoc, sessioneImportazione.getIdAzienda());
            transferPubblicazioni(doc, importazioneDoc);
        }
        return errori;
    }
    
    /**
     * Trasferisce una delibera in scripta.
     * Setta tutti i dati nel doc e torna gli errori di importazione in modo che si possa decidere se committare o fare il rollback.
     * Viene assunto che le righe abbiano già eseguito la validazione
     * @param doc il doc da riempire, va creato fuori e la funzione lo riempie
     * @param persona la persona da usare nei campi in cui è obbligatorio passare una persona e questa non è indicata nel CSV (esempio, personaRegistrante)
     * @param sessioneImportazione la sessioneImportazione che si vuole trasferire
     * @param importazioneDoc la riga ImportazioneDocumento da trasferire
     * @return gli eventuali errori importazione
     */
    private TipErroriImportazione transferDeli(Doc doc, Persona persona, SessioneImportazione sessioneImportazione, ImportazioneDocumento importazioneDoc) {

        TipErroriImportazione errori = importazioneDoc.getErrori();
        if (errori == null) {
            errori = new TipErroriImportazione();
        }
        Registro.CodiceRegistro codiceRegistro = getCodiceRegistroDefault(sessioneImportazione.getTipologia());
        
        // controllo se per caso l'ho già trasferito (cercandolo per registro, numero e anno) e nel caso lo indico come waring e lo salto
        if (isDocAlreadyPresent(
                sessioneImportazione.getTipologia(),
                sessioneImportazione.getIdAzienda(),
                Integer.valueOf(importazioneDoc.getNumero()),
                importazioneDoc.getAnno())) {
            errori.setWarning(ColonneDelibera.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
            errori.setWarning(ColonneDelibera.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
            errori.setWarning(ColonneDelibera.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
        } else { // se non l'ho già trasferito procedo al trasferimento

            try {
                /* 
                * importazione campi della registrazione:
                * registro, numero, anno, dataRegistrazione/dataAdozione e adottatoDa
                */
                transferRegistrazione(doc, ColonneDelibera.registro, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdStrutturaDefault(), codiceRegistro, importazioneDoc);
            } catch (TipTransferBadDataException ex) {
                log.error("errore nel trasferimento dei dati di registrazione", ex);
                errori.setError(ColonneDelibera.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                errori.setError(ColonneDelibera.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                errori.setError(ColonneDelibera.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
            }

            // trasferisco i related (destinatari)

            /*
            * creo la lista di destinatari interni.
            * nel caso Dete sono solo strutture,  per cui passo false al parametro soloStrutture, inoltre solo solo A, perché in Dete nonc i sono i CC
            */
            List<Related>destinatariInterni = buildRelated(
                doc,
                persona,
                sessioneImportazione.getIdAzienda(),
                true,
                importazioneDoc,
                Related.TipoRelated.A,
                ColonneDelibera.destinatariInterni,
                null, 
                null);

            /*
            * creo la lista di destinatari esterni.
            * nel caso Dete sono solo contatti,  per cui passo false al parametro soloStrutture, inoltre solo solo A, perché in Dete nonc i sono i CC
            */
            List<Related> destinatariEsterni = buildRelated(
                doc,
                persona,
                sessioneImportazione.getIdAzienda(),
                false,
                importazioneDoc,
                Related.TipoRelated.A,
                ColonneDelibera.nomiDestinatariEsterni,
                ColonneDelibera.indirizziDestinatariEsterni, 
                null);
            // setto i related sul documento
            List<Related> related = doc.getRelated();
            if (destinatariInterni != null || destinatariEsterni != null) {
                if (related == null) {
                    related = new ArrayList<>();
                    doc.setRelated(related);
                }
                if (destinatariInterni != null) {
                    related.addAll(destinatariInterni);
                }
                if (destinatariEsterni != null) {
                    related.addAll(destinatariEsterni);
                }
            }

            transferVisiblita(doc, importazioneDoc);
            doc.setOggetto(importazioneDoc.getOggetto());
            try {
                transferFascicolazione(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdArchivioDefault(), persona);
            } catch (TipTransferBadDataException ex) {
                log.error("errore nel trasferimento delle fascicolazioni", ex);
                errori.setError(ColonneDelibera.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
            }
            addInAdditionalData(doc, ColonneDelibera.classificazione, importazioneDoc.getClassificazione());
            transferAllegati(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), codiceRegistro);
            transferPrecedente(doc, sessioneImportazione, importazioneDoc, persona);
            transferAnnullamento(doc, importazioneDoc, persona);
            transferVersamento(doc, importazioneDoc, sessioneImportazione.getIdAzienda());
            transferNoteDocumento(doc, importazioneDoc, persona);
            transferAttori(doc, importazioneDoc, sessioneImportazione.getIdAzienda());
            transferPubblicazioni(doc, importazioneDoc);
        }
        return errori;
    }
    
    /**
     * Inserisce i dati di pubblicazione all'albo in addizionalData nella chiave dati_pubblicazione di additionalData
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     */
    public Doc transferPubblicazioni(Doc doc, ImportazioneDocumento importazioneDocumento) {
        if (
                StringUtils.hasText(importazioneDocumento.getNumeroPubblicazione()) || 
                StringUtils.hasText(importazioneDocumento.getAnnoPubblicazione()) ||
                StringUtils.hasText(importazioneDocumento.getDataInizio()) || 
                StringUtils.hasText(importazioneDocumento.getDataFine())
                ) {
            if (doc.getAdditionalData() == null) {
                doc.setAdditionalData(new HashMap<>());
            }
            Map<String, Object> datiPubblicazione = new HashMap();
            if (StringUtils.hasText(importazioneDocumento.getNumeroPubblicazione()))
                datiPubblicazione.put("numero", importazioneDocumento.getNumeroPubblicazione());
            if (StringUtils.hasText(importazioneDocumento.getAnnoPubblicazione()))
                datiPubblicazione.put("anno", importazioneDocumento.getAnnoPubblicazione());
            if (StringUtils.hasText(importazioneDocumento.getDataInizio()))
                datiPubblicazione.put("inizio_pubblicazione", TipDataValidator.dateToISOLocalDateTimeString(TipDataValidator.parseData(importazioneDocumento.getDataInizio())));
            if (StringUtils.hasText(importazioneDocumento.getDataFine()))
                datiPubblicazione.put("fine_pubblicazione", TipDataValidator.dateToISOLocalDateTimeString(TipDataValidator.parseData(importazioneDocumento.getDataFine())));
            if (StringUtils.hasText(importazioneDocumento.getDataEsecutivita()))
                datiPubblicazione.put("data_esecutivita", TipDataValidator.dateToISOLocalDateTimeString(TipDataValidator.parseData(importazioneDocumento.getDataEsecutivita())));
            else
                datiPubblicazione.put("data_esecutivita",
                    StringUtils.hasText(importazioneDocumento.getDataRegistrazione()) ?  
                        TipDataValidator.dateToISOLocalDateTimeString(TipDataValidator.parseData(importazioneDocumento.getDataRegistrazione())): 
                        TipDataValidator.dateToISOLocalDateTimeString(TipDataValidator.parseData(importazioneDocumento.getDataAdozione()))
                );
            doc.getAdditionalData().put("dati_pubblicazione", datiPubblicazione);
            
        }
        return doc;
    }
    
    /**
     * Aggiunge la pec mittente sul doc, se questa viene trovata
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     */
    public Doc transferPecMittente(Doc doc, ImportazioneDocumento importazioneDocumento) {
        if (StringUtils.hasText(importazioneDocumento.getPecMittente())) {
            QPec qPec = QPec.pec;
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
            Pec pec = jPAQueryFactory
                .select(qPec)
                .from(qPec)
                .where(qPec.indirizzo.equalsIgnoreCase(importazioneDocumento.getPecMittente()))
                .fetchOne();
            if (pec != null) {
                doc.setIdPecMittente(pec);
            }
        }
        return doc;
    }
    
    /**
     * Crea gli attori sul doc, a seconda della tipologia del doc che si sta trasferendo gli attori possono essere diversi.
     * Questa funzione a importa tutti gli attori definiti nei vari campi attori di importazioneDocumento, non considerandone la tipologia, in quanto se per la tipologia
     * che si sta importando un attore non è previsto su importazioneDocumento il campo corrispondente non sarà popolato
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param azienda l'azienda sulla quale si sta eseguendo l'importazione 
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     */
    public Doc transferAttori(Doc doc, ImportazioneDocumento importazioneDocumento, Azienda azienda) {
        List<AttoreDoc> attori = new ArrayList();
        List<Pair<String, AttoreDoc.RuoloAttoreDoc>> attoreStringAndRuoloList = new ArrayList<>();
        
        if (StringUtils.hasText(importazioneDocumento.getRedattore())) {
            for (String attoreString : importazioneDocumento.getRedattore().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.REDAZIONE));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getPareri())) {
            for (String attoreString : importazioneDocumento.getPareri().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.PARERI));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getFirmatari())) {
            for (String attoreString : importazioneDocumento.getFirmatari().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.FIRMA));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getFirmatario())) {
            for (String attoreString : importazioneDocumento.getFirmatario().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.FIRMA));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getProponente())) {
            for (String attoreString : importazioneDocumento.getProponente().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.FIRMA));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getVisto())) {
            for (String attoreString : importazioneDocumento.getVisto().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.VISTI));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getDirettoreAmministrativo())) {
            for (String attoreString : importazioneDocumento.getDirettoreAmministrativo().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.DIRETTORE_AMMINISTRATIVO));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getDirettoreSanitario())) {
            for (String attoreString : importazioneDocumento.getDirettoreSanitario().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.DIRETTORE_SANITARIO));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getDirettoreGenerale())) {
            for (String attoreString : importazioneDocumento.getDirettoreGenerale().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.DIRETTORE_GENERALE));
            }
        }
        if (StringUtils.hasText(importazioneDocumento.getVicarioDirettoreGenerale())) {
            for (String attoreString : importazioneDocumento.getVicarioDirettoreGenerale().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {  
                attoreStringAndRuoloList.add(Pair.of(attoreString, AttoreDoc.RuoloAttoreDoc.VICARIO_DIRETTORE_GENERALE));
            }
        }
        
        for (Pair<String, AttoreDoc.RuoloAttoreDoc> attoreStringAndRuolo : attoreStringAndRuoloList) {
            String attoreString = attoreStringAndRuolo.getFirst();
            String cf = null;
            String cognome = null;
            String nome = null;
            String indefinito = null;
            if (attoreString.contains(ImportazioneDocumento.DEFAULT_ATTORE_SEPARATOR)) {
                String[] attoreStringSplitted = attoreString.split(ImportazioneDocumento.DEFAULT_ATTORE_SEPARATOR);
                if (StringUtils.hasText(attoreStringSplitted[0])) {
                    cf = attoreStringSplitted[0];
                }
                if (StringUtils.hasText(attoreStringSplitted[1])) {
                    cognome = attoreStringSplitted[1];
                }
                if (StringUtils.hasText(attoreStringSplitted[2])) {
                    nome = attoreStringSplitted[2];
                }
            } else {
                indefinito = attoreString;
            }
            
            Persona persona = findOrCreatePersona(cf, cognome, nome, indefinito, azienda);
            AttoreDoc attoreDoc = new AttoreDoc();
            attoreDoc.setIdDoc(doc);
            attoreDoc.setIdPersona(persona);
            attoreDoc.setRuolo(attoreStringAndRuolo.getSecond());
            attori.add(attoreDoc);
        }
        if (!attori.isEmpty()) {
            if (doc.getAttoriList() == null) {
                doc.setAttoriList(attori);
            } else {
                doc.getAttoriList().addAll(attori);
            }
        }
        
        return doc;
    }
    
    
    /**
     * Se è presente la data di invio conservazione nei dati del CSV, crea il versamento e gliela setta.
     * Il versamento verrà inserito in una sessione apposista, con tipologia IMPORTATA. Se la sessione non esiste già la crea.
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param azienda l'azienda sulla quale si sta eseguendo l'importazione 
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     * @throws TipTransferUnexpectedException in caso venga trovata più di una sessione versamento di tipologia importata
     */
    private Doc transferVersamento(Doc doc, ImportazioneDocumento importazioneDocumento, Azienda azienda) throws TipTransferUnexpectedException {
        if (StringUtils.hasText(importazioneDocumento.getDataInvioConservazione())) {
            SessioneVersamento sessioneVersamento;
            QSessioneVersamento qSessioneVersamento = QSessioneVersamento.sessioneVersamento;
            
            // vedo se esiste già una sessione versamento di tipo IMPORTATA, che è quella in cui inseriamo tutti i versamenti dei documenti importati
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            List<SessioneVersamento> sessioniVersamento = queryFactory
                .select(qSessioneVersamento)
                .from(qSessioneVersamento)
                .where(qSessioneVersamento.tipologia.eq(SessioneVersamento.TipologiaVersamento.IMPORTATA))
                .fetch();
            // se non c'è allora la creo chiusa
            if (sessioniVersamento == null || sessioniVersamento.isEmpty()) {
                sessioneVersamento = new SessioneVersamento();
                sessioneVersamento.setTipologia(SessioneVersamento.TipologiaVersamento.IMPORTATA);
                sessioneVersamento.setStato(SessioneVersamento.StatoSessioneVersamento.DONE);
                sessioneVersamento.setIdAzienda(azienda);
                sessioneVersamento.setTimeInterval(Range.open(now, now));
                //entityManager.persist(sessioneVersamento);
            } else if (sessioniVersamento.size() == 1) {
                // se c'è ed è una la prendo
                sessioneVersamento = sessioniVersamento.get(0);
            } else {
                // se ne trovo più di una c'è qualcosa che non va e torno errore
                String errorMessage = String.format("sono state trovate %s sessioni versamento con tipologia %s. Ce ne dovrebbe essere solo una", 
                        sessioniVersamento.size(), SessioneVersamento.TipologiaVersamento.IMPORTATA);
                throw new TipTransferUnexpectedException(errorMessage);
            }

            // creo l'oggetto versamento settandoci la data letta dal CSV
            Versamento versamento = new Versamento();
            versamento.setIdDoc(doc);
            versamento.setIdSessioneVersamento(sessioneVersamento);
            versamento.setDataInserimento(TipDataValidator.parseData(importazioneDocumento.getDataInvioConservazione()).atStartOfDay(ZoneId.systemDefault()));
            versamento.setIgnora(true);
            versamento.setForzabile(false);
            versamento.setStato(Versamento.StatoVersamento.VERSATO);
            versamento.setForzabileConcordato(false);
            if (doc.getVersamentiList() == null) {
                doc.setVersamentiList(new ArrayList<>());
            }
            doc.getVersamentiList().add(versamento);
            //entityManager.persist(versamento);
        }
        return doc;
    }
    
    /**
     * setta la nota del documento creando la riga in scripta.note_doc
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param persona la persona da inserire come persona che ha inserito la nota
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     */
    private Doc transferNoteDocumento(Doc doc, ImportazioneDocumento importazioneDocumento, Persona persona) {
        if (StringUtils.hasText(importazioneDocumento.getNote()) || StringUtils.hasText(importazioneDocumento.getAnnotazioni())) {
            if (doc.getNotaDocList() == null) {
                doc.setNotaDocList(new ArrayList<>());
            }
            if (StringUtils.hasText(importazioneDocumento.getNote())) {
                NotaDoc notaDoc = new NotaDoc(doc, persona, NotaDoc.TipoNotaDoc.DOCUMENTO, importazioneDocumento.getNote(), now);
                doc.getNotaDocList().add(notaDoc);
            }
            if (StringUtils.hasText(importazioneDocumento.getAnnotazioni())) {
                NotaDoc notaDoc = new NotaDoc(doc, persona, NotaDoc.TipoNotaDoc.DOCUMENTO, importazioneDocumento.getAnnotazioni(), now);
                doc.getNotaDocList().add(notaDoc);
            }
        }
        return doc;
    }
    
    /**
     * setta l'annullamento del doc, se è da settare (cioè se in importazioneDocumento.getAnnullato() è true)
     * Per farlo crea la riga in scripta.docs_annullati per indicare lo stato di annullamento del documento
     *  se presente nel CSV setta la data, altrimento mette la data attuale
     *  se presente nel CSV setta la nota di annullamento, creando la riga in scripta.note_doc
     * setta anche la data annullamento, se presente nel CSV
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param persona la persona da inserire come persona che ha inserito la nota
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     */
    private Doc transferAnnullamento(Doc doc, ImportazioneDocumento importazioneDocumento, Persona persona) {
        if (StringUtils.hasText(importazioneDocumento.getAnnullato()) && TipDataValidator.parseBoolean(importazioneDocumento.getAnnullato())) {
            DocAnnullato docAnnullato = new DocAnnullato();
            docAnnullato.setIdDoc(doc);
            docAnnullato.setTipo(DocAnnullato.TipoAnnullamento.ANNULLATO);
            docAnnullato.setIdPersonaAnnullante(persona);
            ZonedDateTime dataAnnullamento;
            if (StringUtils.hasText(importazioneDocumento.getDataAnnullamento())) {
                dataAnnullamento = TipDataValidator.parseData(importazioneDocumento.getDataAnnullamento()).atStartOfDay(ZoneId.systemDefault());
            } else {
                dataAnnullamento = now;
            }
            docAnnullato.setData(dataAnnullamento);
            
            if (StringUtils.hasText(importazioneDocumento.getNoteAnnullamento())) {
                NotaDoc notaDoc = new NotaDoc(doc, persona, NotaDoc.TipoNotaDoc.ANNULLAMENTO, importazioneDocumento.getNoteAnnullamento(), dataAnnullamento);
                docAnnullato.setIdNota(notaDoc);
//                if (doc.getNotaDocList() == null) {
//                    doc.setNotaDocList(new ArrayList<>());
//                }
//                doc.getNotaDocList().add(notaDoc);
            }
        }
        return doc;
    }
    
    /**
     * Setta il collegamento con il precedente. Procede in questo modo:
     * Se il doc in esame era un precedente di un doc precedentemente importato, ma che non poteva collegare perché ancora non c'era viene collegato 
     *  (Questa informazione è scritta nella tabella DocumentoDaCollegare)
     * Poi viene cercato il doc precedente indicato:
     *  - Se lo trova lo collega
     *  - Se non lo trova lo scrive nella tabella DocumentoDaCollegare per collegarlo successivamente, quando il doc interessato sarà importato
     * @param doc il doc
     * @param sessioneImportazione
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param persona la persona da inserire come persona che ha collegato
     * @return il doc che è lo stesso in input, utile per poter concatenare il metodo a qualcos altro
     *  devo tornare anche il DocumentoDaCollegare perché nel caso l'importazione è andata a buon fine devo fare il persist
     * @throws TipTransferBadDataException se c'è qualche errore da segnalare all'utente
     */
    private Doc transferPrecedente(Doc doc, SessioneImportazione sessioneImportazione, ImportazioneDocumento importazioneDocumento, Persona persona) throws TipTransferUnexpectedException {
        // per poter settare il precedente il doc deve avere la riga in registri_docs con il numero, se non c'è devo dare errore
        if (StringUtils.hasText(importazioneDocumento.getCollegamentoPrecedente()) && (doc.getRegistroDocList() == null || doc.getRegistroDocList().isEmpty())) {
            String errorMessage = "impossibile inserire il precedente perché c'è un errore nell'importazione del registro";
            log.error(errorMessage);
            throw new TipTransferUnexpectedException(errorMessage);
        }
        
        QDocumentoDaCollegare qDocumentoDaCollegare = QDocumentoDaCollegare.documentoDaCollegare;
        QDoc qDoc = QDoc.doc;
        QRegistroDoc qRegistroDoc = QRegistroDoc.registroDoc;
        RegistroDoc registroDoc = doc.getRegistroDocList().get(0);
        
        /*
        per prima cosa controllo se il doc era un precedente di un doc precedentemente importato, ma che non poteva collegare perché ancora non c'era ancora.
        Per farlo cerco il doc (per registro numero e anno) nella tabella Documenti_da_collegare, usando le colonne destinazione 
        (id_registro_destinazione, numero_destinazione, anno_destinazione)
        */
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<DocumentoDaCollegare> documentiDestinazione = queryFactory
            .select(qDocumentoDaCollegare)
            .from(qDocumentoDaCollegare)
            .where(
                qDocumentoDaCollegare.idRegistroDestinazione.id.eq(registroDoc.getIdRegistro().getId())
                    .and(
                        qDocumentoDaCollegare.numeroDestinazione.eq(registroDoc.getNumero()))
                    .and(
                        qDocumentoDaCollegare.annoDestinazione.eq(registroDoc.getAnno()))
                    .and(
                        qDocumentoDaCollegare.tipoCollegamento.eq(DocDoc.TipoCollegamentoDoc.PRECEDENTE))
            )
            .fetch();
        /*
        Se lo trovo (dovrei trovarlo) vuol dire che questo doc doveva essere il suo precedente, per cui lo carico e creo il collegamento
        */
        if (documentiDestinazione != null && !documentiDestinazione.isEmpty()) {
            for (DocumentoDaCollegare documentoDestinazione : documentiDestinazione) {
                Doc docSorgente = queryFactory
                    .select(qRegistroDoc.idDoc)
                    .from(qRegistroDoc)
                    .join(qDoc).on(qRegistroDoc.idDoc.id.eq(qDoc.id))
                    .where( qRegistroDoc.idRegistro.id.eq(documentoDestinazione.getIdRegistroSorgente().getId())
                            .and(
                                    qRegistroDoc.numero.eq(documentoDestinazione.getNumeroSorgente()))
                            .and(
                                    qRegistroDoc.anno.eq(documentoDestinazione.getAnnoSorgente()))
                    )
                    .fetchOne();
                if (docSorgente.getDocsCollegati() == null) {
                    List docsCollegati = new ArrayList();
                    docSorgente.setDocsCollegati(docsCollegati);
                }
                docSorgente.getDocsCollegati().add(new DocDoc(docSorgente, doc, DocDoc.TipoCollegamentoDoc.PRECEDENTE, persona));
            }
            // salvare?
        }
        
        if (StringUtils.hasText(importazioneDocumento.getCollegamentoPrecedente())) {
            for (String precedente: importazioneDocumento.getCollegamentoPrecedente().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {
                // Poi passo a cercare e collegare a questo doc il suo precedente
                String[] precedenteSplitted = precedente.split("/");
                Integer numeroPrecedente = Integer.valueOf(precedenteSplitted[0]);
                Integer annoPrecedente = Integer.valueOf(precedenteSplitted[1]);
                // lo cerco nel db
                Doc docDestinazione = queryFactory
                    .select(qRegistroDoc.idDoc)
                    .from(qRegistroDoc)
                    .join(qDoc).on(qRegistroDoc.idDoc.id.eq(qDoc.id))
                    .where( qRegistroDoc.idRegistro.id.eq(registroDoc.getIdRegistro().getId())
                        .and(
                            qRegistroDoc.numero.eq(numeroPrecedente))
                        .and(
                            qRegistroDoc.anno.eq(annoPrecedente)
                        )
                    )
                    .fetchOne();

                DocumentoDaCollegare documentoDaCollegare = null;
                // se lo trovo allora creo il collegamento
                if (docDestinazione != null) {
                    if (doc.getDocsCollegati() == null) {
                        List docsCollegati = new ArrayList();
                        doc.setDocsCollegati(docsCollegati);
                    }
                    doc.getDocsCollegati().add(new DocDoc(doc, docDestinazione, DocDoc.TipoCollegamentoDoc.PRECEDENTE, persona));
                } else {
                    /* 
                    se non lo trovo lo inserisco nella tabella documenti_da_collegare.
                    Inserisco il doc come sorgente e il precedente che dovevo inserire, ma che non ho trovato come destinazione. In questo modo, nel momento in cui 
                    verrà importato, sarà creato il collegamento tramite la parte iniziale della funzione.
                    */
                    documentoDaCollegare = new DocumentoDaCollegare();
                    documentoDaCollegare.setIdSessioneImportazione(sessioneImportazione);
                    documentoDaCollegare.setIdRegistroSorgente(registroDoc.getIdRegistro());
                    documentoDaCollegare.setNumeroSorgente(registroDoc.getNumero());
                    documentoDaCollegare.setAnnoSorgente(registroDoc.getAnno());
                    documentoDaCollegare.setIdRegistroDestinazione(registroDoc.getIdRegistro());
                    documentoDaCollegare.setNumeroDestinazione(numeroPrecedente);
                    documentoDaCollegare.setAnnoDestinazione(annoPrecedente);
                    documentoDaCollegare.setTipoCollegamento(DocDoc.TipoCollegamentoDoc.PRECEDENTE);
                    entityManager.persist(documentoDaCollegare);
                }
            }
        }
        
        return doc;
    }
    
    /**
     * Importa i metadati degli allegati. Per ogni path indicato nella stringa, crea l'oggetto Allegato e i suoi dettagli.
     * Gestisce sia il caso nel cui il file fisico per l'allegato in esame sia già stato importato su MinIO tramite il Caricongo, 
     * sia quello in cui non è ancora stato importato.
     * Nel primo caso, scrive tutti i metadati, leggengo il file da MinIO, nel secondo crea solo quelli che o è possibile dedurre e poi sarà Caricongo a calcolare gli alti.
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param allegatiString la stringa degli allegati da trasferire
     * @param azienda l'azienda sulla quale si sta eseguendo l'importazione 
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     * @throws TipTransferUnexpectedException 
     */
    private Doc transferAllegati(Doc doc, ImportazioneDocumento importazioneDocumento, Azienda azienda, Registro.CodiceRegistro codiceRegistro) throws TipTransferUnexpectedException {
        String allegatiString = importazioneDocumento.getAllegati();
        if (StringUtils.hasText(allegatiString)) {
            MinIOWrapper minIOWrapper = repositoryConnectionManager.getMinIOWrapper();
            List<Allegato> allegati = doc.getAllegati();
            if (allegati == null) {
                allegati = new ArrayList<>();
                doc.setAllegati(allegati);
            }
            String[] allegatiPath = allegatiString.split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR);
            int ordinale = 1;
            Pattern pattern = Pattern.compile(ImportazioneDocumento.REGEX_PREFISSI_ALLEGATI, Pattern.MULTILINE);
            for (String allegatoPath : allegatiPath) {
                Allegato.TipoAllegato tipoAllegato = Allegato.TipoAllegato.ALLEGATO;
                Allegato.SottotipoAllegato sottoTipoAllegato = null;
                HashMap<String, Object> additionalData = new HashMap();
                /*
                ogni allegato potrebbe avere un prefisso che ne identifica il tipo (es. VER__<numero_versione>, RIC_ACC__, RIC_CONS__, RIC_ERR__, ALL_INT_)
                quindi per prima cosa controllo se c'è il prefisso con una regex e nel caso lo gestisco
                */
                Matcher matcher = pattern.matcher(allegatoPath);
                if (matcher.find()) { // se entro vuol dire che c'è un prefisso
                    String prefissoString = matcher.group(0);
                    /*
                    se il prefisso è quello della versione (VER__<numero_versione>) lo gestisco:
                     setto il tipo e il sottotipo corretti
                     leggo il numero versione e creo gli additionalData da inserire sull'allegato
                    */
                    if (prefissoString.startsWith(ImportazioneDocumento.PrefissiAllegati.VER__.toString())) {
                        tipoAllegato = Allegato.TipoAllegato.ANNESSO;
                        sottoTipoAllegato = Allegato.SottotipoAllegato.DELIBERA_NO_OMISSIS;
                        Integer versione = Integer.valueOf(prefissoString.replaceAll("[^0-9]", ""));
                        additionalData.put("versione", versione);
                    } else { // in questo switch gestisco i casi delle ricevute pec, si gestiscono settando il tipo e il sottotipo corretto
                        ImportazioneDocumento.PrefissiAllegati prefisso = ImportazioneDocumento.PrefissiAllegati.valueOf(prefissoString);
                        switch (prefisso) {
                            case RIC_ACC__:
                                tipoAllegato = Allegato.TipoAllegato.ANNESSO;
                                sottoTipoAllegato = Allegato.SottotipoAllegato.RICEVUTA_ACCETTAZIONE_PEC;
                                break;
                            case RIC_CONS__:
                                tipoAllegato = Allegato.TipoAllegato.ANNESSO;
                                sottoTipoAllegato = Allegato.SottotipoAllegato.RICEVUTA_CONSEGNA_PEC;
                                break;
                            case RIC_ERR__:
                                tipoAllegato = Allegato.TipoAllegato.ANNESSO;
                                sottoTipoAllegato = Allegato.SottotipoAllegato.RICEVUTA_ERRORE_PEC;
                                break;
                        }
                    }
                    // rimuovo il prefisso dal path
                    allegatoPath = allegatoPath.replace(prefissoString, "");
                }

                Allegato allegato = new Allegato();
                allegato.setIdDoc(doc);
                allegato.setOrdinale(ordinale++);
                allegato.setTipo(tipoAllegato);
                allegato.setSottotipo(sottoTipoAllegato);
                Allegato.DettagliAllegato dettagliAllegato = allegato.getDettagli();
                if (dettagliAllegato == null) {
                    dettagliAllegato = new Allegato.DettagliAllegato();
                    allegato.setDettagli(dettagliAllegato);
                }
                Allegato.DettaglioAllegato originale = dettagliAllegato.getOriginale();
                if (originale == null) {
                    originale = new Allegato.DettaglioAllegato();
                    dettagliAllegato.setOriginale(originale);
                }

                // il separatore di path deve essere sempre "/", questo lo dovrà fare anche caricango
                allegatoPath = allegatoPath.replace("\\", "/");
                // se per caso trovo c:/qualcosa lo trasformo in c/qualcosa (è capitato in passato) anche se non dovrebbe succedere perché non passerebbe la validazione
                allegatoPath = allegatoPath.replaceAll("(^[aA-zZ]):", "/$1");
                File allegatoFakeFileForMinIO = new File(allegatoPath);
                String filePath = allegatoFakeFileForMinIO.getParent();
                if (filePath.startsWith("/"))
                    filePath = ImportazioneDocumento.MINIO_DOCS_ROOT_PATH + filePath;
                else
                    filePath = ImportazioneDocumento.MINIO_DOCS_ROOT_PATH + "/" + filePath;
                filePath = filePath.replace("\\", "/"); // lo devo rifare perché la new File se siamo su windows cambia i separatori
                String fileName = allegatoFakeFileForMinIO.getName();

                originale.setNome(fileName);
                originale.setBucket(azienda.getCodice());
                originale.setEstensione(FilenameUtils.getExtension(fileName));
                allegato.setNome(fileName);
                try {
                    MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(filePath, fileName, azienda.getCodice());
                    if (fileInfo != null) {
                        File tempFile = File.createTempFile(originale.getNome(), originale.getEstensione());
                        tempFile.deleteOnExit();
                        try (InputStream is = minIOWrapper.getByFileId(fileInfo.getFileId())) {
                            try (FileOutputStream os = new FileOutputStream(tempFile)) {
                                IOUtils.copy(is, os);
                                try (FileInputStream fis = new FileInputStream(tempFile)) {
                                    originale.setHashSha256(org.apache.commons.codec.digest.DigestUtils.sha256Hex(fis));
                                }
                                originale.setMimeType(FileUtilities.getMimeTypeFromPath(tempFile.getAbsolutePath()));
                            }
                        } finally {
                            if (tempFile != null && tempFile.exists()) {
                                tempFile.delete();
                            }
                        }
                        originale.setIdRepository(fileInfo.getFileId());
                        originale.setBucket(fileInfo.getBucketName());
                        originale.setHashMd5(fileInfo.getMd5());
                        originale.setDimensioneByte(fileInfo.getSize());
                    }
                } catch (MinIOWrapperException | IOException | MimeTypeException ex) {
                    String errorMessage = String.format("errore nel calcolo dei dettagli allegato del file %s", allegatoPath);
                    log.error(errorMessage, ex);
                    throw new TipTransferUnexpectedException(errorMessage, ex);
                }
                /*
                scrivo negli additionalData i dati per poter reperire l'allegato quando sarà lanciata l'importaizone dei file
                se l'importazione dei file è stata già lanciata scrivo comunque i dati negli additionalData anche se non dovrebbero servire. Lo faccio perché non
                costa nulla e magari poi si riveleranno utili
                */
                additionalData.put("path", allegatoPath);
                additionalData.put("registro", codiceRegistro.toString());
                additionalData.put("numero", importazioneDocumento.getNumero());
                additionalData.put("anno", importazioneDocumento.getAnno());
                allegato.setAdditionalData(additionalData);
                allegati.add(allegato);
            }
        }
        return doc;
    }
   
    /**
     * setta la fascicolazione sulla tabella archivi_doc.
     * la fascicolazione è indicata nel campo fascicolazione o idFascicoloPregresso. 
     * il campo fascicolazione indica la numerazione gerarchica in Babel, se è stato passato viene usato per reperire l'archivio e il doc sarà fascicolato lì, se
     *  non viene trovato nessun archivio con il dato passato viene restituito un errore e l'importazione della riga viene saltata.
     * se non è passato fascicolazione, ma viene passato idFascicoloPregresso (che si riferisce alla fascicolazione sul veccchio sistema), viene cercato l'archivio 
     *  che ha quel dato come id_archivio_importato. Se non viene trovato nessun fascicolo, il documento viene fascicolato nel fascicolo di default.
     * Se non viene passato nessuno dei due campi indicati sopra, il documento viene fascicolato nel fascicolo di default.
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param azienda l'azienda sulla quale si sta eseguendo l'importazione 
     * @param archivioDefault l'archivio di default
     * @param persona la persona da inserire come persona che ha fascicolato
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     * @throws TipTransferBadDataException  nel caso che l'archivio indicato nel campo fascicolazione del CSV non esista sul sistema 
     */
    private Doc transferFascicolazione(Doc doc, ImportazioneDocumento importazioneDocumento, Azienda azienda, Archivio archivioDefault, Persona persona) throws TipTransferBadDataException {
        boolean insertInDefault = true;
        List<ArchivioDoc> archiviDocs = new ArrayList<>();
        if (StringUtils.hasText(importazioneDocumento.getFascicolazione())) {
            String errorMessage = null;
            String[] fascicolazioniSplitted = importazioneDocumento.getFascicolazione().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR);
            for (String fascicolazione : fascicolazioniSplitted) {
                Archivio archivio = nonCachedEntities.getArchivioFromNumerazioneGerarchicaAndIdAzienda(fascicolazione, azienda.getId());
    
                if (archivio != null) {
                    ArchivioDoc archivioDoc = new ArchivioDoc(archivio, doc, persona);
                    archiviDocs.add(archivioDoc);
                    insertInDefault = false;
                } else { // se non trovo l'archivio scrivo l'errore nella errorMessage, ma non mi fermo in modo da avere l'elenco di tutti gli errori al termine del ciclo
                    if (errorMessage == null) {
                        errorMessage = String.format("archivio con numerazione gerarchica %s non trovato per l'azienda con id %s", 
                            fascicolazione, 
                            azienda.getId());
                    } else {
                        errorMessage += ";" + String.format("archivio con numerazione gerarchica %s non trovato per l'azienda con id %s", 
                            fascicolazione, 
                            azienda.getId());
                    }
                }
            }
            // se entro qui c'è stato un errore, per cui lancio eccezione
            if (errorMessage != null) {
                throw new TipTransferBadDataException(errorMessage);
            }
        } else if (StringUtils.hasText(importazioneDocumento.getIdFascicoloPregresso())) {
            String[] fascicolazioniSplitted = importazioneDocumento.getIdFascicoloPregresso().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR);
            for (String fascicolazione : fascicolazioniSplitted) {
                Archivio archivio = nonCachedEntities.getArchivioFromIdArchivioImportatoAndIdAzienda(fascicolazione, azienda.getId());
                if (archivio != null) {
                    ArchivioDoc archivioDoc = new ArchivioDoc(archivio, doc, persona);
                    archiviDocs.add(archivioDoc);
                    insertInDefault = false;
                } // se non trovo l'archivio non faccio nulla in modo che venga inserito in quello di default
            }
        } 
        
        if (insertInDefault) {
            archiviDocs.add(new ArchivioDoc(archivioDefault, doc, persona));
        }
        doc.setArchiviDocList(archiviDocs);
        
        return doc;
    }
    
    /**
     * Setta la visibiltà sul documento a seconda di quanto indicato in fase di importazione CSV. Questa può essere NORMALE, LIMITATA o RISERVATO
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     */
    private Doc transferVisiblita(Doc doc, ImportazioneDocumento importazioneDocumento) {
        Doc.VisibilitaDoc visibilitaDoc = Doc.VisibilitaDoc.NORMALE;
        if (StringUtils.hasText(importazioneDocumento.getRiservato()) && TipDataValidator.parseBoolean(importazioneDocumento.getRiservato())) {
            visibilitaDoc = Doc.VisibilitaDoc.RISERVATO;
        } else if (StringUtils.hasText(importazioneDocumento.getVisibilitaLimitata()) && TipDataValidator.parseBoolean(importazioneDocumento.getVisibilitaLimitata())) {
            visibilitaDoc = Doc.VisibilitaDoc.LIMITATA;
        }
        doc.setVisibilita(visibilitaDoc);
        return doc;
    }
    
    /**
     * Setta e salva i campi relativi alla registrazione sull'entità RegistroDoc:
     * i campi sono registro, numero, anno, dataRegistrazione, propostoDa.
     * NB: il registro presente in importazioneDocumento viene inserito in additionalData del doc e viene usato come effettivo quello PG, DETE o DELI
     * a seconda della tipologia
     * @param doc il doc
     * @param colonnaRegistro la colonna importazione che identifica il registro
     * @param azienda l'azienda per la quale si sta effettuando l'importazione
     * @param tipologia la tipologia di importazione
     * @param dataRegistrazione la data di registrazione del documento
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     * @throws TipTransferBadDataException nel caso il registro indicato non sia tra quelli consentiti dall'enum Registro.CodiceRegistro
     */
    private <E extends Enum<E> & ColonneImportazioneOggetto> Doc transferRegistrazione(Doc doc, Enum<E> colonnaRegistro, Azienda azienda, Struttura strutturaDefault, Registro.CodiceRegistro codiceRegistro, ImportazioneDocumento importazioneDocumento) throws TipTransferBadDataException {
        
        ZonedDateTime dataRegistrazione = null;
        if (StringUtils.hasText(importazioneDocumento.getDataRegistrazione())) {
            dataRegistrazione = TipDataValidator.parseData(importazioneDocumento.getDataRegistrazione()).atStartOfDay(ZoneId.systemDefault());
        } else if (StringUtils.hasText(importazioneDocumento.getDataAdozione())) {
            dataRegistrazione = TipDataValidator.parseData(importazioneDocumento.getDataAdozione()).atStartOfDay(ZoneId.systemDefault());
        }
        
        // aggiungo il registro che ci hanno indicato negli additional data, come registro effettivo userò invece quello derivante dalla tipologia
        if(StringUtils.hasText(importazioneDocumento.getRegistro())) {
            addInAdditionalData(doc, colonnaRegistro, importazioneDocumento.getRegistro());
        }
        
        // carico il registro(se non esiste)
        Registro registroEntity = nonCachedEntities.getRegistro(azienda.getId(), codiceRegistro);
        
        // siccome ogni tracciato chiama in modo diverso la struttura registrante, prendo quella non vuota, che verosimilmente sarà quella giusta
        String nomeStrutturaRegistrazione = null;
        boolean usaStrutturaDefault = false;
        if (StringUtils.hasText(importazioneDocumento.getPropostoDa())) {
            nomeStrutturaRegistrazione = importazioneDocumento.getPropostoDa();
        } else if (StringUtils.hasText(importazioneDocumento.getAdottatoDa())) {
            nomeStrutturaRegistrazione = importazioneDocumento.getAdottatoDa();
        } else if (StringUtils.hasText(importazioneDocumento.getProtocollatoDa())) {
            nomeStrutturaRegistrazione = importazioneDocumento.getProtocollatoDa();
        } else {
            usaStrutturaDefault = true;
        }
        
        if (registroEntity == null)
            throw new TipTransferBadDataException(String.format("registro con codice %s non valido", importazioneDocumento.getRegistro()));
        RegistroDoc registroDoc = new RegistroDoc();
        registroDoc.setIdRegistro(registroEntity);
        registroDoc.setNumero(Integer.valueOf(importazioneDocumento.getNumero()));
        registroDoc.setAnno(Integer.valueOf(importazioneDocumento.getAnno()));
        registroDoc.setDataRegistrazione(dataRegistrazione);
        if (usaStrutturaDefault)
            registroDoc.setIdStrutturaRegistrante(strutturaDefault);
        else
            registroDoc.setIdStrutturaRegistrante(findOrCreateStruttura(nomeStrutturaRegistrazione, azienda, dataRegistrazione));
        registroDoc.setIdDoc(doc);
        doc.setRegistroDocList(Arrays.asList(registroDoc));
//        entityManager.persist(registroDoc);
        return doc;
    }
    
    /**
     * Crea i related con relativa spedizione per rappresentare i destinatari/mittenti
     * @param persona la persona TIP che verrà inserita come persona che ha inserito il mittente
     * @param azienda l'azienda per cui si sta eseguendo l'esportazione
     * @param dataRegistrazione la data di registrazione del documento
     * @param soloStrutture se "true" indica che i destinatari passati sono struttura, 
     *  per cui verrano cercati solo all'interno delle strutture e nel caso non esista la struttura, verà creata
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param tipoRelated il tipo di related (MITTENTE / DESTINATARIO A / DESTINATARIO CC)
     * @param nomeColonnaNome l'enum della colonna che rapresenta il nome del/dei destinatari/mittenti
     * @param nomeColonnaIndirizzo l'enum della colonna che rapresenta l'indirizzo del/dei destinatari/mittenti
     * @param nomeColonnaDescrizione l'enum della colonna che rapresenta la descrizione del/dei destinatari/mittenti
     * @return la lista nomeColonnaDescrizione related con relativa spedizione per rappresentare i destinatari/mittenti
     */
    private <E extends Enum<E> & ColonneImportazioneOggetto>  List<Related> buildRelated(
            Doc doc,
            Persona persona, 
            Azienda azienda,
            boolean soloStrutture,
            ImportazioneDocumento importazioneDocumento, 
            Related.TipoRelated tipoRelated, 
            Enum<E> nomeColonnaNome, 
            Enum<E> nomeColonnaIndirizzo, 
            Enum<E> nomeColonnaDescrizione) {

        ZonedDateTime dataRegistrazione = null;
        if (StringUtils.hasText(importazioneDocumento.getDataRegistrazione())) {
            dataRegistrazione = TipDataValidator.parseData(importazioneDocumento.getDataRegistrazione()).atStartOfDay(ZoneId.systemDefault());
        } else if (StringUtils.hasText(importazioneDocumento.getDataAdozione())) {
            dataRegistrazione = TipDataValidator.parseData(importazioneDocumento.getDataAdozione()).atStartOfDay(ZoneId.systemDefault());
        }
        
        /* 
        la data di spedizione, nel caso di mittente ci viene passata in dataArrivo del CSV, nel caso di destinatari non è passata 
        per cui usiamo quella di registrazione perché verosimilmente il documento viene spedito subito dopo la registrazione
        */
        ZonedDateTime dataSpedizione;
        if (tipoRelated == Related.TipoRelated.MITTENTE && StringUtils.hasText(importazioneDocumento.getDataArrivo())) {
            dataSpedizione = TipDataValidator.parseData(importazioneDocumento.getDataArrivo()).atStartOfDay(ZoneId.systemDefault());
        } else {
            dataSpedizione = dataRegistrazione;
        }

        List<Related> relatedList = new ArrayList<>();

        /*
        le informazioni dei destinatari nel csv cambiano a seconda della tipologia di importazione, però in generale ci sono/possono essere le seguenti informazioni:
        nomi,
        indirizzi,
        descrizioni,
        Per questo, reperisco le 3 informazioni calcolando il nome del metodo che me restituisce usando la reflection. Il nome del metodo per convenzione è:
        get<nome della colonna>. Il nome delle varie colonne rappresentanti nomi, indirizzi e descrizioni mi viene passato in input.
        In più mi possono essere passati anche i mezzi, ma questi si reperiscono sembre con il metodo getMezzo
        */
        String nomi = null;
        String indirizzi = null;
        String descrizioni = null;
        try {
            Method getMethod = EntityReflectionUtils.getGetMethod(importazioneDocumento.getClass(), nomeColonnaNome.name());
            nomi = (String) getMethod.invoke(importazioneDocumento);
        } catch (Throwable ex) {}
        try {
            Method getMethod = EntityReflectionUtils.getGetMethod(importazioneDocumento.getClass(), nomeColonnaIndirizzo.name());
            indirizzi = (String) getMethod.invoke(importazioneDocumento);
        } catch (Throwable ex) {}
        try {
            Method getMethod = EntityReflectionUtils.getGetMethod(importazioneDocumento.getClass(), nomeColonnaDescrizione.name());
            descrizioni = (String) getMethod.invoke(importazioneDocumento);
        } catch (Throwable ex) {}
        
        // le varie informazioni, possono essere più di una e nel caso lo siano sono separate da un separatore. Per questo le splitto
        String[] nomiSplitted = null;
        String[] indirizziSplitted = null;
        String[] descrizioniSplitted = null;
        String[] mezziSplitted = null;
        int lenght = 0;
        if (StringUtils.hasText(nomi)) {
            nomiSplitted = nomi.split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR);
            lenght = nomiSplitted.length;
        }
        if (StringUtils.hasText(indirizzi)) {
            indirizziSplitted = indirizzi.split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR);
            lenght = indirizziSplitted.length;
        }
        if (StringUtils.hasText(descrizioni)) {
            descrizioniSplitted = descrizioni.split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR);
            lenght = descrizioniSplitted.length;
        }
        if (!soloStrutture && StringUtils.hasText(importazioneDocumento.getMezzo())) {
            mezziSplitted = importazioneDocumento.getMezzo().split(ImportazioneDocumento.DEFAULT_STRING_SEPARATOR);
        }
        
        // sono sicuro che tutti gli array hanno lo stesso numero di elementi perché altrimenti viene dato errore in fase di validazione dell'importazione
        for (int i = 0; i<lenght; i++) {
            String nome = null;
            if (nomiSplitted != null) {
                nome = nomiSplitted[i];
            }
            String descrizione = null;
            if (descrizioniSplitted != null) {
                descrizione = descrizioniSplitted[i];
            }
            String indirizzo = null;
            if (indirizziSplitted != null) {
                indirizzo = indirizziSplitted[i];
            }
           
            String mezzoTip = null;
            if (mezziSplitted != null) {
                mezzoTip = mezziSplitted[i];
            }
            
            // se è stato passato l'indirizzo lo uso, altrimenti prenso la descrizione, se non c'è neanche la descrizione prendo il nome. Se non c'è nulla è null
            String possibileIndirizzo = indirizzo != null? indirizzo: descrizione != null? descrizione: nome;
            
            Related related = new Related();
            related.setIdDoc(doc);
            
            // calcolo il codice mezzo
            Mezzo.CodiciMezzo codiceMezzo;
            Related.OrigineRelated origine = Related.OrigineRelated.ESTERNO;
            if (StringUtils.hasText(mezzoTip)) {
                /*
                se mi è stato passato nel CSV calcolo il codice mezzo che mi servirà dopo per istanziare l'entità.
                Sono sicuro che sia tra quelli consentiti perché l'ho controllato in fase di validazione
                 */
                ColonneImportazioneOggettoEnums.MezziConsentiti mezzoEnum = EnumUtils.getEnumIgnoreCase(ColonneImportazioneOggettoEnums.MezziConsentiti.class, mezzoTip);
                if (mezzoEnum == null) {
                    mezzoEnum = KeyValueEnum.findEnumKeyFromValue(mezzoTip, ColonneImportazioneOggettoEnums.MezziConsentiti.class);
                }
                codiceMezzo = mezzoEnum.getCodiceMezzoScripta();
            } else {
                // se non mi è stato passato lo deduco
                codiceMezzo = Mezzo.CodiciMezzo.POSTA_ORDINARIA; // il caso di default è posta ordinaria
                // se non sto trattando i destinatari solo come struttura (caso dei destinatari di un PE) e ho rilevato un indirizzo email valido, come mezzo userò email
                if (!soloStrutture && TipDataValidator.validaIndirizzoEmail(possibileIndirizzo)) {
                    codiceMezzo = Mezzo.CodiciMezzo.MAIL;
                } else {
                    // altrimenti controllo se per caso il destinatario è una struttura
                    Struttura struttura;
                    /*
                    se i destinatari sono solo strutture allora cerco la struttura per possibileIndirizzo (che nel caso di struttura dovrebbe essere il nome) e azienda.
                    Se non la trovo la creo
                    */
                   
                    if (soloStrutture) {
                        origine = Related.OrigineRelated.INTERNO;
                        struttura = findOrCreateStruttura(possibileIndirizzo, azienda, dataRegistrazione);
                    } else {
                        // altrimento la cerco per possibileIndirizzo (che nel caso di struttura dovrebbe essere il nome) e azienda.
                        struttura = findStruttura(possibileIndirizzo, azienda);
                    }
                    // Se la trovo allora il codice mezzo sarà BABEL e setto sul related l'idContatto della struttura
                    if (struttura != null) {
                        origine = Related.OrigineRelated.INTERNO;
                        codiceMezzo = Mezzo.CodiciMezzo.BABEL; 
                        related.setIdContatto(struttura.getIdContatto());
                    }
                }
            }
            
            // carico l'entità mezzo dal codice calcolato sopra
            Mezzo mezzo = getMezzoFromCodice(codiceMezzo);
            
            // creo il related
            related.setIdPersonaInserente(persona);
            related.setDescrizione(descrizione != null? descrizione: nome != null? nome: possibileIndirizzo);
            related.setOrigine(origine);
            related.setTipo(tipoRelated);
//            entityManager.persist(related);
            
            // creo la spedizione e l'indirizzo da associare al related
            Spedizione spedizione = new Spedizione();
            spedizione.setData(dataSpedizione);
            spedizione.setIdMezzo(mezzo);
            IndirizzoSpedizione indirizzoSpedizione = new Spedizione.IndirizzoSpedizione(possibileIndirizzo);
            spedizione.setIndirizzo(indirizzoSpedizione);
            spedizione.setIdRelated(related);
//            entityManager.persist(spedizione);
            related.setSpedizioneList(Arrays.asList(spedizione));
            relatedList.add(related);
        }
        return relatedList;
    }
    
    /**
     * aggiunge in additionalData di doc una chiave con il nome colonna passato e come valore il valore passato
     * Se additionalData è null, lo crea nuovo
     * @param <E>
     * @param doc il doc
     * @param nomeColonna il nome della chiave che sarà aggiunta
     * @param valore il valore da aggiungere
     * @return gli additionalData
     */
    private <E extends Enum<E> & ColonneImportazioneOggetto> HashMap<String, Object> addInAdditionalData(Doc doc, Enum<E> nomeColonna, String valore) {
        HashMap<String, Object> additionalData = doc.getAdditionalData();
        if (StringUtils.hasText(valore)) {
            if (additionalData == null) {
                additionalData = new HashMap<>();
                doc.setAdditionalData(additionalData);
            }
            additionalData.put(nomeColonna.name(), valore);
        }
        return additionalData;
    }
    
    /**
     * Cerca la struttura con il nome passato:
     *  - se ne trova più di una prende la più recente
     *  - se non la trova la crea spenta con data attivazione uguale alla data dataCessazione passata e data di cessazione uguale alla data di attivazione più un secondo
     * @param nome il nome della struttura da cercare
     * @param azienda l'azienda a cui appartiene la struttura
     * @param dataCessazione la data da inserire nel caso venga creata spenta
     * @return la struttura più recente con il nome passato se esiste, altrimenti una nuova spenta
     */
    private Struttura findOrCreateStruttura(String nome, Azienda azienda, ZonedDateTime dataCessazione) {
        Struttura struttura = findStruttura(nome, azienda);
        if (struttura == null) {
            struttura = new Struttura();
            struttura.setIdAzienda(azienda);
            struttura.setNome(nome);
            struttura.setAttiva(false);
            struttura.setSpettrale(false);
            struttura.setUsaSegreteriaBucataPadre(true);
            struttura.setUfficio(false);
            struttura.setDataAttivazione(dataCessazione);
            struttura.setDataCessazione(dataCessazione.plusSeconds(1));
        }
        return struttura;
    }
    
    /**
     * Cerca la struttura con il nome passato e se ne trova più di una prende la più recente
     * @param nome il nome della struttura da cercare
     * @param azienda l'azienda a cui appartiene la struttura
     * @return  la struttura più recente con il nome passato
     */
    private Struttura findStruttura(String nome, Azienda azienda) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QStruttura qStruttura = QStruttura.struttura;
        Struttura struttura = queryFactory
            .select(qStruttura)
            .from(qStruttura)
            .where(qStruttura.nome.equalsIgnoreCase(nome).and(qStruttura.idAzienda.id.eq(azienda.getId())))
            .orderBy(qStruttura.attiva.desc(), qStruttura.dataAttivazione.desc())
            .fetchOne();
        return struttura;
    }
    
    /**
     * Cerca la persona:
     *  se viene passato il cf allora la cerca solo per cf,
     *  altrimenti la cerca per nome e cognome,
     *  altrimenti, se viene passato il campo "altro", considera che "altro" possa essere cognome + nome, oppure lo username
     * @param cf
     * @param cognome
     * @param nome
     * @param altro considera che possa essere cognome + nome, oppure lo username
     * @param azienda
     * @return la persona, se viene trovata, altrimenti null
     */
    private Persona findOrCreatePersona(String cf, String cognome, String nome, String altro, Azienda azienda) {
        Persona persona = findPersona(cf, altro, azienda.getId());
        if (persona == null) {
            if (cognome == null && nome == null && altro != null) {
                if (altro.contains(" ")) {
                    String[] split = altro.split(" ", 2);
                    cognome = split[0];
                    nome = split[1];
                } else {
                    cognome = altro;
                    nome = altro;
                }
            }
            if (cf == null) {
                cf = RandomStringUtils.randomAlphanumeric(16);
            }
            persona = new Persona();
            persona.setNome(nome);
            persona.setCognome(cognome);
            persona.setCodiceFiscale(cf);
            persona.setAttiva(false);
            persona.setDescrizione(cognome + " " + nome);
            persona.setIdAziendaDefault(azienda);
            
            Utente utente = new Utente();
            utente.setIdInquadramento("tip");
            utente.setUsername(cf);
            utente.setAttivo(false);
            utente.setIdAzienda(azienda);
            utente.setIdPersona(persona);
            
            persona.setUtenteList(Arrays.asList(utente));
            
            entityManager.persist(persona);
        }
        return persona;
    }
    
    /**
     * Cerca la persona:
     *  se viene passato il cf allora la cerca solo per cf,
     *  altrimenti se viene passato lo username cerca la persona corrispondente all'utente che ha quello username
     * @param cf
     * @param username
     * @return la persona, se viene trovata, altrimenti null
     */
    private Persona findPersona(String cf, String username, Integer idAzienda) {
        Persona persona = null;
        QPersona qPersona = QPersona.persona;
        QUtente qUtente = QUtente.utente;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        if (cf != null) {
            BooleanExpression filter = qPersona.codiceFiscale.equalsIgnoreCase(cf);
            persona = queryFactory.select(qPersona).from(qPersona).where(filter).fetchOne();
        } else if (username != null) {
            BooleanExpression filter = qUtente.username.eq(username).and(qUtente.idAzienda.id.eq(idAzienda));
            persona = queryFactory.select(qUtente.idPersona).from(qUtente).where(filter).fetchOne();
        }
        
        return persona;
    }
    
    /**
     * Torna l'entità Mezzo identificata dal codice passato evitando di eseguire la query se in questo trasferimento è già stato caricato una volta
     * @param codiceMezzo il codice mezzo del mezzo da caricare
     * @return l'entità Mezzo identificata dal codice passato
     */
    private Mezzo getMezzoFromCodice(Mezzo.CodiciMezzo codiceMezzo) {
        Mezzo mezzo = tipTransferCachedEntities.getCachedEntityByKey(codiceMezzo, Mezzo.class);
        if (mezzo == null) {
            mezzo = nonCachedEntities.getMezzoFromCodice(codiceMezzo);
            tipTransferCachedEntities.cacheEntityByKey(codiceMezzo, mezzo);
        }
        return mezzo;
    }
    
    /**
     * Controlla se un doc è già presente in base al registor, numero, anno e azienda.
     * Il registor è dedotto in automatico dalla tipologia
     * @param tipologia la tipologia della sessione
     * @param azienda l'azienda per la quale si sta eseguendo l'importazione
     * @param numero il numero di registrazione
     * @param anno l'anno di registrazione
     * @return true se il doc è già presente in Babel, false altrimenti
     */
    private boolean isDocAlreadyPresent(SessioneImportazione.TipologiaPregresso tipologia, Azienda azienda, Integer numero, String anno) {
        QDoc qDoc = QDoc.doc;
        QRegistroDoc qRegistroDoc = QRegistroDoc.registroDoc;
        Registro.CodiceRegistro codiceRegistro = getCodiceRegistroDefault(tipologia);
        
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        BooleanExpression filter = 
                qRegistroDoc.idRegistro.codice.eq(codiceRegistro)
                        .and(
           qRegistroDoc.numero.eq(numero))
                   .and(
           qRegistroDoc.anno.eq(Integer.valueOf(anno)))
                    .and(
           qRegistroDoc.idRegistro.idAzienda.id.eq(azienda.getId()));
        
        Integer selectOne = transactionTemplate.execute(a -> {
            return queryFactory.selectOne().from(qRegistroDoc).where(filter).fetchFirst();
        });
        return selectOne != null;
    }
    
    /**
     * Torna il codice registro di default per la tipologia passata.
     * Sarà usato questo registro se non ne è stato specificato uno.
     * @param tipologia
     * @return il codice registro di default per la tipologia passata
     */
    private Registro.CodiceRegistro getCodiceRegistroDefault(SessioneImportazione.TipologiaPregresso tipologia) {
        Registro.CodiceRegistro res;
        switch (tipologia) {
            case DELIBERA:
                res = Registro.CodiceRegistro.DELI;
                break;
            case DETERMINA:
                res = Registro.CodiceRegistro.DETE;
                break;
            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                res = Registro.CodiceRegistro.PG;
                break;
            case FASCICOLO:
                res = Registro.CodiceRegistro.FASCICOLO;
                break;
            default:
                throw new AssertionError(String.format("registro per la tipologia %s non trovato", tipologia.toString()));
        }
        return res;
    }
    
}
