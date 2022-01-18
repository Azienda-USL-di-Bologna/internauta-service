/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.controllers.tools.ToolsCustomController;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ParametroAziendeRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.handlers.ParametroAziendeInvioMailNotificaAttivitaHandler;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.internauta.service.utils.SimpleMailSenderUtility;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class InviaNotificaAttivitaSospeseWorker implements Runnable {

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    SimpleMailSenderUtility simpleMailSenderUtility;

    @Autowired
    InternautaUtils internautaUtils;

    @Autowired
    ParametroAziendeRepository parametroAziendeRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    AttivitaRepository attivitaRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    @Value("${internauta.mode}")
    private String internautaMode;

    @Value("${internauta.scheduled.invio-mail-notifica-attivita-sospese.enabled-emails-test}")
    private String[] enabledEmailsForTest;

    private static final Logger log = LoggerFactory
            .getLogger(InviaNotificaAttivitaSospeseWorker.class);

    private static List<Integer> idPersoneAvvisate = new ArrayList<>();
    //private Integer idAzienda;
    private List<Azienda> listaAziende;
    private ParametroAziendeInvioMailNotificaAttivitaHandler handler;

    public void setParameter(List<Integer> idPersoneAvvisate, ParametroAziendeInvioMailNotificaAttivitaHandler handler) {
        this.idPersoneAvvisate = idPersoneAvvisate;
        //this.idAzienda = idAzienda;
        this.handler = handler;
    }

    private void loadAziende() {
        log.info("Load aziende");
        listaAziende = aziendaRepository.findAll();
    }

    private Azienda getAziendaById(Integer idAzienda) {
        Azienda toReturn = null;
        for (Azienda azienda : listaAziende) {
            if (azienda.getId().equals(idAzienda)) {
                toReturn = azienda;
            }
        }
        return toReturn;
    }

    private String getNomeAzienda(Integer idAzienda) {
        Azienda azienda = getAziendaById(idAzienda);
        return azienda.getNome();
    }

    //NO NO NO
    private String preparaListaAttivitaDaMostrareHTML(List<Attivita> listaAttivita) {
        final String format = "<tr><td>%-25s</td><td>%-25s</td><td>%-40s</td><td>%-40s</td><td>%s</td></tr>";
        // Azienda, Data, Tipo, Provenienza, Oggetto attivita
        String tabella = "<table>"
                + String.format(format, "Azienda", "Data", "Tipo", "Provenienza", "Oggetto");
        for (Attivita attivita : listaAttivita) {
            String provenienza = attivita.getProvenienza() != null && attivita.getProvenienza().length() > 40
                    ? attivita.getProvenienza().substring(0, 37) + "..."
                    : attivita.getProvenienza();
            String oggetto = attivita.getOggetto() != null && attivita.getOggetto().length() > 40
                    ? attivita.getOggetto().substring(0, 37) + "..."
                    : attivita.getOggetto();

            tabella += String.format(format, getNomeAzienda(attivita.getIdAzienda().getId()),
                    attivita.getData().format(DateTimeFormatter.ofPattern("dd/MM/uuuu")).toString(),
                    attivita.getDescrizione(),
                    provenienza,
                    attivita.getOggetto());
        }
        return tabella + "</table>";
    }

    private String preparaListaAttivitaDaMostrare(List<Attivita> listaAttivita) {
        final String format = "%-25s %-25s %-40s  %-40s %s\n";
        // Azienda, Data, Tipo, Provenienza, Oggetto attivita
        String tabella = String.format(format, "Azienda", "Data", "Tipo", "Provenienza", "Oggetto");
        for (Attivita attivita : listaAttivita) {
            String provenienza = attivita.getProvenienza() != null && attivita.getProvenienza().length() > 40
                    ? attivita.getProvenienza().substring(0, 37) + "..."
                    : attivita.getProvenienza();
            String oggetto = attivita.getOggetto() != null && attivita.getOggetto().length() > 40
                    ? attivita.getOggetto().substring(0, 37) + "..."
                    : attivita.getOggetto();

            tabella += String.format(format, getNomeAzienda(attivita.getIdAzienda().getId()),
                    attivita.getData().format(DateTimeFormatter.ofPattern("dd/MM/uuuu")).toString(),
                    attivita.getDescrizione(),
                    provenienza,
                    attivita.getOggetto());
        }
        return tabella;
    }

    private String getMailMittente() {
        String mittente = null;
        try {
            Azienda azienda = getAziendaById(handler.getIdAzienda());
            JSONObject parametriAziendaJSON = new JSONObject(azienda.getParametri());
            JSONObject mailParams = (JSONObject) parametriAziendaJSON.get("mailParams");
            mittente = (String) mailParams.get("mailFrom");
        } catch (Throwable ex) {
            log.error("ERRRORE: Impossibile recuperare il parametro del mittente", ex);
        }

        return mittente;
    }

    private String recuperaIndirizzoLogin() {
        String indirizzo = "";
        try {
            Azienda azienda = getAziendaById(handler.getIdAzienda());
            JSONObject parametriAziendaJSON = new JSONObject(azienda.getParametri());
            System.out.println("parametriAziendaJSON: " + parametriAziendaJSON.toString(4));
            indirizzo = (String) parametriAziendaJSON.get("basePath") + "/scrivania/attivita";
        } catch (Throwable ex) {
            log.error("ERRRORE: Impossibile recuperare l'url di login", ex);
        }
        return indirizzo;
    }

    private String preparaBodyMessaggio(List<Attivita> attivitaSuScrivania) throws Exception {
        String url = recuperaIndirizzoLogin();
        String tabellaFormattataAttività = preparaListaAttivitaDaMostrare(attivitaSuScrivania);
        String body = "Buongiorno, hai delle nuove attività sulla Scrivania di Babel.\n"
                + "Clicca qui per accedere alla Scrivania e consultarle\n\n"
                + url + "\n\n\n"
                + "Ecco la lista delle prime " + attivitaSuScrivania.size() + " attività:\n\n\n";
        body += tabellaFormattataAttività;
        body += "\n\n\nBuon lavoro! Team Babel\n\n\n";
        System.out.println("QUESTA E' LA LISTA INVIATA");
        System.out.println(tabellaFormattataAttività);
        return body;
    }

    private List<Attivita> getAttivitaSuScrivania(Integer idPersona) {
        log.info("Chiamo attivitaRepository.getLatestFiftyAttivitaInScrivaniaByIdPersona(" + idPersona + ") ....");
        return attivitaRepository.getLatestFiftyAttivitaInScrivaniaByIdPersona(idPersona);
    }

    private boolean hasUserEmail(Utente utente) {
        String[] emails = utente.getEmails();
        return emails.length != 0 && emails[0] != null && !emails[0].trim().equalsIgnoreCase("");
    }

    private boolean possoProseguire(Persona persona, Utente utenteAziendale, List<Attivita> attivitaSuScrivania) {
        if (!persona.getAttiva()) {
            log.info("La persona " + persona.getDescrizione() + " non e' attiva: la salto");
            return false;
        }
        if (utenteAziendale.getAttivo()) {
            log.info("La persona " + persona.getDescrizione() + " non ha utente attivo in "
                    + getNomeAzienda(handler.getIdAzienda()) + ": la salto");
            return false;
        }

        if (!hasUserEmail(utenteAziendale)) {
            log.info("La persona " + persona.getDescrizione() + " ha l'utente in "
                    + getNomeAzienda(handler.getIdAzienda()) + " senza una mail: la salto");
            return false;
        }
        if (attivitaSuScrivania == null || attivitaSuScrivania.size() == 0) {
            log.info("La persona " + persona.getDescrizione() + " non ha attivita in scrivania: la salto");
            return false;
        }
        return true;
    }

    private boolean isUtenteValido(Utente utenteAziendale) {
        return utenteAziendale.getAttivo() && hasUserEmail(utenteAziendale);
    }

    private boolean isEnabledTestMail(String mailAddres) {
        boolean isEnabledTestEmail = false;
        log.info("Is enable test email?");
        for (int i = 0; i < enabledEmailsForTest.length; i++) {
            if (enabledEmailsForTest[i].equals(mailAddres)) {
                isEnabledTestEmail = true;
                break;
            }
        }
        log.info("Answer: " + isEnabledTestEmail);
        return isEnabledTestEmail;
    }

    private boolean isValidEmailAddres(String mailAddres) {
        return internautaUtils.isValidEmailAddress(mailAddres);
    }

    private void updateAndFlushParametroAziende() {
        log.info("updateAndFlushParametroAziende() ");
        JSONObject updatedParametroAziendeCampoValoreJSON = handler.setDatiUltimaEsecuzioneMestiere();
        ParametroAziende parametroAziende = handler.getParametroAziende();
        parametroAziende.setValore(updatedParametroAziendeCampoValoreJSON.toString(4));
        parametroAziende = parametroAziendeRepository.saveAndFlush(parametroAziende);
        log.info("Updated parametroAziende.valore:\n"
                + parametroAziende.getValore());
    }

    private void preparaMessaggioAndInvia(Utente utenteAziendale, List<Attivita> attivitaSuScrivania) throws Exception {
        // preparo il messaggio
        String body = preparaBodyMessaggio(attivitaSuScrivania);
        log.info("Preparo il messaggio da ");
        ArrayList<String> destinatari = new ArrayList<String>();
        destinatari.add(utenteAziendale.getEmails()[0]);
        log.info("Invio la mail...");
        Boolean sendMail = simpleMailSenderUtility.sendMail(handler.getIdAzienda(),
                getMailMittente(), "Nuove attività su Scrivania",
                destinatari, body,
                null, null, null, null);
        if (!sendMail) {
            log.error("ERRORE: la mail NON e' stata inviata!");
        } else {

            log.info("Mail INVIATA!");
        }
    }

    private void verificaAndInvia(Integer idPersona, Azienda azienda) throws Exception {
        // se non già fatte:
        if (!idPersoneAvvisate.contains(idPersona)) {
            log.info("Load persona...");
            Persona persona = personaRepository.findById(idPersona).get();
            log.info("Persona: " + persona.getDescrizione());

            if (!persona.getAttiva()) {
                log.info("La persona " + persona.getDescrizione() + " non e' attiva: la salto");
                return;
            }
            log.info("Cerco l'utente by Azienda, Persona...");
            Utente utenteAziendale = utenteRepository.findByIdAziendaAndIdPersona(azienda, persona);

            if (utenteAziendale == null || !utenteAziendale.getAttivo()) {
                log.info("La persona " + persona.getDescrizione() + " non ha utente attivo in "
                        + getNomeAzienda(handler.getIdAzienda()) + ": la salto");
                return;
            }

            if (!hasUserEmail(utenteAziendale)) {
                log.info("La persona " + persona.getDescrizione() + " ha l'utente in "
                        + getNomeAzienda(handler.getIdAzienda()) + " senza una mail: la salto");
                return;
            }

            //      cerca attività su scrivania
            log.info("Cerco le attivita'...");
            List<Attivita> attivitaSuScrivania = getAttivitaSuScrivania(idPersona);
            //      se count(attività) > 0
            log.info("Verifico se posso proseguire");
            if (attivitaSuScrivania != null && attivitaSuScrivania.size() > 0) {
                if (!isValidEmailAddres(utenteAziendale.getEmails()[0])) {
                    log.info("ATTENZIONE: la mail dell'utente '"
                            + utenteAziendale.getEmails()[0]
                            + "' non supera criteri di validità, quindi non invio!");
                    return;
                }
//                      SE SIAMO IN !TEST || (SIAMO IN TEST && EMAIL E' IN ARRAY DI MAIL ABILITATE):
                if (!internautaMode.equals("test") || isEnabledTestMail(utenteAziendale.getEmails()[0])) {
                    preparaMessaggioAndInvia(utenteAziendale, attivitaSuScrivania);
                } else {
                    log.info("Siamo in test e la mail dell'utente non è tra quelle abilitate");
                }
                idPersoneAvvisate.add(idPersona);
            } else {

                log.info("La persona "
                        + persona.getDescrizione() + " non ha attivita' sulla scrivania: la salto ");
            }
        } else {
            log.info("Gia' avvisato, skippo");
        }
    }

    @Override
    public void run() {
        // preparo una collection di aziende per trovare subito la descrizione in seguito
        log.info("Run...");
        log.info("parametri: " + handler.getIdAzienda().toString() + " " + idPersoneAvvisate.size());
        loadAziende();
        log.info("Recupero l'azienda attuale da quelle appena caricate");
        Azienda azienda = getAziendaById(handler.getIdAzienda());
        // cerca le persone attive con un utente attivo nell'azienda
        log.info("Cerco le persone con  un utente in azienda " + handler.getIdAzienda());
        List<Integer> personeAttiveConUtentiAttiviSuAzienda
                = personaRepository.getPersoneAttiveInAziendaConAttivitaSuScrivaniaDaAvvisare(handler.getIdAzienda());
        log.info("Trovate " + personeAttiveConUtentiAttiviSuAzienda.size());
        // cicla le persone 
        for (Integer idPersona : personeAttiveConUtentiAttiviSuAzienda) {
            try {
                log.info("Persona " + idPersona);
                verificaAndInvia(idPersona, azienda);
            } catch (Throwable ex) {
                log.error("Errore in fase di esecutione", ex);
                log.error("Proseguo con la prossima persona");
            }
        }
        log.info("Ciclo finito su " + handler.getIdAzienda());
        log.info("Persone avvisate " + idPersoneAvvisate);
        log.info("Aggiorno il parametro su handler");
        updateAndFlushParametroAziende();
    }

}
