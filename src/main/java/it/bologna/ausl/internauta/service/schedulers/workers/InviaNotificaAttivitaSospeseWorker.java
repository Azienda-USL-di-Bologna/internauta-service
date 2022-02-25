/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers;

import org.apache.commons.lang.StringUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ParametroAziendeRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.handlers.ParametroAziendeInvioMailNotificaAttivitaHandler;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.internauta.service.utils.SimpleMailSenderUtility;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
    ImpostazioniApplicazioniRepository impostazioniApplicazioniRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    AttivitaRepository attivitaRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    ApplicazioneRepository applicazioneRepository;

    @Value("${internauta.mode}")
    private String internautaMode;

    @Value("${internauta.scheduled.invio-mail-notifica-attivita-sospese.enabled-emails-test}")
    private String[] enabledEmailsForTest;

    private String personeAvvisateString;

    private JSONObject personeAvvisateJSON = new JSONObject();

    private JSONArray personeAvvisateJArray = new JSONArray();

    private List<Integer> personeAvvisate = new ArrayList();

    private Map<Integer, String> listEmailToNotify = new HashMap<>();

    private static final Logger log = LoggerFactory
            .getLogger(InviaNotificaAttivitaSospeseWorker.class);

    private boolean isToday(Date date) {
        LocalDate thatLocalDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate TODAY = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return thatLocalDate.isEqual(TODAY);
    }

    public List<Integer> loadPersoneNotificate(Azienda azienda) {
        Integer[] arrayAziende = new Integer[]{azienda.getId()};
        ParametroAziende pA = parametroAziendeRepository.findOne(
                QParametroAziende.parametroAziende.nome.eq("personeNotificate")
        ).get();

        personeAvvisateString = pA.getValore();

        if (personeAvvisateString != null && !personeAvvisateString.replace("{", "").replace("}", "").isEmpty()) {

            personeAvvisateJSON = new JSONObject(personeAvvisateString);

            if (personeAvvisateJSON.has("persone")) {

                JSONArray jsonPersone = (JSONArray) personeAvvisateJSON.get("persone");

                for (int i = 0; i < jsonPersone.length(); i++) {

                    JSONObject persona = (JSONObject) jsonPersone.get(i);

                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                    String dateString = (String) persona.get("version");

                    if (dateString != null && !dateString.isEmpty()) {

                        try {
                            Date lastUpdate = formatter.parse(dateString);

                            if (isToday(lastUpdate)) {

                                log.info("Persona " + persona.getInt("id") + " già avvisata");

                                if (!personeAvvisate.contains(persona.getInt("id"))) {
                                    personeAvvisate.add(persona.getInt("id"));
                                }

                            }
                        } catch (java.text.ParseException ex) {
                            log.error("La persona " + persona.get("id") + " non ha la version: la avviso");
                        }

                    }

                }
            }

        }
        log.info("Numero di persone avvisate pre run: " + personeAvvisate.size());
        log.info("personeAvvisateJArray size: " + personeAvvisateJArray.length());
        return personeAvvisate;
    }

    private static List<Integer> idPersoneAvvisate = new ArrayList<>();
    private List<Azienda> listaAziende;
    private List<Applicazione> listaApplicazioni;
    private ParametroAziendeInvioMailNotificaAttivitaHandler handler;

    public void setParameter(ParametroAziendeInvioMailNotificaAttivitaHandler handler) {
//        //this.idPersoneAvvisate = idPersoneAvvisate;
//        //this.idAzienda = idAzienda;
        this.handler = handler;
    }

    private void loadAziende() {
        log.info("Load aziende");
        listaAziende = aziendaRepository.findAll();
    }

    private void loadApplicazioni() {
        log.info("Load applicazioni");
        listaApplicazioni = applicazioneRepository.findAll();
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

    private void loadEmailToNotify(Integer idAzienda) {

        List<ImpostazioniApplicazioni> listEmail = impostazioniApplicazioniRepository.getEmailToNotify();

        JSONParser parser = new JSONParser();

        log.info("Load emailToNotify...");

        for (ImpostazioniApplicazioni iA : listEmail) {
            Map<Integer, String> tempMap = new HashMap();
            org.json.simple.JSONObject json = new org.json.simple.JSONObject();
            try {

                json = (org.json.simple.JSONObject) parser.parse(iA.getImpostazioniVisualizzazione());

            } catch (ParseException ex) {
                log.error("Errore parsing impostazioni_apllicazione");
            }

            String mail = (String) json.get("scrivania.emailToNotify");

            listEmailToNotify.put(iA.getIdPersona().getId(), mail);

        }

        log.info("Numero persone con parametro mailToNotify: " + listEmailToNotify.size());
    }

    private String getNomeAzienda(Integer idAzienda) {
        Azienda azienda = getAziendaById(idAzienda);
        return azienda.getNome();
    }

    private String sistemaOggettoAttivitaPerMail(Attivita attivita) {
        String oggettoDaTornare = "";
//        if (attivita.getIdApplicazione().getId().equals("gedi")) {
//            oggettoDaTornare = attivita.getOggetto();
//        } else {
//            int lastCharIndex = attivita.getOggetto().indexOf(":");
//            oggettoDaTornare = attivita.getOggetto().substring(0, lastCharIndex);
//        }
        oggettoDaTornare = attivita.getOggetto();
        return oggettoDaTornare;
    }

    private String preparaListaAttivitaDaMostrareHTML(List<Attivita> listaAttivita) {
        final String format = "<tr>%-18s %-18s %-35s  %-35s %s</tr>";
        // Azienda, Data, Tipo, Provenienza, Oggetto attivita
        String tabella = "<table>";
        tabella += String.format(format, "<th>AZIENDA</th>", "<th>DATA</th>", "<th>TIPO</th>", "<th>PROVENIENZA</th>", "<th>OGGETTO</th>");
        for (Attivita attivita : listaAttivita) {
            String provenienza = attivita.getProvenienza() != null && attivita.getProvenienza().length() > 40
                    ? attivita.getProvenienza().substring(0, 37) + "..."
                    : attivita.getProvenienza();
            String oggetto = sistemaOggettoAttivitaPerMail(attivita);

            tabella += String.format(format, "<th>" + getNomeAzienda(attivita.getIdAzienda().getId()) + "</th>",
                    "<th>" + attivita.getData().format(DateTimeFormatter.ofPattern("dd/MM/uuuu")).toString() + "</th>",
                    "<th>" + attivita.getDescrizione() + "</th>",
                    "<th>" + provenienza + "</th>",
                    "<th>" + oggetto + "</th>");
        }
        tabella += "</table>";
        return tabella;
    }

    private String preparaListaAttivitaDaMostrareWithStringUtils(List<Attivita> listaAttivita) {
        // Azienda, Data, Tipo, Provenienza, Oggetto attivita
        String newLine = "";
        String tabella = "";
        newLine = StringUtils.center("AZIENDA", 15, " ")
                + StringUtils.center("DATA", 14, " ")
                + StringUtils.center("TIPO", 28, " ")
                + StringUtils.center("PROVENIENZA", 30, " ")
                + StringUtils.center("OGGETTO", 40, " ");
        tabella += newLine;
        tabella += "\n";
        for (Attivita attivita : listaAttivita) {
            tabella += "\n";
            String azienda = getNomeAzienda(attivita.getIdAzienda().getId());
            String data = attivita.getData().format(DateTimeFormatter.ofPattern("dd/MM/uuuu")).toString();
            String tipo = attivita.getDescrizione();
            String provenienza = attivita.getProvenienza();
            String oggetto = sistemaOggettoAttivitaPerMail(attivita);

            newLine = StringUtils.center(azienda, 15, " ")
                    + StringUtils.center(data, 14, " ")
                    + StringUtils.center(tipo, 28, " ")
                    + StringUtils.center(StringUtils.abbreviate(provenienza, 28), 30, " ")
                    + StringUtils.center(StringUtils.abbreviate(oggetto, 38), 40, " ");

            tabella += newLine;
        }
        tabella += "\n";
        return tabella;
    }

    private String preparaListaAttivitaDaMostrare(List<Attivita> listaAttivita) {
        final String format = "%-18s %-18s %-35s  %-35s %s\n";
        //StringUtils.
        // Azienda, Data, Tipo, Provenienza, Oggetto attivita
        String tabella = String.format(format, "AZIENDA", "DATA", "TIPO", "PROVENIENZA", "OGGETTO");
        tabella += "\n";
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
                    oggetto);
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
            log.error("ERRORE: Impossibile recuperare il parametro del mittente", ex);
        }

        return mittente;
    }

    private String recuperaIndirizzoLogin(Persona persona) {
        String indirizzo = "";
        Integer idAziendaUrl = persona.getIdAziendaDefault() != null ? persona.getIdAziendaDefault().getId() : handler.getIdAzienda();
        try {
            Azienda azienda = getAziendaById(idAziendaUrl);
            JSONObject parametriAziendaJSON = new JSONObject(azienda.getParametri());
            //log.info("parametriAziendaJSON: " + parametriAziendaJSON.toString(4));
            indirizzo = (String) parametriAziendaJSON.get("basePath") + "/scrivania/attivita";
        } catch (Throwable ex) {
            log.error("ERRRORE: Impossibile recuperare l'url di login", ex);
        }
        return indirizzo;
    }

    private String preparaBodyMessaggioHTML(Persona persona, List<Attivita> attivitaSuScrivania) throws Exception {
        String url = recuperaIndirizzoLogin(persona);
        String tabellaFormattataAttività = preparaListaAttivitaDaMostrareHTML(attivitaSuScrivania);
        String body = "<p>Buongiorno, hai delle nuove attività sulla Scrivania di Babel.</p><br>"
                + "<p>Clicca <a href=\"" + url + "\">qui</a> per accedere alla Scrivania e consultarle<br><br><br>"
                + "<p>Ecco la lista delle ultime " + attivitaSuScrivania.size() + " attività:</p><br><br><br>";
        body += tabellaFormattataAttività;
        body += "<br><br>Buon lavoro! Team Babel<br><br><br><br>";
        System.out.println("QUESTA E' LA LISTA INVIATA");
        System.out.println(tabellaFormattataAttività);
        return body;
    }

    private String preparaBodyMessaggio(Persona persona, List<Attivita> attivitaSuScrivania) throws Exception {
        String url = recuperaIndirizzoLogin(persona);
        String tabellaFormattataAttività = preparaListaAttivitaDaMostrareWithStringUtils(attivitaSuScrivania);
        String body = "Buongiorno, hai delle nuove attività sulla Scrivania di Babel.\n"
                + "Clicca qui per accedere alla Scrivania e consultarle\n\n"
                + url + "\n\n\n"
                + "Ecco la lista delle ultime " + attivitaSuScrivania.size() + " attività:\n\n\n";
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

    private int getJSONArrayElementIndex(JSONArray array, JSONObject object, String chiaveDiVerifica) {
        int index = -1;
        for (int i = 0; i < array.length(); i++) {
            JSONObject element = (JSONObject) array.get(i);
            if (element.get(chiaveDiVerifica).equals(object.get(chiaveDiVerifica))) {
                return i;
            }
        }
        return index;
    }

    private boolean containsJSONArrayThisObject(JSONArray array, JSONObject object, String chiaveDiVerifica) {
        return getJSONArrayElementIndex(array, object, chiaveDiVerifica) > -1;
    }

    private void updatePersoneAvvisate(JSONObject personaAvvisata) {

        //Integer[] arrayAzienda = new Integer[]{a.getId()};
        ParametroAziende pA = parametroAziendeRepository.findOne(
                QParametroAziende.parametroAziende.nome.eq("personeNotificate")
        ).get();

        //log.info("Persone notificate pre aggiornamento: " + personeAvvisateJSON.toString());
        JSONObject parametroAziendaValorePersoneNotificate = new JSONObject(pA.getValore());
        JSONArray personeNotificateJsnArray = (JSONArray) parametroAziendaValorePersoneNotificate.get("persone");
        if (!containsJSONArrayThisObject(personeNotificateJsnArray, personaAvvisata, "id")) {
            personeNotificateJsnArray.put(personaAvvisata);
        } else {
            int index = getJSONArrayElementIndex(personeNotificateJsnArray, personaAvvisata, "id");
            personeNotificateJsnArray.put(index, personaAvvisata);
        }

        personeAvvisateJSON.put("persone", personeNotificateJsnArray);
        //log.info("Persone notificate post aggiornamento: " + personeAvvisateJSON.toString());
//        boolean found = false;

//        for (int i : pA.getIdAziende()) {
//            if (a.getId() == i) {
//                found = true;
//                break;
//            }
//        }
//        if (found == false) {
//            Integer[] temp = pA.getIdAziende();
//            Integer[] temp2 = new Integer[temp.length + 1];
//            for (int i = 0; i < temp.length; i++) {
//                temp2[i] = temp[i];
//            }
//            temp2[temp.length] = a.getId();
//            pA.setIdAziende(temp2);
//        }
        pA.setValore(personeAvvisateJSON.toString());
        parametroAziendeRepository.saveAndFlush(pA);

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

    private void preparaMessaggioAndInvia(Persona persona, Utente utenteAziendale, List<Attivita> attivitaSuScrivania, String mail) throws Exception, Throwable {
        // preparo il messaggio
        String body = preparaBodyMessaggio(persona, attivitaSuScrivania);
        System.out.println(body);
        log.info("Preparo il messaggio da ");
        ArrayList<String> destinatari = new ArrayList<String>();
        destinatari.add(mail);
        log.info("Invio la mail...");
        Boolean sendMail = simpleMailSenderUtility.sendMail(handler.getIdAzienda(),
                getMailMittente(), "Nuove attività su Scrivania",
                destinatari, body,
                null, null, null, null);
        if (!sendMail) {
            log.error("ERRORE: la mail NON e' stata inviata!");
            throw new Throwable("Invio mail fallito");
        } else {
            log.info("Mail INVIATA!");
        }

    }

    private void verificaAndInvia(Integer idPersona, Azienda azienda) throws Exception {
        // se non già fatte:
        if (!personeAvvisate.contains(idPersona)) {

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

            //      cerca attività su scrivania
            log.info("Cerco le attivita'...");
            List<Attivita> attivitaSuScrivania = getAttivitaSuScrivania(idPersona);
            //      se count(attività) > 0
            log.info("Verifico se posso proseguire");
            if (attivitaSuScrivania != null && attivitaSuScrivania.size() > 0) {
                try {

                    String mail = listEmailToNotify.get(persona.getId());

                    try {
                        preparaMessaggioAndInvia(persona, utenteAziendale, attivitaSuScrivania, mail);

                    } catch (Throwable t) {
                        log.error("Errore: non sono riuscito ad inviare la mail a " + mail + " per la persona " + persona.getDescrizione(), t);
                    }

                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                    Date date = new Date();

                    String personaAvvisata = "{ \"id\": " + persona.getId() + ", \"version\":\"" + formatter.format(date) + "\" }";

                    JSONObject personaAvvisataJSON = new JSONObject(personaAvvisata);

                    if (!personeAvvisate.contains(persona.getId())) {
                        personeAvvisate.add(persona.getId());
                    }

                    personeAvvisateJArray.put(personaAvvisataJSON);

                    log.info("Avvisata persona:" + personaAvvisata + " inserita in personaAvvisataJSON");

                    log.info("personeAvvisateJArray size: " + personeAvvisateJArray.length());

                    updatePersoneAvvisate(personaAvvisataJSON);

                } catch (Exception e) {

                    log.error("Errore invio mail");

                }

            } else {
                log.info("La persona "
                        + persona.getDescrizione() + " non ha attivita' sulla scrivania: la salto ");
            }

        } else {
            log.info("Persona " + idPersona + " già avvisata, la salto");
        }

    }

    @Override
    public void run() {
        // preparo una collection di aziende per trovare subito la descrizione in seguito
        log.info("Run...");
        personeAvvisate = new ArrayList<Integer>();
        log.info("parametri: " + handler.getIdAzienda().toString());
        loadAziende();
        loadApplicazioni();

        log.info("Recupero l'azienda attuale da quelle appena caricate");
        Azienda azienda = getAziendaById(handler.getIdAzienda());
        loadPersoneNotificate(azienda);
        loadEmailToNotify(azienda.getId());
        // cerca le persone attive con un utente attivo nell'azienda
        log.info("Cerco le persone con  un utente in azienda " + handler.getIdAzienda());
        List<Integer> personeAttiveConUtentiAttiviSuAzienda = new ArrayList<>();
        Set<Integer> keySet = listEmailToNotify.keySet();
        Iterator<Integer> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            personeAttiveConUtentiAttiviSuAzienda.add(iterator.next());
        }
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
        log.info("Aggiorno il parametro su handler");
        updateAndFlushParametroAziende();
    }

}
