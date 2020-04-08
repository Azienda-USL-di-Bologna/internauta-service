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

    ParametersManagerInterface parametersManager;

    final String ATTIVITA_SUPPORTO_VALUE = "Attività Supporto";
    final String SEGNALAZIONE_UTENTE_VALUE = "Segnalazione utente";
    final String TEMPO_CHIAMATA_VALUE = "Tempo di chiamata";
    final String TEMPO_DI_CHIAMATA_NESSUNO = "nessuno";
    final String TIPO_DI_CHIAMATA_VALUE = "Tipo Chiamata ";
    final String TIPO_DI_CHIAMATA_BABEL_FORM = "Babelform";
    final String AZIENDA_VALUE = "Azienda";
    final String UTENTE_AFFLITTO_KEY = "Utente afflitto (username)";
    final String UTENTE_SEGNALANTE_KEY = "Utente segnalante (username)";
    final String PROJECT_ID_KEY = "project_id";
    final String PRIORITY_ID_KEY = "priority_id";
    final String TRACKER_ID_KEY = "tracker_id";
    final String STATUS_ID_KEY = "status_id";
    final String ID_KEY = "id";
    final String VALUE_KEY = "value";

    public IssueWrapper(ParametersManagerInterface pm) {
        parametersManager = pm;
    }

    private JSONArray getCustomFiledsJSONArray(Segnalazione segnalazione) throws Exception {
        try {
            JSONArray array = new JSONArray();
            RedmineCustmoFiledsManager manager = (RedmineCustmoFiledsManager) RedmineManagerFactory.getRedmineCustomFieldManager(parametersManager);

            String user = segnalazione.getCognome() + " " + segnalazione.getNome()
                    + " (" + segnalazione.getUsername() + ")";

            JSONObject utenteSegnalante = new JSONObject();
            utenteSegnalante.put(ID_KEY, manager.getCustomFieldIdByName(UTENTE_SEGNALANTE_KEY).toString());
            utenteSegnalante.put(VALUE_KEY, user);

            JSONObject utenteAfflitto = new JSONObject();
            utenteAfflitto.put(ID_KEY, manager.getCustomFieldIdByName(UTENTE_AFFLITTO_KEY).toString());
            utenteAfflitto.put(VALUE_KEY, user);

            JSONObject azienda = new JSONObject();
            azienda.put(ID_KEY, manager.getCustomFieldIdByName(AZIENDA_VALUE).toString());
            azienda.put(VALUE_KEY, AziendaWrapper.convertAziendaToAvailableValues(segnalazione.getAzienda()));

            JSONObject tempoChiamata = new JSONObject();
            tempoChiamata.put(ID_KEY, manager.getCustomFieldIdByName(TEMPO_CHIAMATA_VALUE).toString());
            tempoChiamata.put(VALUE_KEY, TEMPO_DI_CHIAMATA_NESSUNO);

            array.put(utenteSegnalante);
            array.put(utenteAfflitto);
            array.put(azienda);
            array.put(tempoChiamata);

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

    public JSONObject buildAndReturnMiddleMineIssueBySegnalazione(Segnalazione segnalazione) throws RedmineException {
        try {
            JSONObject issue = getDefaultJsonIssue();
            issue.put("subject", segnalazione.getOggetto());
            issue.put("description", segnalazione.getDescrizione());
            issue.put("custom_fields", getCustomFiledsJSONArray(segnalazione));
            return issue;
        } catch (Exception ex) {
            throw new RedmineException("Errore durante la build dei parametri della segnlazione: " + ex.getMessage(), ex);
        }
    }
}
