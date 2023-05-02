package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.repositories.baborg.CambiamentiAssociazioneRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.model.entities.baborg.CambiamentiAssociazione;
import it.bologna.ausl.model.entities.baborg.QCambiamentiAssociazione;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 *
 * @author utente
 */
@MasterjobsWorker
public class ManageCambiAssociazioniJobWorker extends JobWorker<ManageCambiAssociazioniJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(ManageCambiAssociazioniJobWorker.class);
    
    private final String OBJECT_ID_JOB = "ManageCambiAssociazioniId";
    private final String TYPE_JOB = "ManageCambiAssociazioniType";

    @Autowired
    private CambiamentiAssociazioneRepository cambiamentiAssociazioneRepository;
    
    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;
    
    @Autowired
    private PermissionManager permissionManager;
    
    @Autowired
    private PermessoArchivioRepository permessoArchivioRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
   
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("job %s started", getName()));

        /* Mi tiro su tutte le righe di cambiamento associazione che hanno una data di esecuzione minore di ora */
        ManageCambiAssociazioniJobWorkerData data = getWorkerData();
        Iterable<CambiamentiAssociazione> cambiamentiAssociazioni = cambiamentiAssociazioneRepository.findAll(
                QCambiamentiAssociazione.cambiamentiAssociazione.fatto.eq(false).and(
                        QCambiamentiAssociazione.cambiamentiAssociazione.dataEsecuzione.loe(data.getDataRiferimento())), 
                Sort.by(Sort.Direction.ASC, QCambiamentiAssociazione.cambiamentiAssociazione.id.getAnnotatedElement().getDeclaredAnnotation(Column.class).name()));
        
        if (cambiamentiAssociazioni != null) {
            Set<Integer> idUtenteSet = new HashSet();
            List<Integer> idCambiamentiAssociazioni = new ArrayList<>();
            Set<Integer> idArchiviRadiceDaPermessizzare = new HashSet<>();

            /**
             * Ciclando sui cambaimenti associazione vado a recuperare un set di archivi che hanno bisogno del ricalcolo dei permessi
             */
            for (CambiamentiAssociazione cambiamentiAssociazione : cambiamentiAssociazioni) {
                
                Utente utente = cambiamentiAssociazione.getIdUtente();
                
                // if utente non già presente nel set altrimenti lo conto come già fatto, non voglio far calcoli sullo stesso utente. 
                if (!idUtenteSet.contains(utente.getId())) {
                    idUtenteSet.add(utente.getIdPersona().getId());

                    /*Mi costruisco il soggetto per la richiesta alla BB*/
                    List<PermessoEntitaStoredProcedure> permessiPersonaSuArchivi = null ;
                    List<Integer> listIdPermessiSuArchivi = new ArrayList<>();
//                    Set<Integer> setIdPermessiSuArchivi = new HashSet<>();

                    try {
                        log.info(String.format("sto chiedendo alla blackbox i permessi per la riga %s", cambiamentiAssociazione.getId()));

                        /* Mi reperisco la lista delle strutture in cui l'utente ha delle afferenze attive*/
                        List<Object> listIdStruttureAfferenze = null;
                        
                        if(utente.getAttivo()) {
                            listIdStruttureAfferenze = utenteStrutturaRepository.getListUtentiStrutturaAfferenzeAttiveByIdUtente(utente.getId()).stream().map(us -> us.getIdStruttura()).collect(Collectors.toList());      
                        }
                        
                        permessiPersonaSuArchivi = permissionManager.getPermissionsOfSubjectAdvanced(
                                utente.getIdPersona(),
                                null,
                                null,
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.SCRIPTA.toString()}),
                                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.ARCHIVIO.toString()}), 
                                true, 
                                null, 
                                null, 
                                listIdStruttureAfferenze, 
                                BlackBoxConstants.Direzione.PRESENTE
                        ); 
                        
                        /* Mi prendo tutti i permessi dell'utente passando anche i suoi soggetti virtuali*/
                                   
                    } catch (BlackBoxPermissionException ex) {
                        String errorMessage = String.format("Errore nella lettura dei permessi della struttura, nella riga %s", cambiamentiAssociazione.getId());
                        log.error(errorMessage, ex);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                    if (permessiPersonaSuArchivi != null && !permessiPersonaSuArchivi.isEmpty()) {
                        /* Creo il set dei permessi dell'utente */
                        permessiPersonaSuArchivi.stream().forEach(p -> {
                            p.getCategorie().forEach(c -> {
                                c.getPermessi().forEach(perm -> {
                                    if (!listIdPermessiSuArchivi.contains(perm.getId()))
                                    listIdPermessiSuArchivi.add(perm.getId());
                                });
                            });
                        });
                        
                       
                    }
                    /*chiamo la storedprocedure per ritornare tutti gli archivi, voglio farlo anche se l'utente non è attivo*/
                    List<Integer> archiviDaPermessizzareUtente = new ArrayList<>();
                    if (!listIdPermessiSuArchivi.isEmpty()) {
                     try {
                         String archiviString;
                         archiviString = permessoArchivioRepository.getArchiviRadiceDaPermessizzareFromIdPermessi(utente.getIdPersona().getId(), utente.getIdAzienda().getId(), UtilityFunctions.getArrayString(objectMapper, listIdPermessiSuArchivi));
                         if(archiviString != null && archiviString.contains(",") ) {
                             for (String field : archiviString.split(","))
                             archiviDaPermessizzareUtente.add(Integer.parseInt(field));
                         } else if (archiviString != null && !archiviString.contains(",")){
                             archiviDaPermessizzareUtente.add(Integer.parseInt(archiviString));
                         }


                     } catch (JsonProcessingException ex) {
                         java.util.logging.Logger.getLogger(ManageCambiAssociazioniJobWorker.class.getName()).log(Level.SEVERE, null, ex);
                     }
                    }
                    /*Metto tutti gli archivi all'interno della lista di tutti gli archivi da permessizzare di tutti gli utenti*/
                    if (!archiviDaPermessizzareUtente.isEmpty()) {
                        idArchiviRadiceDaPermessizzare.addAll(archiviDaPermessizzareUtente);
                        log.info("size: " + idArchiviRadiceDaPermessizzare.size());
                    }
                }
                idCambiamentiAssociazioni.add(cambiamentiAssociazione.getId());
            }
            
            /* Ora ho il set idArchiviRadiceDaPermessizzare contenente tutti gli archivi radice per il quale inseriremo:
                - I job per il ricalcolo dei permessi sull'alberatura archivio
                - Un job per il calcolo delle persone vedenti sui documenti contenuti nei vari archivi
            */
            if (!idArchiviRadiceDaPermessizzare.isEmpty()) {
                
                log.info("Ciclo gli id Archivi Radice per ricalcolarne i permessi");
                
                AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
                
                for (Integer idArchivioRadice : idArchiviRadiceDaPermessizzare) {
                    /**
                     * 
                    * NB(!DEPRECATED! Vedi NB2): Questi job vengono inseriti con waitForObject a false. Ma viene messo l'ObjectID OBJECT_ID_JOB con type TYPE_JOB
                    * in modo che successivamente quando verra messo il JOB CalcolaPersoneVedentiDaArchivi, questo avrà
                    * il waitForObject a TRUE sullo stesso identificativo. Così verranno calcolate le persone vedenti dei doc solo al termine del calcolo
                    * dei permessi espliciti degli archivi.
                    * 
                    * NB2: Quanto detto qui sopra è falso, infatti ho commentato le righe di accodamento di CalcolaPersoneVedentiDaArchiviRadice
                    * il motivo è che essendoc he ora CalcolaPermessiGerarchiaArchivio di fatto accoda poi un job per ogni singolo archivio ed essendo che il calcolo sulle perosne vedenti
                    * lo si vuole effettuare solo dopo aver calcolato i permessi arhcivi allora l'unico modo che ho trovato è stato mettere l'accodamento di CalcolaPersoneVedentiDaArchiviRadice
                    * solo dopo aver accdoato il calcolo permessi archvi di ogni archivio (tutti avranno l'object id che sarà l'id arhdivio radice)
                    */
                    accodatoreVeloce.accodaCalcolaPermessiGerarchiaArchivio(idArchivioRadice, OBJECT_ID_JOB, TYPE_JOB, null);
                }
                
//                log.info("Inserisco il job CalcolaPersoneVedentiDaArchiviRadice");
//                
//                accodatoreVeloce.accodaCalcolaPersoneVedentiDaArchiviRadice(idArchiviRadiceDaPermessizzare, OBJECT_ID_JOB, TYPE_JOB, null);
            } else {
                log.warn("Non ho trovato nessun archivio coinvolto dai cambiamenti associazione presi in esame");
            }
            
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
            jPAQueryFactory
                    .update(QCambiamentiAssociazione.cambiamentiAssociazione)
                    .set(QCambiamentiAssociazione.cambiamentiAssociazione.fatto, true)
                    .where(QCambiamentiAssociazione.cambiamentiAssociazione.id.in(idCambiamentiAssociazioni))
                    .execute();
            log.info("Ho settato i cambiamenti associazione come fatti");
        }
        log.info(String.format("job %s ended", getName()));

        return null;
    }
    
}
