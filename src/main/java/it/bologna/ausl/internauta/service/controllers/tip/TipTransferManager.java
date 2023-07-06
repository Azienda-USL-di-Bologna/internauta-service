package it.bologna.ausl.internauta.service.controllers.tip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.tip.exceptions.TipTransferBadDataException;
import it.bologna.ausl.internauta.service.controllers.tip.exceptions.TipTransferUnexpectedException;
import it.bologna.ausl.internauta.service.controllers.tip.validations.TipDataValidator;
import it.bologna.ausl.internauta.service.utils.FileUtilities;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDoc;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.QDocDoc;
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
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.DELIBERA;
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.DETERMINA;
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.PROTOCOLLO_IN_ENTRATA;
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.PROTOCOLLO_IN_USCITA;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneProtocolloEntrata;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
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
import javax.persistence.EntityManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.tika.mime.MimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class TipTransferManager {
    private static final Logger log = LoggerFactory.getLogger(TipTransferManager.class);
    
    private final String CODICE_FISCALE_PERSONA_TIP = "TIP";
    
    private final String MINIO_DOCS_ROOT_PATH = "docs";
    
    private final EntityManager entityManager;
    
    private final ObjectMapper objectMapper;
    
    private final NonCachedEntities nonCachedEntities;
    
    private final ReporitoryConnectionManager repositoryConnectionManager;
    
    private final TransactionTemplate transactionTemplate;
    
    private TipTransferCachedEntities tipTransferCachedEntities;

    public TipTransferManager(EntityManager entityManager, ObjectMapper objectMapper, NonCachedEntities nonCachedEntities, ReporitoryConnectionManager reporitoryConnectionManager, TransactionTemplate transactionTemplate) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.nonCachedEntities = nonCachedEntities;
        this.repositoryConnectionManager = reporitoryConnectionManager;
        this.transactionTemplate = transactionTemplate;
        this.tipTransferCachedEntities = new TipTransferCachedEntities(entityManager);
    }
    
    public void transferInScripta(Long idSessione) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QSessioneImportazione qSessioneImportazione = QSessioneImportazione.sessioneImportazione;
        SessioneImportazione sessioneImportazione = transactionTemplate.execute( a -> {
            return queryFactory
                .select(qSessioneImportazione)
                .from(qSessioneImportazione)
                .where(qSessioneImportazione.id.eq(idSessione))
                .fetchOne();
        });
        switch (sessioneImportazione.getTipologia()) {
            case DELIBERA:
            case DETERMINA:
            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                
                break;
            default:
                throw new AssertionError();
        }
        
    }
    
    private void importPE(SessioneImportazione sessioneImportazione) throws TipTransferUnexpectedException {
        QImportazioneDocumento qImportazioneDocumento = QImportazioneDocumento.importazioneDocumento;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        transactionTemplate.executeWithoutResult(a -> {
            Persona personaTIP = nonCachedEntities.getPersonaFromCodiceFiscale(CODICE_FISCALE_PERSONA_TIP);
            JPAQuery<ImportazioneDocumento> importazioni = queryFactory
                    .select(qImportazioneDocumento)
                    .from(qImportazioneDocumento)
                    .where(qImportazioneDocumento.idSessioneImportazione.id.eq(sessioneImportazione.getId()))
                    .fetchAll();
            for (Iterator<ImportazioneDocumento> iterator = importazioni.iterate(); iterator.hasNext();) {
                ImportazioneDocumento importazioneDoc = iterator.next();
                Doc doc = new Doc();
                TipErroriImportazione errori = importazioneDoc.getErrori();
                if (errori == null) {
                    errori = new TipErroriImportazione();
                }

                if (isDocAlreadyPresent(
                        sessioneImportazione.getTipologia(),
                        sessioneImportazione.getIdAzienda(),
                        importazioneDoc.getRegistro(),
                        Integer.valueOf(importazioneDoc.getNumero()),
                        importazioneDoc.getAnno())) {
                    errori.setWarning(ColonneProtocolloEntrata.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
                    errori.setWarning(ColonneProtocolloEntrata.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
                    errori.setWarning(ColonneProtocolloEntrata.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
                } else {
                    ZonedDateTime dataRegistrazione = TipDataValidator.parseData(importazioneDoc.getDataRegistrazione()).atStartOfDay(ZoneId.systemDefault());
                    try {
                        /* importazione campi della registrazione:
                        * registro, numero, anno, dataRegistrazione e adottatoDa
                        */
                        RegistroDoc registroDoc = transferRegistrazione(doc, ColonneProtocolloEntrata.registro, sessioneImportazione.getIdAzienda(), sessioneImportazione.getTipologia(), dataRegistrazione, importazioneDoc);
                        doc.setRegistroDocList(Arrays.asList(registroDoc));
                    } catch (TipTransferBadDataException ex) {
                        log.error("errore nel trasferimento dei dati di registrazione", ex);
                        errori.setError(ColonneProtocolloEntrata.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                        errori.setError(ColonneProtocolloEntrata.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                        errori.setError(ColonneProtocolloEntrata.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                    }
                    
                    List<Related> mittenti = transferRelated(
                        personaTIP, 
                        sessioneImportazione.getIdAzienda(),
                        dataRegistrazione,
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
                    
                    List<Related>destinatariA = transferRelated(
                        personaTIP,
                        sessioneImportazione.getIdAzienda(),
                        dataRegistrazione,
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
                    List<Related> destinatariCC = transferRelated(
                        personaTIP,
                        sessioneImportazione.getIdAzienda(),
                        dataRegistrazione,
                        true,
                        importazioneDoc,
                        Related.TipoRelated.CC,
                        ColonneProtocolloEntrata.destinatariInterniA,
                        null, 
                        null);
//                    try {
//                    } catch (Throwable ex) {
//                        log.error("errore nel trasferimento dei destinatari interni CC", ex);
//                        errori.setError(ColonneProtocolloEntrata.destinatariInterniA, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
//                    }
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
                    setVisiblita(doc, importazioneDoc);
                    doc.setOggetto(importazioneDoc.getOggetto());
                    try {
                        setFascicolazione(doc, importazioneDoc, sessioneImportazione.getIdAzienda(), sessioneImportazione.getIdArchivioDefault(), personaTIP);
                    } catch (TipTransferBadDataException ex) {
                        log.error("errore nel trasferimento delle fascicolazioni", ex);
                        errori.setError(ColonneProtocolloEntrata.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                    }
                    addInAdditionalData(doc, ColonneProtocolloEntrata.classificazione, importazioneDoc.getClassificazione());
                    transferAllegati(doc, importazioneDoc.getAllegati(), sessioneImportazione.getIdAzienda());
                    
                }

                importazioneDoc.setErrori(errori);
            }
        });
    }
    
    private Doc transferPrecedente(Doc doc, SessioneImportazione sessioneImportazione, ImportazioneDocumento importazioneDocumento, Registro registro, Persona persona) throws TipTransferBadDataException {
        if (StringUtils.hasText(importazioneDocumento.getCollegamentoPrecedente()) && (doc.getRegistroDocList() == null || doc.getRegistroDocList().isEmpty())) {
            String errorMessage = "impossibile inserire il precedente perché c'è un errore nell'importazionde del registro";
            log.error(errorMessage);
            throw new TipTransferBadDataException(errorMessage);
        }
        QDocumentoDaCollegare qDocumentoDaCollegare = QDocumentoDaCollegare.documentoDaCollegare;
        QDoc qDoc = QDoc.doc;
        QDocDoc qDocDoc = QDocDoc.docDoc;
        QRegistroDoc qRegistroDoc = QRegistroDoc.registroDoc;
        RegistroDoc registroDoc = doc.getRegistroDocList().get(0);
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
        // TODO: collegare il precedente
    }
    
    /**
     * Importa i metadati degli allegati. Per ogni path indicato nella stringa, crea l'oggetto Allegato e i suoi dettagli.
     * Gestisce sia il caso nel cui il file fisico per l'allegato in esame sia già stato importato su MinIO tramite il Caricongo, 
     * sia quello in cui non è ancora stato importato.
     * Nel primo caso, scrive tutti i metadati, leggengo il file da MinIO, nel secondo crea solo quelli che o è possibile dedurre e poi sarà Caricongo a calcolare gli alti.
     * @param doc il doc
     * @param allegatiString la stringa degli allegati da trasferire
     * @param azienda l'azienda sulla quale si sta eseguendo l'importazione 
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     * @throws TipTransferUnexpectedException 
     */
    private Doc transferAllegati(Doc doc, String allegatiString, Azienda azienda) throws TipTransferUnexpectedException {
        MinIOWrapper minIOWrapper = repositoryConnectionManager.getMinIOWrapper();
        List<Allegato> allegati = doc.getAllegati();
        if (allegati == null) {
            allegati = new ArrayList<>();
            doc.setAllegati(allegati);
        }
        String[] allegatiPath = allegatiString.split(TipDataValidator.DEFAULT_STRING_SEPARATOR);
        for (String allegatoPath : allegatiPath) {
            Allegato allegato = new Allegato();
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
            
            File allegatoFile = new File(allegatoPath);
            String filePath = allegatoFile.getParent();
            
            // il separatore di path deve essere sempre "/", questo lo dovrà fare anche caricango
            filePath = filePath.replace("\\", "/");
            String fileName = allegatoFile.getName();
            
            originale.setNome(fileName);
            originale.setBucket(azienda.getCodice());
            originale.setEstensione(FilenameUtils.getExtension(fileName));
            try {
                MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(filePath, allegatoPath, azienda.getCodice());
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
                }
            } catch (MinIOWrapperException | IOException | MimeTypeException ex) {
                String errorMessage = String.format("errore nel calcolo dei dettagli allegato del file %s", allegatoPath);
                log.error(errorMessage, ex);
                throw new TipTransferUnexpectedException(errorMessage, ex);
            }
            allegati.add(allegato);
        }
        return doc;
    }
    
    /**
     * setta la fascicolazione sulla tabella archivi_doc.
     * la fascicolazione è indicata nel campo fascicolazione. Se questa non è stata passata o 
     * è stato passato idFascicoloPregresso(che si riferisce alla fascicolazione sul veccchio sistema) il documento viene inserito nel fascicolo di default.
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param azienda l'azienda sulla quale si sta eseguendo l'importazione 
     * @param archivioDefault l'archivio di default
     * @param persona la persona da inserire come persona che ha fascicolato
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     * @throws TipTransferException  nel caso che l'archivio indicato nel campo fascicolazione del CSV non esista sul sistema 
     */    
    /**
     * setta la fascicolazione sulla tabella archivi_doc.
     * la fascicolazione è indicata nel campo fascicolazione. Se questa non è stata passata o 
     * è stato passato idFascicoloPregresso(che si riferisce alla fascicolazione sul veccchio sistema) il documento viene inserito nel fascicolo di default.
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param azienda l'azienda sulla quale si sta eseguendo l'importazione 
     * @param archivioDefault l'archivio di default
     * @param persona la persona da inserire come persona che ha fascicolato
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     * @throws TipTransferBadDataException  nel caso che l'archivio indicato nel campo fascicolazione del CSV non esista sul sistema 
     */
    private Doc setFascicolazione(Doc doc, ImportazioneDocumento importazioneDocumento, Azienda azienda, Archivio archivioDefault, Persona persona) throws TipTransferBadDataException {
        List<ArchivioDoc> archiviDocs = new ArrayList<>();
        if (StringUtils.hasText(importazioneDocumento.getFascicolazione())) {
            String errorMessage = null;
            String[] fascicolazioniSplitted = importazioneDocumento.getFascicolazione().split(TipDataValidator.DEFAULT_STRING_SEPARATOR);
            for (String fascicolazione : fascicolazioniSplitted) {
                Archivio archivio = nonCachedEntities.getArchivioFromNumerazioneGerarchicaAndIdAzienda(fascicolazione, azienda.getId());
    
                if (archivio != null) {
                    ArchivioDoc archivioDoc = new ArchivioDoc(archivio, doc, persona);
                    archiviDocs.add(archivioDoc);
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
            doc.setArchiviDocList(archiviDocs);    
        } else {
            doc.setArchiviDocList(Arrays.asList(new ArchivioDoc(archivioDefault, doc, persona)));
        }
        return doc;
    }
    
    /**
     * Setta la visibiltà sul documento a seconda di quanto indicato in fase di importazione CSV. Questa può essere NORMALE, LIMITATA o RISERVATO
     * @param doc il doc
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @return lo stesso doc in input, utile per poter concatenare il metodo a qualcos altro
     */
    private Doc setVisiblita(Doc doc, ImportazioneDocumento importazioneDocumento) {
        Doc.VisibilitaDoc visibilitaDoc = Doc.VisibilitaDoc.NORMALE;
        if (StringUtils.hasText(importazioneDocumento.getRiservato()) && Boolean.parseBoolean(importazioneDocumento.getRiservato())) {
            visibilitaDoc = Doc.VisibilitaDoc.RISERVATO;
        } else if (StringUtils.hasText(importazioneDocumento.getVisibilitaLimitata()) && Boolean.parseBoolean(importazioneDocumento.getVisibilitaLimitata())) {
            visibilitaDoc = Doc.VisibilitaDoc.LIMITATA;
        }
        doc.setVisibilita(visibilitaDoc);
        return doc;
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
        if (valore != null) {
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
     * @return l'entità RegistroDoc correttamente popolata
     * @throws TipTransferBadDataException nel caso il registro indicato non sia tra quelli consentiti dall'enum Registro.CodiceRegistro
     */
    private <E extends Enum<E> & ColonneImportazioneOggetto> RegistroDoc transferRegistrazione(Doc doc, Enum<E> colonnaRegistro, Azienda azienda, SessioneImportazione.TipologiaPregresso tipologia, ZonedDateTime dataRegistrazione, ImportazioneDocumento importazioneDocumento) throws TipTransferBadDataException {
        
        // aggiungo il registro che ci hanno indicato negli additional data, come registro effettivo userò invece quello derivante dalla tipologia
        if(StringUtils.hasText(importazioneDocumento.getRegistro())) {
            addInAdditionalData(doc, colonnaRegistro, importazioneDocumento.getRegistro());
        }
        
        // carico il registro(se non esiste)
        Registro registroEntity = nonCachedEntities.getRegistro(azienda.getId(), getCodiceRegistroDefault(tipologia));
        
        if (registroEntity == null)
            throw new TipTransferBadDataException(String.format("registro con codice %s non valido", importazioneDocumento.getRegistro()));
        RegistroDoc registroDoc = new RegistroDoc();
        registroDoc.setIdRegistro(registroEntity);
        registroDoc.setNumero(Integer.valueOf(importazioneDocumento.getNumero()));
        registroDoc.setAnno(Integer.valueOf(importazioneDocumento.getAnno()));
        registroDoc.setDataRegistrazione(dataRegistrazione);
        registroDoc.setIdStrutturaRegistrante(findOrCreateStruttura(importazioneDocumento.getPropostoDa(), azienda, dataRegistrazione));
//        entityManager.persist(registroDoc);
        return registroDoc;
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
    private <E extends Enum<E> & ColonneImportazioneOggetto>  List<Related> transferRelated(
            Persona persona, 
            Azienda azienda,
            ZonedDateTime dataRegistrazione,
            boolean soloStrutture,
            ImportazioneDocumento importazioneDocumento, 
            Related.TipoRelated tipoRelated, 
            Enum<E> nomeColonnaNome, 
            Enum<E> nomeColonnaIndirizzo, 
            Enum<E> nomeColonnaDescrizione) {

        ZonedDateTime dataSpedizione;
        if (tipoRelated == Related.TipoRelated.MITTENTE && StringUtils.hasText(importazioneDocumento.getDataArrivo())) {
            dataSpedizione = TipDataValidator.parseData(importazioneDocumento.getDataArrivo()).atStartOfDay(ZoneId.systemDefault());
        } else {
            dataSpedizione = dataRegistrazione;
        }
        
        List<Related> relatedList = new ArrayList<>();
        
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
        
        
        String[] nomiSplitted = null;
        String[] indirizziSplitted = null;
        String[] descrizioniSplitted = null;
        String[] mezziSplitted = null;
        int lenght = 0;
        if (StringUtils.hasText(nomi)) {
            nomiSplitted = nomi.split(TipDataValidator.DEFAULT_STRING_SEPARATOR);
            lenght = nomiSplitted.length;
        }
        if (StringUtils.hasText(indirizzi)) {
            indirizziSplitted = indirizzi.split(TipDataValidator.DEFAULT_STRING_SEPARATOR);
            lenght = indirizziSplitted.length;
        }
        if (StringUtils.hasText(descrizioni)) {
            descrizioniSplitted = descrizioni.split(TipDataValidator.DEFAULT_STRING_SEPARATOR);
            lenght = descrizioniSplitted.length;
        }
        if (StringUtils.hasText(importazioneDocumento.getMezzo())) {
            mezziSplitted = importazioneDocumento.getMezzo().split(TipDataValidator.DEFAULT_STRING_SEPARATOR);
        }
        
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
            String mezzoTip = mezziSplitted[i];
            
            String possibileIndirizzo = indirizzo != null? indirizzo: descrizione != null? descrizione: nome;
            
            Mezzo.CodiciMezzo codiceMezzo;
            
            Related related = new Related();
            if (StringUtils.hasText(mezzoTip)) {
                codiceMezzo = ColonneImportazioneOggettoEnums.MezziConsentiti.valueOf(mezzoTip).getCodiceMezzoScripta();
            } else {
                codiceMezzo = Mezzo.CodiciMezzo.POSTA_ORDINARIA;
                if (!soloStrutture && TipDataValidator.validaIndirizzoEmail(possibileIndirizzo)) {
                    codiceMezzo = Mezzo.CodiciMezzo.MAIL;
                } else {
                    Struttura struttura;
                    if (soloStrutture) {
                        struttura = findOrCreateStruttura(possibileIndirizzo, azienda, dataRegistrazione);
                    } else {
                        struttura = findStruttura(possibileIndirizzo, azienda);
                    }
                    if (struttura != null) {
                        codiceMezzo = Mezzo.CodiciMezzo.BABEL; 
                        related.setIdContatto(struttura.getIdContatto());
                    }
                }
            }
            Mezzo mezzo = getMezzoFromCodice(codiceMezzo);
            
            related.setIdPersonaInserente(persona);
            related.setDescrizione(descrizione != null? descrizione: possibileIndirizzo);
            related.setOrigine(Related.OrigineRelated.ESTERNO);
            related.setTipo(tipoRelated);
//            entityManager.persist(related);
            
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
    
    private Mezzo getMezzoFromCodice(Mezzo.CodiciMezzo codiceMezzo) {
        Mezzo mezzo = tipTransferCachedEntities.getCachedEntityByKey(codiceMezzo, Mezzo.class);
        if (mezzo == null) {
            mezzo = nonCachedEntities.getMezzoFromCodice(codiceMezzo);
            tipTransferCachedEntities.cacheEntityByKey(codiceMezzo, mezzo);
        }
        return mezzo;
    }
    
    private boolean isDocAlreadyPresent(SessioneImportazione.TipologiaPregresso tipologia, Azienda azienda, String registro, Integer numero, String anno) {
        QDoc qDoc = QDoc.doc;
        QRegistroDoc qRegistroDoc = QRegistroDoc.registroDoc;
        Registro.CodiceRegistro codiceRegistro;
        if (!StringUtils.hasText(registro)) {
            codiceRegistro = getCodiceRegistroDefault(tipologia);
        } else {
            codiceRegistro = EnumUtils.getEnumIgnoreCase(Registro.CodiceRegistro.class, registro);
        }
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
            return queryFactory.selectOne().from(qDoc).where(filter).fetchFirst();
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
