/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.service.controllers.tip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.tip.exceptions.TipTransferException;
import it.bologna.ausl.internauta.service.controllers.tip.validations.TipDataValidator;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.QRegistroDoc;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.scripta.Spedizione.IndirizzoSpedizione;
import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
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
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.MezziConsentiti;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.persistence.EntityManager;
import org.apache.commons.lang3.EnumUtils;
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
    
    private final EntityManager entityManager;
    
    private final ObjectMapper objectMapper;
    
    private final NonCachedEntities nonCachedEntities;
    
    private final ReporitoryConnectionManager reporitoryConnectionManager;
    
    private final TransactionTemplate transactionTemplate;
    
    private TipTransferCachedEntities tipTransferCachedEntities;

    public TipTransferManager(EntityManager entityManager, ObjectMapper objectMapper, NonCachedEntities nonCachedEntities, ReporitoryConnectionManager reporitoryConnectionManager, TransactionTemplate transactionTemplate) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.nonCachedEntities = nonCachedEntities;
        this.reporitoryConnectionManager = reporitoryConnectionManager;
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
    
    private void importPE(SessioneImportazione sessioneImportazione) {
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
                        importazioneDoc.getNumero(),
                        importazioneDoc.getAnno())) {
                    errori.setWarning(ColonneProtocolloEntrata.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
                    errori.setWarning(ColonneProtocolloEntrata.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
                    errori.setWarning(ColonneProtocolloEntrata.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, "documento già importato/presente");
                } else {
                    try {
                        RegistroDoc registroDoc = transferRegistrazione(sessioneImportazione.getIdAzienda(), sessioneImportazione.getTipologia(), importazioneDoc);
                        doc.setRegistroDocList(Arrays.asList(registroDoc));
                    } catch (TipTransferException ex) {
                        errori.setError(ColonneProtocolloEntrata.registro, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                        errori.setError(ColonneProtocolloEntrata.numero, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                        errori.setError(ColonneProtocolloEntrata.anno, TipErroriImportazione.Flusso.TipoFlusso.IMPORTAZIONE, ex.getMessage());
                    }
                    
//                    List<Related> mittenti = transferMittenti( personaTIP, importazioneDoc);
                    List<Related> mittenti = transferRelated(personaTIP, importazioneDoc, Related.TipoRelated.MITTENTE, ColonneProtocolloEntrata.mittente, ColonneProtocolloEntrata.indirizzoMittente, null);
                    doc.setRelated(mittenti);
                    
                    addInAdditionalData(doc, ColonneProtocolloEntrata.protocolloEsterno, importazioneDoc.getProtocolloEsterno());
                    addInAdditionalData(doc, ColonneProtocolloEntrata.dataProtocolloEsterno, importazioneDoc.getDataProtocolloEsterno());
                    
                }

                
                
                importazioneDoc.setErrori(errori);

            }
        });
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
        if (additionalData == null) {
            additionalData = new HashMap<>();
            doc.setAdditionalData(additionalData);
        }
        additionalData.put(nomeColonna.name(), valore);
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
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QStruttura qStruttura = QStruttura.struttura;
        Struttura struttura = queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(qStruttura.nome.equalsIgnoreCase(nome).and(qStruttura.idAzienda.id.eq(azienda.getId())))
                .orderBy(qStruttura.attiva.desc(), qStruttura.dataAttivazione.desc())
                .fetchOne();
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
     * Controlla e setta e salva i campi relativi alla registrazione sull'entità RegistroDoc
     * @param azienda l'azienda per la quale si sta effettuando l'importazione
     * @param tipologia la tipologia di importazione
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @return l'entità RegistroDoc correttamente popolata
     * @throws TipTransferException nel caso il registro indicato non sia tra quelli consentiti dall'enum Registro.CodiceRegistro
     */
    private RegistroDoc transferRegistrazione(Azienda azienda, SessioneImportazione.TipologiaPregresso tipologia, ImportazioneDocumento importazioneDocumento) throws TipTransferException {
        
        // carico o creo il registro(se non esiste)
        Registro registroEntity;
        if (!StringUtils.hasText(importazioneDocumento.getRegistro())) {
            // se non mi è stato passato il registro, devo usare quello di default, per cui sicuramente esisterà già, quindi lo carico
            registroEntity = nonCachedEntities.getRegistro(azienda.getId(), getCodiceRegistroDefault(tipologia));
        } else {
            // se mi è stato passato, prima lo cerco, se non esiste lo creo
            registroEntity = nonCachedEntities.getRegistro(azienda.getId(), EnumUtils.getEnumIgnoreCase(Registro.CodiceRegistro.class, importazioneDocumento.getRegistro()));
        }
        if (registroEntity == null)
            throw new TipTransferException(String.format("registro con codice %s non valido", importazioneDocumento.getRegistro()));
        RegistroDoc registroDoc = new RegistroDoc();
        registroDoc.setIdRegistro(registroEntity);
        registroDoc.setNumero(importazioneDocumento.getNumero());
        registroDoc.setAnno(Integer.valueOf(importazioneDocumento.getAnno()));
        ZonedDateTime dataRegistrazione = TipDataValidator.parseData(importazioneDocumento.getDataRegistrazione()).atStartOfDay(ZoneId.systemDefault());
        registroDoc.setDataRegistrazione(dataRegistrazione);
        registroDoc.setIdStrutturaRegistrante(findOrCreateStruttura(importazioneDocumento.getPropostoDa(), azienda, dataRegistrazione));
//        entityManager.persist(registroDoc);
        return registroDoc;
    }
    
    /**
     * Crea i related con relativa spedizione per rappresentare i destinatari/mittenti
     * @param persona la persona TIP che verrà inserita come persona che ha inserito il mittente
     * @param importazioneDocumento l'oggetto contente i campi da trasferire (quello che è stato popolato dal CSV)
     * @param tipoRelated il tipo di related (MITTENTE / DESTINATARIO A / DESTINATARIO CC)
     * @param nomeColonnaNome l'enum della colonna che rapresenta il nome del/dei destinatari/mittenti
     * @param nomeColonnaIndirizzo l'enum della colonna che rapresenta l'indirizzo del/dei destinatari/mittenti
     * @param nomeColonnaDescrizione l'enum della colonna che rapresenta la descrizione del/dei destinatari/mittenti
     * @return la lista nomeColonnaDescrizione related con relativa spedizione per rappresentare i destinatari/mittenti
     */
    private <E extends Enum<E> & ColonneImportazioneOggetto>  List<Related> transferRelated(
            Persona persona, 
            ImportazioneDocumento importazioneDocumento, 
            Related.TipoRelated tipoRelated, 
            Enum<E> nomeColonnaNome, 
            Enum<E> nomeColonnaIndirizzo, 
            Enum<E> nomeColonnaDescrizione) {
        
        String dataSpedizioneString = null;
        if (tipoRelated == Related.TipoRelated.MITTENTE)
            dataSpedizioneString = importazioneDocumento.getDataArrivo();
        if (!StringUtils.hasText(dataSpedizioneString)) {
            dataSpedizioneString = importazioneDocumento.getDataRegistrazione();
        }
        ZonedDateTime dataSpedizione = null;
        if (StringUtils.hasText(dataSpedizioneString)) {
            dataSpedizione = TipDataValidator.parseData(dataSpedizioneString).atStartOfDay(ZoneId.systemDefault());
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
            
            String possibileIndirizzo = indirizzo != null? indirizzo: descrizione != null? descrizione: nome;
            
            Mezzo.CodiciMezzo codiceMezzo = Mezzo.CodiciMezzo.POSTA_ORDINARIA;
            if (TipDataValidator.validaIndirizzoEmail(possibileIndirizzo)) {
                codiceMezzo = Mezzo.CodiciMezzo.MAIL;
            }
            Mezzo mezzo = getMezzoFromCodice(codiceMezzo);
            
            Related related = new Related();
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
    
    private boolean isDocAlreadyPresent(SessioneImportazione.TipologiaPregresso tipologia, Azienda azienda, String registro, String numero, String anno) {
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
