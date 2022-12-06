package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.repositories.baborg.CambiamentiAssociazioneRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.jpa.natiquery.NativeQueryTools;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorkerData;
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
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 *
 * @author utente
 */
@MasterjobsWorker
public class ManageCambiAssociazioniJobWorker extends JobWorker {
    private static final Logger log = LoggerFactory.getLogger(ManageCambiAssociazioniJobWorker.class);

    @Autowired
    private CambiamentiAssociazioneRepository cambiamentiAssociazioneRepository;
    
    @Autowired
    PermissionRepositoryAccess permissionRepositoryAccess;
    
    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;
    
    @Autowired
    private PermissionManager permissionManager;
    
        
    @Autowired
    PermessoArchivioRepository permessoArchivioRepository;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    EntityManager entityManager;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("job %s started", getName()));
        
        /*Mi tiro su tutte le righe di cambiamento associazione che hanno una data di esecuzione minore di ora*/
        ManageCambiAssociazioniJobWorkerData data = getWorkerData(ManageCambiAssociazioniJobWorkerData.class);
        Iterable<CambiamentiAssociazione> cambiamentiAssociazioni = cambiamentiAssociazioneRepository.findAll(
                QCambiamentiAssociazione.cambiamentiAssociazione.fatto.eq(false).and(
                        QCambiamentiAssociazione.cambiamentiAssociazione.dataEsecuzione.loe(data.getDataRiferimento())), 
                Sort.by(Sort.Direction.ASC, QCambiamentiAssociazione.cambiamentiAssociazione.id.getAnnotatedElement().getDeclaredAnnotation(Column.class).name()));
        
        if (cambiamentiAssociazioni!= null) {
            Set<Integer> idUtenteSet = new HashSet();
            List<Integer> idCambiamentiAssociazioni = new ArrayList<>();
            Set<Integer> archiviDaPermessizzare = new HashSet<>();

            
            for (CambiamentiAssociazione cambiamentiAssociazione : cambiamentiAssociazioni) {
                
                Utente utente = cambiamentiAssociazione.getIdUtente();
                
                // if utente non già presente nel set altrimenti lo conto come già fatto, non voglio far calcoli sullo stesso utente. 
                if(!idUtenteSet.contains(utente.getId())){
                    idUtenteSet.add(utente.getIdPersona().getId());

                    /*Mi costruisco il soggetto per la richiesta alla BB*/
                    List<PermessoEntitaStoredProcedure> permessiPersonaSuArchivi = null ;
                    List<Integer> listIdPermessiSuArchivi = new ArrayList<>();
                    Set<Integer> setIdPermessiSuArchivi = new HashSet<>();

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
                       if(listIdPermessiSuArchivi != null && !listIdPermessiSuArchivi.isEmpty()) {
                           
                        try {
                            String archiviString;
                            archiviString = permessoArchivioRepository.getArchiviDaPermessizzareFromIdPermessi(utente.getIdPersona().getId(), utente.getIdAzienda().getId(), UtilityFunctions.getArrayString(objectMapper, listIdPermessiSuArchivi));
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
                           archiviDaPermessizzare.addAll(archiviDaPermessizzareUtente);
                       }
                }
                idCambiamentiAssociazioni.add(cambiamentiAssociazione.getId());
            }
            
            /*ora ho il set di tutti archivi di tutti gli utenti. Lo ciclo e faccio partire il job per il ricalcolo */
            if (!archiviDaPermessizzare.isEmpty()) {
                log.info("Vado a vedere su quali archivi bisogna ricalcolare i permessi");
                
                for (Integer archivio : archiviDaPermessizzare) {
                    CalcoloPermessiArchivioJobWorkerData calcoloPermessiArchivioJobWorkerData = new CalcoloPermessiArchivioJobWorkerData(archivio);
                    CalcoloPermessiArchivioJobWorker jobWorker = super.masterjobsObjectsFactory.getJobWorker(CalcoloPermessiArchivioJobWorker.class, calcoloPermessiArchivioJobWorkerData, false);
                  
                    try {
                        super.masterjobsJobsQueuer.queue(jobWorker, null, null, null, false, it.bologna.ausl.model.entities.masterjobs.Set.SetPriority.HIGHEST);
                    } catch (MasterjobsQueuingException ex) {
                        String errorMessage = String.format("Errore nell'accodamento di %s", CalcoloPermessiArchivioJobWorker.class.getSimpleName());
                        log.error(errorMessage, ex);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                    JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
                    jPAQueryFactory
                            .update(QCambiamentiAssociazione.cambiamentiAssociazione)
                            .set(QCambiamentiAssociazione.cambiamentiAssociazione.fatto, true)
                            .where(QCambiamentiAssociazione.cambiamentiAssociazione.id.in(idCambiamentiAssociazioni))
                            .execute();
                    log.info("ho settato il cambiamento associazione come fatto");
                }
            } else {
                log.warn("Non ho trovato nessun archivio su cui devo calcolare i permessi");
            }
            
        }
        log.info(String.format("job %s ended", getName()));

        return null;
    }
    
}
