package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.cambioprofilo;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ProfiliPredicatiRuoliRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ProfiliRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.ProfiliPredicatiRuoli;
import it.bologna.ausl.model.entities.baborg.QProfiliPredicatiRuoli;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

/**
 *
 * @author mido
 */
@MasterjobsWorker
public class CambioProfiloJobWorker extends JobWorker<CambioProfiloJobWorkerData, JobWorkerResult> {

    private static final Logger log = LoggerFactory.getLogger(CambioProfiloJobWorker.class);
    private final String name = CambioProfiloJobWorker.class.getSimpleName();

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ProfiliRepository profiliRepository;

    @Autowired
    private ProfiliPredicatiRuoliRepository profiliPredicatiRuoliRepository;

    @Autowired
    private PermissionManager permissionManager;

    @Value("${internauta.cache.redis.prefix}")
    private String prefixInternauta;

    @Autowired
    @Qualifier(value = "redisCache")
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio", getName());
        //carico l'azienda
        Integer idAzienda = getWorkerData().getIdAzienda();
        Azienda azienda = aziendaRepository.findById(idAzienda).get();

        //carico la persona a cui devo spegnere/accendere i permessi/ruoli
        Persona persona = personaRepository.findById(getWorkerData().getIdPersona()).get();

        //carico l'utente a cui devo spegnere/accendere i permessi/ruoli
        Utente utente = utenteRepository.findByIdAziendaAndIdPersona(azienda, persona);
//      carico tutti i predicati dei permessi che servono al profilo nuovo
        String profilNew = getWorkerData().getProfiloNew();
        List<String> predicatiNew = new ArrayList<>();
        List<Ruolo> ruoliNew = new ArrayList<>();
        List<ProfiliPredicatiRuoli> pprNews = new ArrayList<>();
        if (profilNew != null) {
            pprNews = processProfiliPredicatiRuoli(profilNew, predicatiNew, ruoliNew);
        }
//      baborg.profili_predicati_ruoli è la tabella di congiunzione tra profili e (predicati o ruoli)
//      carico tutti i predicati dei permessi che servivano al profilo vecchio
        String profilOld = getWorkerData().getProfiloOld();
        List<ProfiliPredicatiRuoli> pprDaAttivare = null;
        if (profilOld != null) {
            List<String> predicatiOld = new ArrayList<>();
            List<Ruolo> ruoliOld = new ArrayList<>();
            List<ProfiliPredicatiRuoli> pprOld = processProfiliPredicatiRuoli(profilOld, predicatiOld, ruoliOld);

            //spengo i permessi coi preficatiOld che non sono in predicatiNew
            List<String> predicatiOldC = new ArrayList<>(predicatiOld);
            predicatiOld.removeAll(predicatiNew);

            deletePermissions(pprOld, predicatiOld, persona);
            deletePermissions(pprOld, predicatiOld, utente);

            //accendo i nuovi permessi che hanno predicati in profilo new e non in old
            if (profilNew != null) {
                predicatiNew.removeAll(predicatiOldC);
                pprDaAttivare = pprNews.stream().filter(pprNew -> predicatiNew.contains(pprNew.getPredicato())).collect(Collectors.toList());
            }
        }

        if (profilNew != null) {
            Object soggetto = new Object();
            Object oggetto = new Object();
            //se è null vuol dire che non ho un old e quindi devo sicuramente inserire i  nuovi 
            if (pprDaAttivare == null) {
                pprDaAttivare = pprNews;
            }
            //se non è vuoto devo attivare alcuni permessi
            if (!pprDaAttivare.isEmpty()) {
                //setto il ruolo a 1 (utente generico)
                Integer sommaRuoliUtente = 1;
                Integer sommaRuoliPersona = 1;
                for (ProfiliPredicatiRuoli ppr : pprDaAttivare) {
                    if (ppr.getPredicato() != null) {
                        if (null == ppr.getTipoSoggetto()) {
                            String errorMsg = "Errore nell'inserimento del permesso Tipologia soggetto non riconosciuto " + ppr.getTipoSoggetto().toString();
                            log.error(errorMsg);
                            throw new MasterjobsWorkerException(errorMsg);
                        } else {
                            //creo il soggetto per la blackbox
                            switch (ppr.getTipoSoggetto()) {
                                case UTENTE:
                                    soggetto = utente;
                                    break;
                                case PERSONA:
                                    //se il tipo di soggetto è PERSONA
                                    soggetto = persona;
                                    break;
                                default:
                                    String errorMsg = "Errore nell'inserimento del permesso Tipologia soggetto non riconosciuto " + ppr.getTipoSoggetto().toString();
                                    log.error(errorMsg);
                                    throw new MasterjobsWorkerException(errorMsg);
                            }
                        }

                        if (null == ppr.getTipoOggetto()) {
                            String errorMsg = "Errore nell'inserimento del permesso Tipologia soggetto non riconosciuto " + ppr.getTipoSoggetto().toString();
                            log.error(errorMsg);
                            throw new MasterjobsWorkerException(errorMsg);
                        } else {
                            //creo l'oggetto per la blackbox
                            switch (ppr.getTipoOggetto()) {
                                case STRUTTURE:
                                    for (UtenteStruttura us : utente.getUtenteStrutturaList()) {
                                        if (
                                            us.getIdAfferenzaStruttura().getCodice() == AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA &&
                                            us.getAttivo()) {
                                            oggetto = us.getIdStruttura();
                                            break;
                                        }
                                    }
                                    break;
                                case ARCHIVI:
                                case PEC:
                                case UTENTI:
                                case CONTATTI:
                                default:
                                    String errorMsg = "Errore nell'inserimento del permesso Tipologia soggetto non riconosciuto " + ppr.getTipoSoggetto().toString();
                                    log.error(errorMsg);
                                    throw new MasterjobsWorkerException(errorMsg);
                            }
                        }

                        try {
                            permissionManager.insertSimplePermission(soggetto, oggetto, ppr.getPredicato(), name, false, false, ppr.getTipoAmbito().toString(), ppr.getTipoPermesso().toString());
                        } catch (BlackBoxPermissionException ex) {
                            String errorMsg = "Errore nell'inserimento del permesso qualcosa è andato storto";
                            log.error(errorMsg, ex);
                            throw new MasterjobsWorkerException(errorMsg, ex);
                        }
                    } else {
                        //inizio a gestire la parte di ruoli
                        //qui non devo tenere conto delle date
                        //aggiungo al valore del ruolo quello che serve per il profilo
                        if (ppr.getIdRuolo() != null) {
                            if (ppr.getTipoSoggetto().equals(ProfiliPredicatiRuoli.TipoSoggetto.UTENTE)) {
                                sommaRuoliUtente += ppr.getIdRuolo().getMascheraBit();
                            } else {
                                sommaRuoliPersona += ppr.getIdRuolo().getMascheraBit();
                            }
                        }
                    }
                }
                //setto il ruolo ottenuto sulla persona / sull'utente e salvo
                persona.setBitRuoli(sommaRuoliPersona);
                utente.setBitRuoli(sommaRuoliUtente);
                personaRepository.save(persona);
                utenteRepository.save(utente);
            }            
        }

        // cancello le chiavi cache che interessano perchè sono cambiati i permessi e in matrice non si vedono
        cleanUtenteStrutturaCache(utente);
        return null;
    }

    private void cleanUtenteStrutturaCache(Utente utente) {
        String prefix = "getPermessiFilteredByAdditionalData__ribaltorg__";
        String params = prefixInternauta + prefix + "::" + utente.toString().replace("[", "\\[").replace("]", "\\]") + "*";
        log.info(String.format("pulisco la cache di utente_struttura con la chiave: %s", params));
        Set<String> keys = redisTemplate.keys(params);
        redisTemplate.delete(keys);

        // gestione storico
        prefix = "getPermessiFilteredByAdditionalDataByIdUtente__ribaltorg__";
        params = prefixInternauta + prefix + "::" + utente.getId() + "*";
        log.info(String.format("pulisco la cache di utente_struttura_storico con la chiave: %s", params));
        Set<String> keys2 = redisTemplate.keys(params);
        redisTemplate.delete(keys2);
    }

    @Override
    public String getName() {
        return this.name;
    }

    private List<ProfiliPredicatiRuoli> processProfiliPredicatiRuoli(String profilo, List<String> predicati, List<Ruolo> ruoli) {

        BooleanExpression profiloExpr = QProfiliPredicatiRuoli.profiliPredicatiRuoli.idProfilo.id.eq(profilo);
//        Iterable<ProfiliPredicatiRuoli> pprList = profiliPredicatiRuoliRepository.findAll(profiloExpr);

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<ProfiliPredicatiRuoli> predicatiRuoliDelProfilo = queryFactory
                .select(QProfiliPredicatiRuoli.profiliPredicatiRuoli)
                .from(QProfiliPredicatiRuoli.profiliPredicatiRuoli)
                .where(profiloExpr)
                .fetch();
        List<ProfiliPredicatiRuoli> ppr = new ArrayList<>();
        if (predicatiRuoliDelProfilo != null) {
            for (ProfiliPredicatiRuoli pprTmp : predicatiRuoliDelProfilo) {

                if (pprTmp.getIdRuolo() != null) {
                    ruoli.add(pprTmp.getIdRuolo());
                } else {
                    predicati.add(pprTmp.getPredicato());
                }
                ppr.add(pprTmp);
            }
        }
        return ppr;
    }

    private void deletePermissions(List<ProfiliPredicatiRuoli> pprs, List<String> predicati, Object entitySoggetto) throws MasterjobsWorkerException {
        if (predicati != null) {
            for (String predicato : predicati) {
                try {
                    for (ProfiliPredicatiRuoli ppr : pprs) {
                        if (ppr.getPredicato().equalsIgnoreCase(predicato)) {
                            permissionManager.deletePermission(entitySoggetto, null, predicato, null, null, null, ppr.getTipoAmbito().toString(), ppr.getTipoPermesso().toString());
                        }
                    }
                } catch (BlackBoxPermissionException ex) {
                    String errorMsg;
                    try {
                        String name = UtilityFunctions.getFirstAnnotationOverEntity(entitySoggetto.getClass(), Table.class).name();
                    } catch (ClassNotFoundException err) {
                        errorMsg = "Errore nellerrore";
                        log.error(errorMsg, err);
                        throw new MasterjobsWorkerException(errorMsg, err);
                    }
                    try {
                        errorMsg = "Errore nell'eliminazione dei permessi per il soggetto" + name + UtilityFunctions.getPkValue(entitySoggetto);
                    } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex1) {
                        errorMsg = "Errore nellerrore";
                        log.error(errorMsg, ex1);
                        throw new MasterjobsWorkerException(errorMsg, ex1);
                    }
                    log.error(errorMsg, ex);
                    throw new MasterjobsWorkerException(errorMsg, ex);
                }
            }
        }
    }

}
