package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.cercaanomaliearchivi;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QRuolo;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class CercaAnomalieArchiviJobWorker extends JobWorker<JobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(CercaAnomalieArchiviJobWorker.class);
    private final String name = CercaAnomalieArchiviJobWorker.class.getSimpleName();
    
    @Autowired
    private AttivitaRepository attivitaRepository;
    
    @Autowired
    private ApplicazioneRepository applicazioneRepository;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizia il job");
        log.info("Preparo un po' di utilità");
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        
        Applicazione app = applicazioneRepository.findById(Applicazione.Applicazioni.scripta.name()).get();
        
        Map<Integer, Integer> bitAnomalieMap = new HashMap();
        Map<Integer, Map<String, Integer>> mappaAnomalieAziende = new HashMap();
        Integer bitResponsabileDisattivo = ArchivioDetail.DecimaleAnomalia.RESPONSABILE_DISATTIVO.getValue();
        Integer bitVicariAttiviNonPresenti = ArchivioDetail.DecimaleAnomalia.VICARI_ATTIVI_NON_PRESENTI.getValue();
        Integer bitIncoerenzaStruttura = ArchivioDetail.DecimaleAnomalia.INCOERENZA_STRUTTURA.getValue();
        Integer bitChiusiInvisibili = ArchivioDetail.DecimaleAnomalia.CHIUSI_INVISIBILI.getValue();
        
        QAttoreArchivio qAttoreArchivio = QAttoreArchivio.attoreArchivio;
        QArchivio qArchivio = QArchivio.archivio;
        QArchivioDetail qArchivioDetail = QArchivioDetail.archivioDetail;
        QUtente qUtente = QUtente.utente;
        QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
        QPermessoArchivio qPermessoArchivio = QPermessoArchivio.permessoArchivio;
        QRuolo qRuolo = QRuolo.ruolo;
        
        BooleanExpression aperto = qArchivio.stato.eq(Archivio.StatoArchivio.APERTO.toString());
        BooleanExpression livello1 = qArchivio.livello.eq(1);
        BooleanExpression chiusoPrechiuso = qArchivio.stato.eq(Archivio.StatoArchivio.CHIUSO.toString())
                .or(qArchivio.stato.eq(Archivio.StatoArchivio.PRECHIUSO.toString()));
        
        log.info("Laddove la colonna bitAnomalie è popolata la setto a null");
        jpaQueryFactory.
                update(qArchivioDetail)
                .setNull(qArchivioDetail.bitAnomalie)
                .where(qArchivioDetail.bitAnomalie.isNotNull())
                .execute();
        
        log.info("Responsabile disattivo (fascicolo senza Responsabile)");
        List<Tuple> archivi = jpaQueryFactory
                .select(qArchivio.id, qArchivio.idAzienda.id)
                .from(qArchivio)
                .join(qAttoreArchivio).on(qAttoreArchivio.idArchivio.id.eq(qArchivio.id))
                .join(qUtente).on(qUtente.idPersona.id.eq(qAttoreArchivio.idPersona.id))
                .where(
                        qArchivio.idAzienda.id.eq(qUtente.idAzienda.id)
                                .and(qUtente.attivo.eq(false))
                                .and(qAttoreArchivio.ruolo.eq(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE.toString()))
                                .and(aperto)
                                .and(livello1)
                )
                .fetch();
        
        for (Tuple archivio : archivi) {
            insertBitIntoMap(bitAnomalieMap, bitResponsabileDisattivo, archivio.get(qArchivio.id));
            insertAnomaliesNumberIntoMap(mappaAnomalieAziende, archivio.get(qArchivio.idAzienda.id), ArchivioDetail.DecimaleAnomalia.RESPONSABILE_DISATTIVO);
        }
        
        log.info("Fascicolo senza vicari attivi");
        archivi = jpaQueryFactory
                .select(qArchivio.id, qArchivio.idAzienda.id)
                .from(qArchivio)
                .where(
                        JPAExpressions.selectOne()
                        .from(qAttoreArchivio)
                        .join(qUtente).on(qUtente.idPersona.id.eq(qAttoreArchivio.idPersona.id))
                        .where(qAttoreArchivio.idArchivio.id.eq(qArchivio.id)
                                .and(qAttoreArchivio.ruolo.eq(AttoreArchivio.RuoloAttoreArchivio.VICARIO.toString()))
                                .and(qUtente.attivo.eq(true))
                                .and(qArchivio.idAzienda.id.eq(qUtente.idAzienda.id))
                        )
                        .notExists()
                    .and(aperto)
                    .and(livello1)
                )
                .fetch();
        
        for (Tuple archivio : archivi) {
            insertBitIntoMap(bitAnomalieMap, bitVicariAttiviNonPresenti, archivio.get(qArchivio.id));
            insertAnomaliesNumberIntoMap(mappaAnomalieAziende, archivio.get(qArchivio.idAzienda.id), ArchivioDetail.DecimaleAnomalia.VICARI_ATTIVI_NON_PRESENTI);
        }
        
        log.info("Incoerenza tra struttura del fascicolo e strutture di appartenenza del responsabile");
        // caso in cui il Responsabile non afferisce più alla Struttura precedente, ma il fascicolo resta collegato a quella
        archivi = jpaQueryFactory
                .select(qArchivio.id, qArchivio.idAzienda.id)
                .from(qArchivio)
                .join(qAttoreArchivio).on(qAttoreArchivio.idArchivio.id.eq(qArchivio.id))
                .join(qUtente).on(qUtente.idPersona.id.eq(qAttoreArchivio.idPersona.id))
                .leftJoin(qUtenteStruttura).on(
                        qUtenteStruttura.idUtente.id.eq(qUtente.id)
                        .and(qUtenteStruttura.idStruttura.id.eq(qAttoreArchivio.idStruttura.id))
                        .and(qUtenteStruttura.attivo.eq(true))
                )
                .where(
                        qUtente.idAzienda.id.eq(qArchivio.idAzienda.id)
                        .and(qUtente.attivo.eq(true))
                        .and(qAttoreArchivio.ruolo.eq(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE.toString()))
                        .and(aperto)
                        .and(livello1)
                        .and(qUtenteStruttura.id.isNull())
                )
                .fetch();

        for (Tuple archivio : archivi) {
            insertBitIntoMap(bitAnomalieMap, bitIncoerenzaStruttura, archivio.get(qArchivio.id));
            insertAnomaliesNumberIntoMap(mappaAnomalieAziende, archivio.get(qArchivio.idAzienda.id), ArchivioDetail.DecimaleAnomalia.INCOERENZA_STRUTTURA);
        }
        
        log.info("Chiusi senza nessun vedente");
        archivi = jpaQueryFactory
                .select(qArchivio.id, qArchivio.idAzienda.id)
                .from(qArchivio)
                .where(
                    chiusoPrechiuso
                    .and(livello1)
                    .and(
                            JPAExpressions.selectOne()
                            .from(qPermessoArchivio)
                            .join(qUtente).on(
                                    qUtente.idPersona.id.eq(qPermessoArchivio.idPersona.id)
                                    .and(qUtente.idAzienda.id.eq(qPermessoArchivio.idAzienda.id))
                                    .and(qUtente.attivo.eq(true))
                            )
                            .where(
                                    qPermessoArchivio.idArchivioDetail.id.eq(qArchivio.id)
                                    .and(qPermessoArchivio.dataCreazione.eq(qArchivio.dataCreazione))
                                    .and(qPermessoArchivio.idAzienda.id.eq(qArchivio.idAzienda.id))
                                    .and(qPermessoArchivio.bit.gt(1))
                            )
                            .notExists()
                    )
                )
                .fetch();
        
        for (Tuple archivio : archivi) {
            insertBitIntoMap(bitAnomalieMap, bitChiusiInvisibili, archivio.get(qArchivio.id));
            insertAnomaliesNumberIntoMap(mappaAnomalieAziende, archivio.get(qArchivio.idAzienda.id), ArchivioDetail.DecimaleAnomalia.CHIUSI_INVISIBILI);
        }
        
        log.info("Aggiorno i bitAnomalie");
        for (Integer idArchivio : bitAnomalieMap.keySet()) {
            jpaQueryFactory.
                    update(qArchivioDetail)
                    .set(qArchivioDetail.bitAnomalie, bitAnomalieMap.get(idArchivio))
                    .where(qArchivioDetail.id.eq(idArchivio))
                    .execute();
        }
        
        log.info("Notifico gli AG");
        Integer bitAG = jpaQueryFactory
                .select(qRuolo.mascheraBit)
                .from(qRuolo)
                .where(qRuolo.nomeBreve.eq(Ruolo.CodiciRuolo.AG.toString()))
                .fetchOne();
        
        
        List<Tuple> listaAG = jpaQueryFactory
                .select(qUtente.idPersona, qUtente.idAzienda)
                .from(qUtente)
                .where(
                        Expressions.numberTemplate(Integer.class, "function('bitand', {0}, {1})", qUtente.bitRuoli, bitAG).gt(0)
                                .and(qUtente.attivo.eq(true))
                )
                .fetch();
        
        for (Tuple ag : listaAG) {
            Azienda azienda = ag.get(qUtente.idAzienda);
            Persona persona = ag.get(qUtente.idPersona);
            Integer idAzienda = azienda.getId();//ag.get(qUtente.idAzienda.id);
//            Integer idPersona = ag.get(qUtente.idPersona.id);
            
            if (mappaAnomalieAziende.containsKey(idAzienda)) {
                Map<String, Integer> mappaAnomalie = mappaAnomalieAziende.get(idAzienda);
                String oggettoAttivita = "Sono stati rilevati dei fascicoli anomali. Le anomalie riscontrate sono: ";
                
                if (mappaAnomalie.containsKey(ArchivioDetail.DecimaleAnomalia.RESPONSABILE_DISATTIVO.toString())) {
                    oggettoAttivita += mappaAnomalie.get(ArchivioDetail.DecimaleAnomalia.RESPONSABILE_DISATTIVO.toString()).toString() + " fascicoli con responsabile spento;";
                }
                
                if (mappaAnomalie.containsKey(ArchivioDetail.DecimaleAnomalia.VICARI_ATTIVI_NON_PRESENTI.toString())) {
                    oggettoAttivita += mappaAnomalie.get(ArchivioDetail.DecimaleAnomalia.VICARI_ATTIVI_NON_PRESENTI.toString()).toString() + " fascicoli senza vicari attivi;";
                }
                
                if (mappaAnomalie.containsKey(ArchivioDetail.DecimaleAnomalia.INCOERENZA_STRUTTURA.toString())) {
                    oggettoAttivita += mappaAnomalie.get(ArchivioDetail.DecimaleAnomalia.INCOERENZA_STRUTTURA.toString()).toString() + " fascicoli con struttura a cui il responsabile non appartiene;";
                }
                
                if (mappaAnomalie.containsKey(ArchivioDetail.DecimaleAnomalia.CHIUSI_INVISIBILI.toString())) {
                    oggettoAttivita += mappaAnomalie.get(ArchivioDetail.DecimaleAnomalia.CHIUSI_INVISIBILI.toString()).toString() + " fascicoli chiusi/prechiusi senza utenti vedenti;";
                }
                
                oggettoAttivita = oggettoAttivita.substring(0, oggettoAttivita.length() - 1) + "."; // Tolgo l'ultimo ; e metto .
                
                insertAttivita(azienda, persona, oggettoAttivita, app);
            }
        }
        
        return null;
    }
    
    
    private void insertBitIntoMap(Map<Integer, Integer> bitAnomalieMap, Integer bit, Integer idArchivio) {
        if (bitAnomalieMap.containsKey(idArchivio)) {
            bitAnomalieMap.put(idArchivio, bitAnomalieMap.get(idArchivio) | bit); // Bitwise 
        } else {
            bitAnomalieMap.put(idArchivio, bit);
        }
    }
    
    private void insertAnomaliesNumberIntoMap(Map<Integer, Map<String, Integer>> mappaAnomalieAziende, Integer idAzienda, ArchivioDetail.DecimaleAnomalia tipoAnomalia) {
        if (!mappaAnomalieAziende.containsKey(idAzienda)) {
            Map<String, Integer> newMap = new HashMap();
            mappaAnomalieAziende.put(idAzienda, newMap);
        }
        Map<String, Integer> mappaAnomalie = mappaAnomalieAziende.get(idAzienda);
        if (mappaAnomalie.containsKey(tipoAnomalia.toString())) {
            mappaAnomalie.put(tipoAnomalia.toString(), mappaAnomalie.get(tipoAnomalia.toString()) + 1);
        } else {
            mappaAnomalie.put(tipoAnomalia.toString(), 1);
        }
    }
    
    private void insertAttivita(Azienda azienda, Persona persona, String oggetto, Applicazione app) {
        Attivita a = new Attivita();
        a.setIdAzienda(azienda);
        a.setIdPersona(persona);
        a.setIdApplicazione(app);
        a.setTipo(Attivita.TipoAttivita.NOTIFICA.toString().toLowerCase());
        a.setOggetto(oggetto);
        a.setDescrizione("Anomalie su fascicoli");
        a.setProvenienza("Amministrazione Gedi");
        attivitaRepository.saveAndFlush(a);
    }
    
//    private void insertAttivita(Integer idAzienda, Integer idPersona, String oggetto, Applicazione app) {
//        Attivita a = new Attivita();
//        Azienda azienda = new Azienda();
//        azienda.setId(idAzienda);
//        Persona persona = new Persona();
//        persona.setId(idPersona);
//        a.setIdAzienda(azienda);
//        a.setIdPersona(persona);
//        a.setIdApplicazione(app);
//        a.setTipo(Attivita.TipoAttivita.NOTIFICA.toString().toLowerCase());
//        a.setOggetto(oggetto);
//        a.setDescrizione("Anomalie su fascicoli");
//        a.setProvenienza("Amministrazione Gedi");
//        attivitaRepository.saveAndFlush(a);
//    }
}
