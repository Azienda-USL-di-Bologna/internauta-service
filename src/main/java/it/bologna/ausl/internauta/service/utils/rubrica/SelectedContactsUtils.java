/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.rubrica;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.controllers.rubrica.SelectedContact;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class SelectedContactsUtils {

    DettaglioContatto.TipoDettaglio TIPO_UTENTE_STRUTTURA = DettaglioContatto.TipoDettaglio.UTENTE_STRUTTURA;
    DettaglioContatto.TipoDettaglio TIPO_STRUTTURA = DettaglioContatto.TipoDettaglio.STRUTTURA;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    private JSONObject dammiOggettoDestinatarioGruppoSenzaContattiInterniDiAltraAzienda(
            JSONObject oggettoDestinatarioGruppo,
            Azienda azienda) {
        JSONObject oggettoDestinatarioDaTornare = null;
        JSONObject contact = oggettoDestinatarioGruppo.getJSONObject("contact");

        JSONArray contattiDelGruppoList = (JSONArray) contact
                .get("contattiDelGruppoList");
        if (contattiDelGruppoList != null) {
            contattiDelGruppoList = pulisciListDaInterniDiAltraAzienda(contattiDelGruppoList, azienda);
            contact.put("contattiDelGruppoList", contattiDelGruppoList);
        }
        JSONArray contattiDelGruppoListTransient = contact
                .getJSONArray("contattiDelGruppoListTransient");
        if (contattiDelGruppoListTransient != null) {
            contattiDelGruppoListTransient = pulisciListTransientDaInterniDiAltraAzienda(contattiDelGruppoListTransient, azienda);
            contact.put("contattiDelGruppoListTransient", contattiDelGruppoListTransient);
        }

        if ((contattiDelGruppoListTransient != null || contattiDelGruppoList != null)
                && contattiDelGruppoList.length() > 0 || contattiDelGruppoListTransient.length() > 0) {
            System.out.println("Il gruppo ha conservato almeno un destinatario");
            oggettoDestinatarioGruppo.put("contact", contact);
            oggettoDestinatarioDaTornare = oggettoDestinatarioGruppo;
        }
        return oggettoDestinatarioDaTornare;
    }

    public JSONArray togliDaJSONArrayDestinatariContattiInterniDiAltreAziende(
            JSONArray jarrayContattiDestinatari,
            Azienda azienda) {
        System.out.println("Dentro togliDaJSONArrayDestinatariContattiInterniDiAltreAziende");
        JSONArray jsonArrayToReturn = new JSONArray();
        for (int i = 0; i < jarrayContattiDestinatari.length(); i++) {
            JSONObject elementoContattoDestinatario
                    = jarrayContattiDestinatari.getJSONObject(i);
            JSONObject contact = (JSONObject) elementoContattoDestinatario.get("contact");
            if (contact.get("categoria").equals("GRUPPO") && contact.has("contattiDelGruppoList")) {
                elementoContattoDestinatario = dammiOggettoDestinatarioGruppoSenzaContattiInterniDiAltraAzienda(elementoContattoDestinatario, azienda);
                if (elementoContattoDestinatario != null) {
                    jsonArrayToReturn.put(elementoContattoDestinatario);
                }
            } else {
                jsonArrayToReturn.put(elementoContattoDestinatario);
            }

        }

        return jsonArrayToReturn;
    }

    private JSONArray pulisciListTransientDaInterniDiAltraAzienda(
            JSONArray contattiDelGruppoListTransient,
            Azienda azienda) {
        System.out.println("Dentro togliDaContattiDelGruppoListTransientInterniDiAltraAzienda");
        JSONArray nuovoJsonArrayDaTornare = new JSONArray();

        for (int i = 0; i < contattiDelGruppoListTransient.length(); i++) {
            JSONObject elementoContattoTransient = (JSONObject) contattiDelGruppoListTransient.get(i);
            System.out.println("Processo questo elemento\n" + elementoContattoTransient.toString());

            JSONObject contact = elementoContattoTransient.getJSONObject("contact");
            if (contact.get("categoria").equals("PERSONA")) {
                JSONObject address = elementoContattoTransient.getJSONObject("address");
                JSONObject utenteStrutturaObject = address.getJSONObject("utenteStruttura");
                JSONObject struttura = (JSONObject) utenteStrutturaObject.get("idStruttura");
                Integer idStruttura = struttura.getInt("id");
                boolean idStrutturaDiAltraAzienda = isIdStrutturaDiAltraAzienda(idStruttura, azienda);
                if (!idStrutturaDiAltraAzienda) {
                    nuovoJsonArrayDaTornare.put(elementoContattoTransient);
                }

            } else if (contact.get("categoria").equals("STRUTTURA")) {
                JSONObject strutturaObject = contact.getJSONObject("idStruttura");
                Integer idStruttura = strutturaObject.getInt("id");
                boolean idStrutturaDiAltraAzienda = isIdStrutturaDiAltraAzienda(idStruttura, azienda);
                if (!idStrutturaDiAltraAzienda) {
                    nuovoJsonArrayDaTornare.put(elementoContattoTransient);

                }
            } else {
                nuovoJsonArrayDaTornare.put(elementoContattoTransient);
            }
        }

        return nuovoJsonArrayDaTornare;
    }

    private JSONArray pulisciListDaInterniDiAltraAzienda(
            JSONArray contattiDelGruppoList,
            Azienda azienda) {
        JSONArray nuovoContattiDelGruppoListDaTornare = new JSONArray();
        for (int i = 0; i < contattiDelGruppoList.length(); i++) {
            JSONObject elementoContatto = (JSONObject) contattiDelGruppoList.get(i);
            System.out.println("Processo questo elemento\n" + elementoContatto.toString());

            JSONObject idContattoObject = elementoContatto
                    .getJSONObject("idContatto");
            if (idContattoObject.get("categoria").equals("STRUTTURA")
                    || idContattoObject.get("categoria").equals("PERSONA")) {
                boolean isInternoDaAltraAzienda = isDaAltraAzienda(elementoContatto, azienda);
                if (!isInternoDaAltraAzienda) {
                    nuovoContattiDelGruppoListDaTornare.put(elementoContatto);

                } else {
                    System.out.println("Questo non va messo:\n" + elementoContatto.toString());
                }
            } else {
                nuovoContattiDelGruppoListDaTornare.put(elementoContatto);
            }
        }

        return nuovoContattiDelGruppoListDaTornare;
    }

    private boolean isDaAltraAzienda(JSONObject elementoContatto,
            Azienda azienda) {
        boolean isDaAltraAzienda = false;
        JSONObject idContattoObject = elementoContatto.getJSONObject("idContatto");
        if (idContattoObject.get("categoria").equals("STRUTTURA")) {
            JSONObject idStrutturaJson = idContattoObject.getJSONObject("idStruttura");
            Integer idStruttura = idStrutturaJson.getInt("id");
            isDaAltraAzienda = isIdStrutturaDiAltraAzienda(idStruttura, azienda);

        } else if (idContattoObject.get("categoria").equals("PERSONA")) {
            JSONObject idDettaglioContattoObject = elementoContatto.getJSONObject("idDettaglioContatto");
            JSONObject utenteStruttura = (JSONObject) idDettaglioContattoObject.get("utenteStruttura");
            JSONObject idStrutturaObject = utenteStruttura.getJSONObject("idStruttura");
            Integer idStruttura = idStrutturaObject.getInt("id");
            isDaAltraAzienda = isIdStrutturaDiAltraAzienda(idStruttura, azienda);

        } else {
            System.out.println("Qua non bisogna entrare: "
                    + "spero di non vederlo mai stampato...");
        }
        return isDaAltraAzienda;
    }

    public boolean isIdStrutturaDiAltraAzienda(Integer idStruttura, Azienda azienda) {
        boolean isStrutturaDiAltraAzienda = false;
        Struttura struttura = strutturaRepository.getOne(idStruttura);
        Integer idAziendaDellaStruttura = struttura.getIdAzienda().getId();
        if (!idAziendaDellaStruttura.equals(azienda.getId())) {
            isStrutturaDiAltraAzienda = true;
        }
        return isStrutturaDiAltraAzienda;
    }

}
