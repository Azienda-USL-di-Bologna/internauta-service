package it.bologna.ausl.internauta.service.utils.redmine.wrappers;

import com.taskadapter.redmineapi.RedmineException;
import it.bologna.ausl.internauta.service.utils.redmine.factories.RedmineManagerFactory;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineAbstractManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineCustmoFiledsManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineIssueStatusManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineProjectManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineTrackerManager;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.model.entities.forms.Segnalazione;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Salo
 */
public class IssueWrapper {

    private enum Priorities {
        URGENTE(4),
        ALTA(3),
        NORMALE(2),
        BASSA(1);

        private final int levelCode;

        Priorities(int levelCode) {
            this.levelCode = levelCode;
        }

        public int getLevelCode() {
            return this.levelCode;
        }
    }

    private ParametersManagerInterface parametersManager;
    private RedmineCustmoFiledsManager manager;

    // CUSTOM FIELD
    final String TEMPO_CHIAMATA = "Tempo di chiamata";
    final String TELEFONO_UTENTE = "Telefono utente";
    final String EMAIL_UTENTE = "Email utente";
    final String TIPO_DI_CHIAMATA = "Tipo Chiamata ";
    final String UTENTE_AFFLITTO = "Utente afflitto (username)";
    final String UTENTE_SEGNALANTE = "Utente segnalante (username)";
    final String AZIENDA = "Azienda";
    final String STRUTTURA_UTENTTE = "Struttura Utente";
    final String DATA_MAIL_SEGNALAZIONE = "Data mail segnalazione";

    // VALUES
    final String ATTIVITA_SUPPORTO_VALUE = "Attività Supporto";
    final String SEGNALAZIONE_UTENTE_VALUE = "Segnalazione utente";
    final String TEMPO_DI_CHIAMATA_NESSUNO = "nessuno";
    final String BABEL_FORM_TIPO_DI_CHIAMATA = "Babelform";

    // JSON_KEYS
    final String PROJECT_ID_KEY = "project_id";
    final String PRIORITY_ID_KEY = "priority_id";
    final String TRACKER_ID_KEY = "tracker_id";
    final String STATUS_ID_KEY = "status_id";
    final String SUBJECT_KEY = "subject";
    final String DESCRIPTION_KEY = "description";
    final String CUSTOM_FIELDS_KEY = "custom_fields";

    final String ID_KEY = "id";
    final String VALUE_KEY = "value";

    public IssueWrapper(ParametersManagerInterface pm) {
        parametersManager = pm;
    }

    /**
     * Questo metodo chiama redmine e chiede l'id del campo e lo usa come chiave
     * del json da tornare, mentre il valore è il valore deciso da noi.
     */
    private JSONObject getNewJsonCustomFieldParamFromRedmine(String fieldName, String valString) throws RedmineException {
        JSONObject param = new JSONObject();
        param.putOnce(ID_KEY, manager.getCustomFieldIdByName(fieldName).toString());
        param.putOnce(VALUE_KEY, valString);
        return param;
    }

    private JSONArray getCustomFiledsJSONArray(Segnalazione segnalazione) throws Exception {
        try {
            JSONArray array = new JSONArray();
            RedmineCustmoFiledsManager manager = (RedmineCustmoFiledsManager) RedmineManagerFactory.getRedmineCustomFieldManager(parametersManager);

            String user = segnalazione.getCognome() + " " + segnalazione.getNome()
                    + " (" + segnalazione.getUsername() + ")";

            // chiama diverse volte getNewJsonCustomFieldParamFromRedmine(key, val)
            // UTENTE SEGNALANTE
            array.put(getNewJsonCustomFieldParamFromRedmine(UTENTE_SEGNALANTE.toString(),
                    user));
            // UTENTE AFFLITTO
            array.put(getNewJsonCustomFieldParamFromRedmine(UTENTE_AFFLITTO.toString(),
                    user));
            //TELEFONO UTENTE
            array.put(getNewJsonCustomFieldParamFromRedmine(TELEFONO_UTENTE.toString(),
                    segnalazione.getTelefono()));
            //MAIL UTENTE
            array.put(getNewJsonCustomFieldParamFromRedmine(EMAIL_UTENTE.toString(),
                    segnalazione.getMail()));
            //TIPO DI CHIAMATA
            array.put(getNewJsonCustomFieldParamFromRedmine(TIPO_DI_CHIAMATA.toString(),
                    BABEL_FORM_TIPO_DI_CHIAMATA.toString()));
            //AZIENDA
            array.put(getNewJsonCustomFieldParamFromRedmine(AZIENDA.toString(),
                    AziendaWrapper.convertAziendaToAvailableValues(segnalazione.getAzienda())));
            //TEMPO DI CHIAMATA
            array.put(getNewJsonCustomFieldParamFromRedmine(TEMPO_CHIAMATA.toString(),
                    TEMPO_DI_CHIAMATA_NESSUNO.toString()));
            //DATA MAIL SEGNALAZIONE
            array.put(getNewJsonCustomFieldParamFromRedmine(DATA_MAIL_SEGNALAZIONE.toString(),
                    new SimpleDateFormat("yyyy-MM-dd").format(new Date()).toString()));
            //STRUTTURA UTENTTE
            array.put(getNewJsonCustomFieldParamFromRedmine(STRUTTURA_UTENTTE.toString(),
                    segnalazione.getStruttura()));
            
            return array;
        } catch (RedmineException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new Exception("Errore nella creazione del json dei parametri custom segnalazione: " + ex.getMessage(), ex);
        }
    }

    private Integer getAttiviaSupportoStatusId() throws Exception {
        try {
            RedmineIssueStatusManager redmineIssueStatusManager = (RedmineIssueStatusManager) RedmineManagerFactory.getRedmineIssueStatusManager(parametersManager);
            return redmineIssueStatusManager.getIssueStatusIdByName(ATTIVITA_SUPPORTO_VALUE);
        } catch (Exception ex) {
            throw new RedmineException("Errore durante il reperimento dello status Attivita di Supporto: " + ex.getMessage(), ex);
        }
    }

    private Integer getBabelProjectId() throws Exception {
        try {
            RedmineProjectManager pm = (RedmineProjectManager) RedmineManagerFactory.getRedmineProjectManager(parametersManager);
            Integer babelProjectId = pm.getBabelProjectId();
            return babelProjectId;
        } catch (Exception ex) {
            throw new RedmineException("Errore durante il reperimento del Progect Babel da Redmine: " + ex.getMessage(), ex);
        }
    }

    private Integer getSegnalazioneUtenteTrackerId() throws RedmineException {
        try {
            RedmineTrackerManager redmineTrackerManager = (RedmineTrackerManager) RedmineManagerFactory.getRedmineTrakManager(parametersManager);
            return redmineTrackerManager.getTrackeByName(SEGNALAZIONE_UTENTE_VALUE);
        } catch (Exception ex) {
            throw new RedmineException("Errore durante la creazione dei parametri di default: " + ex.getMessage(), ex);
        }
    }

    private JSONObject getDefaultJsonIssue() throws RedmineException {
        try {
            JSONObject issue = new JSONObject();
            issue.put(PROJECT_ID_KEY, getBabelProjectId());
            issue.put(PRIORITY_ID_KEY, Priorities.NORMALE.getLevelCode());
            issue.put(TRACKER_ID_KEY, getSegnalazioneUtenteTrackerId());
            issue.put(STATUS_ID_KEY, getAttiviaSupportoStatusId());
            return issue;
        } catch (Exception ex) {
            throw new RedmineException("Errore durante la creazione dei parametri di default: " + ex.getMessage(), ex);
        }
    }

    public void buildRedmineCustmoFiledsManager() {
        manager = (RedmineCustmoFiledsManager) RedmineManagerFactory.getRedmineCustomFieldManager(parametersManager);
    }

    public JSONObject buildAndReturnMiddleMineIssueBySegnalazione(Segnalazione segnalazione) throws RedmineException {
        try {
            buildRedmineCustmoFiledsManager();
            JSONObject issue = getDefaultJsonIssue();
            issue.put(SUBJECT_KEY.toString(), segnalazione.getOggetto());
            issue.put(DESCRIPTION_KEY.toString(), segnalazione.getDescrizione().replaceAll("\n\r", "<br>").replaceAll("\n", "<br>"));
            issue.put(CUSTOM_FIELDS_KEY.toString(), getCustomFiledsJSONArray(segnalazione));
            return issue;
        } catch (Exception ex) {
            throw new RedmineException("Errore durante la build dei parametri della segnlazione: " + ex.getMessage(), ex);
        }
    }
}
