/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    UtenteRepository utenteRepository;

    @Autowired
    AttivitaRepository attivitaRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    private static final Logger log = LoggerFactory
            .getLogger(InviaNotificaAttivitaSospeseWorker.class);

    private List<Integer> idPersoneAvvisate = new ArrayList<>();
    private Integer idAzienda;
    List<Azienda> listaAziende;

    public void setParameter(List<Integer> idPersoneAvvisate, Integer idAzienda) {
        this.idPersoneAvvisate = idPersoneAvvisate;
        this.idAzienda = idAzienda;
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

    private String preparaListaAttivitaDaMostrare(List<Attivita> listaAttivita) {
        final String format = "%-15s %-15s %-30s  %-40s %s\n";
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

    private void preparaMessaggio(Persona persona, Utente utenteAziendale, List<Attivita> attivitaSuScrivania) {
        String preparaListaAttivitaDaMostrare = preparaListaAttivitaDaMostrare(attivitaSuScrivania);
        System.out.println("QUESTA E' LA LISTA INVIATA");
        System.out.println(preparaListaAttivitaDaMostrare);
    }

    private List<Attivita> getAttivitaSuScrivania(Integer idPersona) {
        log.info("Chiamo attivitaRepository.getLatestFiftyAttivitaInScrivaniaByIdPersona(" + idPersona + ") ....");
        return attivitaRepository.getLatestFiftyAttivitaInScrivaniaByIdPersona(idPersona);
    }

    private boolean hasUserEmail(Utente utente) {
        String[] emails = utente.getEmails();
        return emails.length != 0 && emails[0] != null && emails[0].trim().equalsIgnoreCase("");
    }

    private boolean possoProseguire(Persona persona, Utente utenteAziendale, List<Attivita> attivitaSuScrivania) {
        if (!persona.getAttiva()) {
            log.info("La persona " + persona.getDescrizione() + " non e' attiva: la salto");
            return false;
        }
        if (utenteAziendale.getAttivo()) {
            log.info("La persona " + persona.getDescrizione() + " non ha utente attivo in "
                    + getNomeAzienda(idAzienda) + ": la salto");
            return false;
        }

        if (!hasUserEmail(utenteAziendale)) {
            log.info("La persona " + persona.getDescrizione() + " ha l'utente in "
                    + getNomeAzienda(idAzienda) + " senza una mail: la salto");
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

    @Override
    public void run() {
        // preparo una collection di aziende per trovare subito la descrizione in seguito
        log.info("Run...");
        log.info("parametri: " + idAzienda.toString() + " " + idPersoneAvvisate.toString());
        loadAziende();
        log.info("Recupero l'azienda attuale da quelle appena caricate");
        Azienda azienda = getAziendaById(idAzienda);
        // cerca le persone attive con un utente attivo nell'azienda
        log.info("Cerco le persone con  un utente in azienda " + idAzienda);
        List<Integer> personeAttiveConUtentiAttiviSuAzienda
                = personaRepository.getPersoneAttiveInAziendaConAttivitaSuScrivaniaDaAvvisare(idAzienda);
        log.info("Trovate " + personeAttiveConUtentiAttiviSuAzienda.size());
        // cicla le persone 
        for (Integer idPersona : personeAttiveConUtentiAttiviSuAzienda) {
            try {
                log.info("Persona " + idPersona);
                // se non già fatte:
                if (!idPersoneAvvisate.contains(idPersona)) {
                    log.info("Load persona...");
                    Persona persona = personaRepository.findById(idPersona).get();
                    log.info("Persona: " + persona.getDescrizione());

                    if (!persona.getAttiva()) {
                        log.info("La persona " + persona.getDescrizione() + " non e' attiva: la salto");
                        continue;
                    }
                    log.info("Cerco l'utente by Azienda, Persona...");
                    Utente utenteAziendale = utenteRepository.findByIdAziendaAndIdPersona(azienda, persona);

                    if (utenteAziendale == null || !utenteAziendale.getAttivo()) {
                        log.info("La persona " + persona.getDescrizione() + " non ha utente attivo in "
                                + getNomeAzienda(idAzienda) + ": la salto");
                        continue;
                    }

                    if (!hasUserEmail(utenteAziendale)) {
                        log.info("La persona " + persona.getDescrizione() + " ha l'utente in "
                                + getNomeAzienda(idAzienda) + " senza una mail: la salto");
                        continue;
                    }

                    //      cerca attività su scrivania
                    log.info("Cerco le attivita'...");
                    List<Attivita> attivitaSuScrivania = getAttivitaSuScrivania(idPersona);
                    //      se count(attività) > 0
                    log.info("Verifico se posso proseguire");
                    if (attivitaSuScrivania != null && attivitaSuScrivania.size() > 0) {
                        // preparo il messaggio
                        preparaMessaggio(persona, utenteAziendale, attivitaSuScrivania);
                        // salvo in messaggio in outbox

                        log.info("Aggiunta mail per la persona: la aggiungo a quelle avvisate");
                        idPersoneAvvisate.add(idPersona);
                    } else {

                        log.info("La persona "
                                + persona.getDescrizione() + " non ha attivita' sulla scrivania: la salto ");
                    }
                } else {
                    log.info("Gia' avvisato, skippo");
                }
            } catch (Throwable ex) {
                log.error("Errore in fase di esecutione", ex);
                log.error("Proseguo con la prossima persona");
            }
        }
        log.info("Ciclo finito su " + idAzienda);
        log.info("Persone avvisate " + idPersoneAvvisate);
    }

}
