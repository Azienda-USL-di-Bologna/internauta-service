package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sendmail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.SimpleMailSenderUtility;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Invia una mail leggendo i parametri aziendali e secondo i parametri specificati.
 * Oggetto e testo sono creati a partire dal parametro aziendale attivitaMailSender nella tabella configurazione.parametri_aziende.
 * @author gdm
 */
@MasterjobsWorker
public class SendAttivitaMailJobWorker extends JobWorker<SendAttivitaMailJobWorkerData, JobWorkerResult> {
    private static Logger log = LoggerFactory.getLogger(SendAttivitaMailJobWorker.class);
    
    private static final String EVERY_ACTIVITIES_KEY = "everyActivities";
    private static final String TEMPLATE_KEY = "template";
    
    @Autowired
    private SimpleMailSenderUtility simpleMailSenderUtility;
    
    @Autowired
    private ParametriAziendeReader parametriAziendeReader;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    private String name = SendAttivitaMailJobWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("avvio del job %s per la mail %s...", getName(), getWorkerData().getTo()));
        SendAttivitaMailJobWorkerData data = getWorkerData();
        Boolean mailSent = false;
        
        Map<String, String> activityMailTemplate = getActivityTemplate();
        
        String subject = activityMailTemplate.get("subject");
        String body;
        try {
            body = buildBody(activityMailTemplate);
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del body della mail";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        String fromAlias = activityMailTemplate.get("fromAlias");
        
        try {
            mailSent = simpleMailSenderUtility.sendMail(
                    data.getIdAzienda(),
                    fromAlias,
                    subject,
                    data.getTo(),
                    body,
                    null,
                    null,
                    null,
                    null,
                    true);
        } catch (Exception ex) {
            String errorMessage = "errore nell'invio della mail";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        if (!mailSent) {
            throw new MasterjobsWorkerException("la sendMail() ha tornato false");
        }
        return null;
    }
    
    /**
     * Legge il template per la creazione della mail dal parametro attivitaMailSender dell'azienda del job dalla tabella 
     * configurazione.parametri_aziende
     * @return il template per la creazione della mail
     * @throws MasterjobsWorkerException 
     */
    private Map<String, String> getActivityTemplate() throws MasterjobsWorkerException {
        List<ParametroAziende> parameters = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.attivitaMailSender, new Integer[]{getWorkerData().getIdAzienda()});
        if (parameters == null || parameters.isEmpty()) {
            String errorMessage = String.format("manca il paramentro %s in configurazione.parametri_aziende", ParametriAziendeReader.ParametriAzienda.attivitaMailSender);
            log.error(errorMessage);
            throw new MasterjobsWorkerException(errorMessage);
        }
        Map<String, Object> value = parametriAziendeReader.getValue(parameters.get(0), new TypeReference<Map<String, Object>>(){});
        Object everyActivityParamsObj = value.get(EVERY_ACTIVITIES_KEY);
        if (everyActivityParamsObj == null) {
            String errorMessage = String.format("il parametro %s in configurazione.parametri_aziende non contiene la chiave %s", ParametriAziendeReader.ParametriAzienda.attivitaMailSender, EVERY_ACTIVITIES_KEY);
            log.error(errorMessage);
            throw new MasterjobsWorkerException(errorMessage);
        }
        
        Map<String, Object> everyActivityParams;
        try {
            everyActivityParams = 
                    objectMapper.convertValue(everyActivityParamsObj, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            String errorMessage = String.format("errore nella lettura della chiave %s del paramentro %s in configurazione.parametri_aziende, dovrebbe essere Map<String, Object>, forse non è nel formato corretto?", EVERY_ACTIVITIES_KEY, ParametriAziendeReader.ParametriAzienda.attivitaMailSender);
            log.error(errorMessage);
            throw new MasterjobsWorkerException(errorMessage);
        }
        
        Object activityMailTemplateObj = everyActivityParams.get(TEMPLATE_KEY);
        if (activityMailTemplateObj == null) {
            String errorMessage = String.format("il parametro %s in configurazione.parametri_aziende non contiene la chiave %s", ParametriAziendeReader.ParametriAzienda.attivitaMailSender, TEMPLATE_KEY);
            log.error(errorMessage);
            throw new MasterjobsWorkerException(errorMessage);
        }
        
        Map<String, String> activityMailTemplate;
        try {
            activityMailTemplate = 
                    objectMapper.convertValue(activityMailTemplateObj, new TypeReference<Map<String, String>>() {});
        } catch (Exception ex) {
            String errorMessage = String.format("errore nella lettura della chiave %s del paramentro %s in configurazione.parametri_aziende, dovrebbe essere Map<String, String>, forse non è nel formato corretto?", TEMPLATE_KEY, ParametriAziendeReader.ParametriAzienda.attivitaMailSender);
            log.error(errorMessage);
            throw new MasterjobsWorkerException(errorMessage);
        }
        return activityMailTemplate;
    }
    
    /**
     * Genera il body della mail
     * @param activityMailTemplate il template per la generazione della mail, letto dalla tabella configuzione.parametri_aziende
     * @return il body della mail, al quale vengono sostituiti i segnaposto con i valori effettivi
     * @throws IOException 
     */
    private String buildBody(Map<String, String> activityMailTemplate) throws IOException {
        String bodyTemplate = activityMailTemplate.get("body");
        Applicazione applicazione = cachedEntities.getApplicazione(Applicazione.Applicazioni.scrivania.toString());
        Azienda azienda = cachedEntities.getAzienda(getWorkerData().getIdAzienda());
        AziendaParametriJson aziendaParametriJson = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        String linkScrivania = aziendaParametriJson.getBasePath() + applicazione.getBaseUrl();
        
        bodyTemplate = bodyTemplate.replace("[oggetto]", getWorkerData().getOggettoAttivita());
        bodyTemplate = bodyTemplate.replace("[link]", linkScrivania);
        bodyTemplate = bodyTemplate.replace("[link_name]", linkScrivania);
        
        return bodyTemplate;
    }
    
}
