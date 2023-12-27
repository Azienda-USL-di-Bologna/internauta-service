package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.automatismifineanno;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.controllers.scripta.ScriptaCopyUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDetailRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.TitoloRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Archivio.TipoArchivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author conte
 */
@MasterjobsWorker
public class AutomatismiFineAnnoJobWorker extends JobWorker<AutomatismiFineAnnoJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(AutomatismiFineAnnoJobWorker.class);
    private final String name = AutomatismiFineAnnoJobWorker.class.getSimpleName();
    
    private Integer anno = ZonedDateTime.now().getYear();
//    private Integer anno = 2024;
    
    private Azienda azienda;
    private Utente utenteResponsabileFascicoloSpeciale;
    private Utente vicarioFascicoloSpeciale;
    private Titolo classificazioneFascSpeciale;
    private Map<String, String> nomeFascicoliSpeciali;
    private Struttura strutturaUtenteResponsabile;
    
    @Autowired
    private ArchivioRepository archivioRepository;
    
    @Autowired
    private ArchivioDetailRepository archivioDetailRepository;

    @Autowired
    private StrutturaRepository strutturaRepository;
    
    @Autowired
    private UtenteStrutturaRepository utenteStrutturaRepository;
    
    @Autowired
    private TitoloRepository titoloRepository;
    
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ScriptaCopyUtils scriptaCopyUtils;
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        // estraggo i dati che mi serveno incrociando workerData e db
        Integer idAzienda = getWorkerData().getIdAzienda();
        azienda = aziendaRepository.getById(idAzienda);
        log.info("start {} dell'azienda {}...", getName(), idAzienda);
        
        Integer idUtenteResponsabileFascicoloSpeciale = getWorkerData().getIdUtenteResponsabileFascicoloSpeciale();
        utenteResponsabileFascicoloSpeciale = utenteRepository.getById(idUtenteResponsabileFascicoloSpeciale);
        
        Integer idVicarioFascicoloSpeciale = getWorkerData().getIdVicarioFascicoloSpeciale();
        vicarioFascicoloSpeciale = utenteRepository.getById(idVicarioFascicoloSpeciale);
        
        Integer idClassificazioneFascSpeciale = getWorkerData().getIdClassificazioneFascSpeciale();
        classificazioneFascSpeciale = titoloRepository.getById(idClassificazioneFascSpeciale);
        
        nomeFascicoliSpeciali = getWorkerData().getNomeFascicoliSpeciali();
        // ottengo la strttura fascicolante dall'afferenza diretta dell'utente responsabile 
        Integer idStrutturaUtenteResponsabile = utenteStrutturaRepository.getIdStrutturaAfferenzaDirettaAttivaByIdUtente(idUtenteResponsabileFascicoloSpeciale);
        strutturaUtenteResponsabile = strutturaRepository.getById(idStrutturaUtenteResponsabile);
        
        // apro le danze
        
        // parto creando gli archivi speciali
        try{
            if (alreadyExistArchiviSpecialiThisYear(idAzienda)) {
                throw new MasterjobsWorkerException("Sono già presenti degli archivi, perciò mi blocco!!!");
                
            }
            
            // Fascicolo: Atti dell'azienda
            Archivio fascicoloSpecialeAtti = createArchivioSpeciale(1, nomeFascicoliSpeciali.get("fascicoloSpecialeAtti"), null);

            if (fascicoloSpecialeAtti != null){
                log.info("creato correttamente il fascicolo {} dell'azienda {}", fascicoloSpecialeAtti.getId(), azienda.getId());

                // Sottofascicoli: Registri, Determinazioni, Deliberazioni
                Archivio fascicoloRegistro = createArchivioSpeciale(1, nomeFascicoliSpeciali.get("fascicoloSpecialeRegistri"), fascicoloSpecialeAtti);
                createArchivioSpeciale(2, nomeFascicoliSpeciali.get("fascicoloSpecialeDete"), fascicoloSpecialeAtti);
                createArchivioSpeciale(3, nomeFascicoliSpeciali.get("fascicoloSpecialeDeli"), fascicoloSpecialeAtti);
                createArchivioSpeciale(4, nomeFascicoliSpeciali.get("fascicoloSpecialeRaccoltaSemplice"), fascicoloSpecialeAtti);
                log.info("ho finito di creare i Sottofascicoli: Registri, Determinazioni, Deliberazioni dell'azienda {}", azienda.getId());

                if (fascicoloRegistro != null){
                    // Inserti: Registro giornaliero di protocollo, Registro giornaliero delle determinazioni, Registro giornaliero delle deliberazioni, Registri annuali
                    createArchivioSpeciale(1, nomeFascicoliSpeciali.get("registroGgProtocollo"), fascicoloRegistro);
                    createArchivioSpeciale(2, nomeFascicoliSpeciali.get("registroGgDeterminazioni"), fascicoloRegistro);
                    createArchivioSpeciale(3, nomeFascicoliSpeciali.get("registroGgDeliberazioni"), fascicoloRegistro);
                    createArchivioSpeciale(4, nomeFascicoliSpeciali.get("registriAnnuali"), fascicoloRegistro);
                    log.info("ho finito di creare i Inserti: Registro giornaliero di protocollo, Registro giornaliero delle determinazioni, Registro giornaliero delle deliberazioni, Registri annuali dell'azienda {}", azienda.getId());
                } else {
                    throw new MasterjobsWorkerException("il fascicoloRegistro è null");
                }
            }
        } catch (Exception ex) {
           String errore = "Errore nel creare un archivio speciale";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }
        
        // ho finito di creare i fascicoli speciali
        // ora procedo a duplicare i fascicoli atti
        try {
            if (!alreadyExistArchiviAttivitaThisYear(idAzienda)){
                duplicaFascicoliAttivita();
            } else {
                // assicurarsi dell'utilità di questo controllo
                throw new MasterjobsWorkerException("Sono già stati trovati degli archivi attività per quest'anno. MI BLOCCO!");
            }
        } catch (Exception ex) {
           String errore = "Errore nel duplicare un archivio atti";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }

        log.info("end {} dell'azienda {}...", getName(), idAzienda);
        return null;
    }
    
    /**
     * Crea un archivio speciale in base ai parametri di ingresso
     * @param numeroArchivio Letteralmente il numero con cui l'archivio verrà numerato
     * @param nomeFascicoloSpeciale L'oggetto dell'archivio
     * @param fascicoloPadre Il padre dell'archivio che sto per creare, null se sono un livello 1
     * @return ritorna L'archivio appena creato
     */
    private Archivio createArchivioSpeciale(Integer numeroArchivio, String nomeFascicoloSpeciale, Archivio fascicoloPadre){
        
        log.info("procedo a creare e numerare il fascicolo speciale \"{}\" dell'azienda {}", nomeFascicoloSpeciale, azienda.getId());
        // creo e inizio a settare il nuovo archivio
        Archivio newArchivio = new Archivio();
        newArchivio.setId(null);
        newArchivio.setIdAzienda(azienda);
        newArchivio.setOggetto(nomeFascicoloSpeciale);
        if (fascicoloPadre == null) {
            newArchivio.setNumero(0);
            newArchivio.setNumerazioneGerarchica("x/" + anno);
            newArchivio.setStato(Archivio.StatoArchivio.APERTO);
            
        } else {
            newArchivio.setNumero(numeroArchivio);
            newArchivio.setNumerazioneGerarchica((fascicoloPadre == null ? newArchivio.getNumero().toString() + "/" + newArchivio.getAnno().toString() : fascicoloPadre.getNumerazioneGerarchica().replace("/", "-" + newArchivio.getNumero().toString() + "/")));
            newArchivio.setStato(Archivio.StatoArchivio.APERTO);
            
        }
        newArchivio.setAnno(anno);
        newArchivio.setTipo(Archivio.TipoArchivio.SPECIALE);// su argo veniva settato ad affare 
        newArchivio.setIdArchivioPadre(fascicoloPadre);
        newArchivio.setLivello((fascicoloPadre == null ? 1 : fascicoloPadre.getLivello() + 1));
        newArchivio.setIdArchivioRadice((fascicoloPadre == null ? newArchivio : fascicoloPadre.getIdArchivioRadice()));
        newArchivio.setIdTitolo((fascicoloPadre == null ? classificazioneFascSpeciale : fascicoloPadre.getIdTitolo()));
        newArchivio.setNumeroSottoarchivi(0);
        newArchivio.setFoglia(true);
        newArchivio.setIdArchivioArgo(null);
        newArchivio.setIdArchivioImportato(null);
        // massimario ?
        newArchivio.setDataCreazione(ZonedDateTime.now());
        newArchivio.setDataInserimentoRiga(ZonedDateTime.now());
        newArchivio.setVersion(ZonedDateTime.now());
        // salvo e refresho l'archio per modificarlo ancora
        entityManager.persist(newArchivio);
        entityManager.flush();
        entityManager.refresh(newArchivio);
        
        // ne setto anche alcuni parametri dell'archivio detail 
        // pescandolo dal db dopo aver salvato l'archivio cosicché il/i trigger/s crei/creino l'archivio_detail
        ArchivioDetail newArchivioDetail = archivioDetailRepository.getById(newArchivio.getId());
        newArchivioDetail.setIdStruttura(strutturaUtenteResponsabile);
        newArchivioDetail.setIdPersonaResponsabile(utenteResponsabileFascicoloSpeciale.getIdPersona());
        newArchivioDetail.setIdPersonaCreazione(utenteResponsabileFascicoloSpeciale.getIdPersona());
        
        
        if (fascicoloPadre == null) {
//            archivioRepository.numeraArchivio(newArchivio.getId());
//            entityManager.refresh(newArchivio); 
            //da commentare dopo i test
            newArchivio.setNumero(1);
            newArchivio.setAnno(anno);
            newArchivio.setNumerazioneGerarchica("1/" + anno);
            entityManager.persist(newArchivio);
            entityManager.flush();
            entityManager.refresh(newArchivio);  
            
        }
        
        // ne setto gli attori (creatore, responsabile e vicario)
        setNewAttoriArchivioSpeciale(newArchivio);
        
        log.info("finito di creare e numerare il fascicolo speciale \"{}\"({}) dell'azienda {}", newArchivio.getOggetto(), newArchivio.getId(), azienda.getId());
        entityManager.flush();
        entityManager.refresh(newArchivio);   
        return newArchivio;
    }
    
    /**
     * Imposto gli attori (creatore, responsabile e vicario) di default per gli archivi speciali
     * @param archivio L'archivio di cui voglio impostare gli attori 
     */
    public void setNewAttoriArchivioSpeciale(Archivio archivio){
        log.info("setto gli attori (creatore, responsabile e vicario) del fascicolo \"{}\"({}) dell'azienda {}", archivio.getOggetto(), archivio.getId(), azienda.getId());
        List<AttoreArchivio> attoriList = new ArrayList<AttoreArchivio>();
        // creazione e salvataggio dell'attore creatore
        AttoreArchivio newAttoreCreatore = new AttoreArchivio(
                archivio, 
                utenteResponsabileFascicoloSpeciale.getIdPersona(), 
                strutturaUtenteResponsabile, 
                AttoreArchivio.RuoloAttoreArchivio.CREATORE);
        entityManager.persist(newAttoreCreatore);
        entityManager.flush();
        entityManager.refresh(newAttoreCreatore);
        // aggiungo l'attore creatore appena refreshato dentro l'array
        attoriList.add(newAttoreCreatore);
        
        // creazione e salvataggio dell'attore responsabile
        AttoreArchivio newAttoreResponsabile = new AttoreArchivio(
                archivio, 
                utenteResponsabileFascicoloSpeciale.getIdPersona(), 
                strutturaUtenteResponsabile, 
                AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE);
        entityManager.persist(newAttoreResponsabile);
        entityManager.flush();
        entityManager.refresh(newAttoreResponsabile);
        // aggiungo l'attore responsabile appena refreshato dentro l'array        
        attoriList.add(newAttoreResponsabile);
        
        // ottengo la strttura fascicolante dall'afferenza diretta dell'utente vicario 
        Integer idStrutturaUtenteResponsabile = utenteStrutturaRepository.getIdStrutturaAfferenzaDirettaAttivaByIdUtente(vicarioFascicoloSpeciale.getId());
        Struttura strutturaUtenteVicario = strutturaRepository.getById(idStrutturaUtenteResponsabile);
        
        // creazione e salvataggio dell'attore vicario
        AttoreArchivio newAttoreVicario = new AttoreArchivio(
                archivio, 
                vicarioFascicoloSpeciale.getIdPersona(),
                strutturaUtenteVicario, 
                AttoreArchivio.RuoloAttoreArchivio.VICARIO);
        entityManager.persist(newAttoreVicario);
        entityManager.refresh(newAttoreVicario);
        // aggiungo l'attore vicario appena refreshato dentro l'array 
        attoriList.add(newAttoreVicario);

        // metto l'array degli attori nell'archivio
        archivio.setAttoriList(attoriList);
        log.info("finito di settare gli attori (creatore, responsabile e vicario) del fascicolo \"{}\"({}) dell'azienda {}", archivio.getOggetto(), archivio.getId(), azienda.getId());
    }
    
    /**
     * Controllo se esiste già almeno un archivio di tipo attività nell'anno corrente.
     * (Do per scontato che sono la prima cosa a partire ad inizio anno quindi se ne trovo almeno uno c'è un problema.)
     * @return true se ne trovo false altrimenti
     */
    private boolean alreadyExistArchiviAttivitaThisYear(Integer idAzienda){
        QArchivio archivio = QArchivio.archivio;
        Predicate predicatoTrovaArchiviAttivita = archivio.tipo.eq(TipoArchivio.ATTIVITA.toString())
                .and(archivio.anno.goe(anno))
                .and(archivio.idAzienda.id.eq(idAzienda));
        
        Iterable<Archivio> archiviTrovaArchiviAttivita = archivioRepository.findAll(predicatoTrovaArchiviAttivita);
        Iterator<Archivio> archiviTrovaArchiviAttivitaIterator = archiviTrovaArchiviAttivita.iterator();
        
        if (archiviTrovaArchiviAttivitaIterator.hasNext()) {
            return true;
        } else {
            return false;
        }
    }
   
    /**
     * Controllo se esiste già almeno un archivio nell'anno corrente.
     * (Do per scontato che sono la prima cosa a partire ad inizio anno quindi se ne trovo almeno uno c'è un problema.)
     * @return true se ne trovo false altrimenti
     */
    private boolean alreadyExistArchiviSpecialiThisYear(Integer idAzienda){
        QArchivio archivio = QArchivio.archivio;
        Predicate predicatoTrovaArchiviAttivita = archivio.anno.goe(anno)
                .and(archivio.tipo.eq(TipoArchivio.SPECIALE.toString()))
                .and(archivio.idAzienda.id.eq(idAzienda));
        
        Iterable<Archivio> archiviTrovaArchiviAttivita = archivioRepository.findAll(predicatoTrovaArchiviAttivita);
        Iterator<Archivio> archiviTrovaArchiviAttivitaIterator = archiviTrovaArchiviAttivita.iterator();
        
        if (archiviTrovaArchiviAttivitaIterator.hasNext()) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Pesco e ciclo gli archivi da duplicare e per ognuno di essi chiamo la funzione apposta.
     * @throws Exception 
     */
    private void duplicaFascicoliAttivita() throws Exception{
        // pool di archivi da duplicare di livello 1 (non mi serve tirar su nessun'altro archivio poiché userò getArchiviFigliList()
        // per tirar su i figli poi i figli dei figli comunemente detti nipoti)
        Predicate predicatoArchiviAttivitaLivello1 = QArchivio.archivio.idAzienda.id.eq(azienda.getId())
                                                     .and(QArchivio.archivio.tipo.eq(TipoArchivio.ATTIVITA.toString()))
                                                     .and(QArchivio.archivio.stato.eq(Archivio.StatoArchivio.APERTO.toString()))
                                                     .and(QArchivio.archivio.anno.eq(anno - 1))
                                                     .and(QArchivio.archivio.livello.eq(1));
        
        Iterable<Archivio> archiviAttivitaLivello1 = archivioRepository.findAll(predicatoArchiviAttivitaLivello1);
        Iterator<Archivio> iteratorArchiviAttivitaLivello1 = archiviAttivitaLivello1.iterator();
        
        // controllo di averne dirato su almeno uno
        if (!iteratorArchiviAttivitaLivello1.hasNext()){
            throw new MasterjobsWorkerException(String.format("Non ho trovato archivi attività da duplicare per l'anno %d", anno - 1));
        }
        
        log.info("inizio a duplicare gli archivi attività dell'azienda {}", azienda.getId());
        
        // li duplico in ordine di livello poiché quando dovrò duplicare quelli di livello 2 mi serviranno gli id dei padri (quelli di livello 1)
        // e stessa cosa per quelli di livello 3 che mi serviranno i padri (quelli di livello 2)
        
        for (Archivio archivioAttivitaDaCopiare : archiviAttivitaLivello1){
               scriptaCopyUtils.duplicaArchivio(archivioAttivitaDaCopiare, null, false, true, false, true);
//            Archivio archivioAttivitaLivello1Duplicato = duplicatoreArchivi(archivioAttivitaLivello1, archivioAttivitaLivello1.getIdArchivioPadre());
//            for (Archivio archivioAttivitaLivello2 : archivioAttivitaLivello1.getArchiviFigliList()){
//                // non mi interessano le bozze figlie quindi le evito
//                if (!archivioAttivitaLivello2.getStato().equals(Archivio.StatoArchivio.BOZZA)) {
//                    Archivio archivioAttivitaLivello2Duplicato = duplicatoreArchivi(archivioAttivitaLivello2, archivioAttivitaLivello1Duplicato);
//                    for (Archivio archivioAttivitaLivello3 : archivioAttivitaLivello2.getArchiviFigliList()){
//                        // non mi interessano le bozze nipoti quindi le evito
//                        if (!archivioAttivitaLivello3.getStato().equals(Archivio.StatoArchivio.BOZZA)) {
//                            duplicatoreArchivi(archivioAttivitaLivello3, archivioAttivitaLivello2Duplicato);
//                        }
//                    }
//                }
//            }
        }
        
        log.info("finito di duplicare gli archivi attività dell'azienda {}", azienda.getId());
    }
    
    /**
     * Duplico realmente l'archivio passato come figlio di uno specifico archivio (archivioDestinazione) e relativi attori.
     * @param archivioDaCopiare L'archivio passato da copiare a cui vanno apportate alcune modifiche.
     * @param archivioDestinazione L'archivio destinazione in cui il nuovo archivio verrà messo come figlio.
     * @return Ritorno il nouvo archivio.
     * @throws JsonProcessingException
     * @throws EntityReflectionException 
     */
    private Archivio duplicatoreArchivi(Archivio archivioDaCopiare, Archivio archivioDestinazione) throws JsonProcessingException, EntityReflectionException{
        // variabile che userò per settare le date come dataCreazione e inserimentoRiga
        ZonedDateTime now = ZonedDateTime.now();
        
        // clono l'archivioDaCopiare in newArchivio e proced a modificarne i parametri che devo cambiare
        Archivio newArchivio = (Archivio) objectMapper.readValue(objectMapper.writeValueAsString(archivioDaCopiare), EntityReflectionUtils.getEntityFromProxyObject(archivioDaCopiare));
        newArchivio.setId(null);
        newArchivio.setAnno(anno);
        newArchivio.setIdArchivioCopiato(archivioDaCopiare);
        newArchivio.setIdArchivioPadre(archivioDestinazione);
        if (archivioDestinazione != null && archivioDaCopiare.getLivello() > 1){ // se non sono un archivio radice
            newArchivio.setNumero(archivioDaCopiare.getNumero());
            newArchivio.setOggetto(archivioDaCopiare.getOggetto());
            newArchivio.setIdArchivioRadice(archivioDestinazione.getIdArchivioRadice());
            newArchivio.setNumerazioneGerarchica(archivioDestinazione.getNumerazioneGerarchica().replace("/", "-" + archivioDaCopiare.getNumero().toString() + "/"));
        } else { // se sono un archivio radice
            // copio il nome, ma se finisce con _AnnoPrecedente, lo sostituisco con _AnnoCorrente
            String oggetto = archivioDaCopiare.getOggetto();
            if (oggetto.endsWith(String.format("_%d", anno - 1))) {
                oggetto = oggetto.replace(String.format("_%d", anno - 1), String.format("_%d", anno));
            }
            newArchivio.setOggetto(oggetto);
            // la tripletta numero, numerazioneGerarchica e stato settati così servono per la numerazione che avverà a fine metodo
            newArchivio.setNumero(0);
            newArchivio.setNumerazioneGerarchica("x/" + anno);
            newArchivio.setStato(Archivio.StatoArchivio.BOZZA);
        }
        // altro dato da settare a default per aiutare i trigger e/o varie automazioni
        newArchivio.setNumeroSottoarchivi(0);
        
        newArchivio.setIdArchivioArgo(null);
        newArchivio.setIdArchivioImportato(null);
        
        newArchivio.setDataCreazione(now);
        newArchivio.setDataInserimentoRiga(now);
        newArchivio.setVersion(now);
        
        // salvo e ricarico per aver l'id del nuovo archivio 
        entityManager.persist(newArchivio);
        entityManager.flush();
        entityManager.refresh(newArchivio);
        
        
        // setto id_radice e numero se si tratta di un fascicolo altrimenti solo l'id_radice prendendolo dal archivioDestinazione
        if(archivioDestinazione == null && archivioDaCopiare.getLivello() == 1){ // fascicolo
            newArchivio.setIdArchivioRadice(newArchivio);
            archivioRepository.numeraArchivio(newArchivio.getId());
        } else {
            newArchivio.setIdArchivioRadice(archivioDestinazione.getIdArchivioRadice());
        }
        
        // finisco settando gli attori
        setNewAttoriArchivioAttivita(newArchivio, archivioDaCopiare);
        
        // ricarico prima di ritornare newArchivio così da avere i dati compilati correttamente 
        // (tipo la numerazione gerarchica che viene generata lato db dalla funzione apposta e quindi non la ho lato be)
        entityManager.flush();
        entityManager.refresh(newArchivio);
        return newArchivio;
    }
    
    /**
     * Clono gli attori dell'archivio che sto duplicando e li metto a quello appena creato.
     * @param newArchivio Il nuovo archivio che riceverà gli attori.
     * @param archivioDaCopiare Il vecchio archivio da cui clonerò gli attori.
     */
    private void setNewAttoriArchivioAttivita(Archivio newArchivio, Archivio archivioDaCopiare){
        // preparo la lista che riemperò con tutti gli attori
        List<AttoreArchivio> attoriList = new ArrayList<AttoreArchivio>();

        for (AttoreArchivio attore: archivioDaCopiare.getAttoriList()){
            // creo un nuovo attore basndomi su quelli che sto ciclando
            AttoreArchivio newAttore = new AttoreArchivio(newArchivio, attore.getIdPersona(), attore.getIdStruttura(), attore.getRuolo());
            entityManager.persist(newAttore);
            entityManager.refresh(newAttore);
            attoriList.add(newAttore);
        }
        // setto gli attori appena clonati al nuovo archivio
        newArchivio.setAttoriList(attoriList);
    }
}
