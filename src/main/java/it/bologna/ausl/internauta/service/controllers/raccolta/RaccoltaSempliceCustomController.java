/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.controllers.raccolta;

import it.bologna.ausl.internauta.service.argo.raccolta.CoinvoltiRaccolte;
import it.bologna.ausl.internauta.service.argo.raccolta.Coinvolto;
import it.bologna.ausl.internauta.service.argo.raccolta.DocumentoBabel;
import it.bologna.ausl.internauta.service.argo.raccolta.Fascicolo;
import it.bologna.ausl.internauta.service.argo.raccolta.Raccolta;
import it.bologna.ausl.internauta.service.argo.raccolta.RaccoltaManager;
import it.bologna.ausl.internauta.service.argo.raccolta.Sottodocumento;
import it.bologna.ausl.internauta.service.argo.raccolta.Storico;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.rubrica.maven.client.RestClientException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import ucar.nc2.units.DateFromString;

/**
 *
 * @author Matteo Next
 */
@RestController
@RequestMapping(value = "${scrivania.mapping.url.root}")
public class RaccoltaSempliceCustomController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaccoltaSempliceCustomController.class);

    @Autowired
    private PostgresConnectionManager postgresConnectionManager;

    @RequestMapping(value = {"getRaccoltaSemplice"}, method = RequestMethod.GET)
    public List<Raccolta> getRaccoltaSemplice(@RequestParam("codiceAzienda") String codiceAzienda,
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            HttpServletRequest request) throws Http500ResponseException, Http404ResponseException, RestClientException {

        // Prendo la connessione dal connection manager
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryGetRaccoltaSemplice());

        List<Raccolta> datiRaccolta;
        List<Raccolta> returnRaccolta = new ArrayList<Raccolta>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try ( Connection conn = (Connection) dbConnection.open()) {
            Query queryWithParams = conn.createQuery(RaccoltaManager.queryRaccoltaSemplice())
                    .addParameter("from", dateFormat.parse(from))
                    .addParameter("to", dateFormat.parse(to));
            LOGGER.info("esecuzione query getRaccoltaSemplice: " + queryWithParams.toString());
            datiRaccolta = (List<Raccolta>) queryWithParams.executeAndFetch(Raccolta.class);
            for (Raccolta r : datiRaccolta) {
                dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryCodiceBabel());
                Query queryCodice = conn.createQuery(RaccoltaManager.queryCodiceBabel(r.getIdGddoc()));
                List<DocumentoBabel> doc = (List<DocumentoBabel>) queryCodice.executeAndFetch(DocumentoBabel.class);
                if ((doc == null || doc.isEmpty()) || doc.get(0).getNumeroRegistro() == null || doc.get(0).getCodiceRegistro() == null
                        || doc.get(0).getAnnoRegistro() == null || doc.get(0).getNumeroRegistro().isEmpty()
                        || doc.get(0).getCodiceRegistro().isEmpty()) {
                    r.setDocumentoBabel("Non associato");
                } else {
                    r.setDocumentoBabel(doc.get(0).getCodiceBabel());
                }
                dbConnection.setDefaultColumnMappings(RaccoltaManager.mapNumerazioneGerarchica());
                Query queryFascicolo = conn.createQuery(RaccoltaManager.queryNumerazioneGerarchica(r.getIdGddoc()));
                Query queryFascicoloAssociato = conn.createQuery(RaccoltaManager.queryNumerazioneGerarchica(r.getIdGddocAssociato()));
                List<Fascicolo> fascicoli = (List<Fascicolo>) queryFascicolo.executeAndFetch(Fascicolo.class);
                List<Fascicolo> fascicoliAssociati = (List<Fascicolo>) queryFascicoloAssociato.executeAndFetch(Fascicolo.class);
                fascicoli.addAll(fascicoliAssociati);
                List<Fascicolo> fascicoliCorretti = fascicoli.stream().distinct().collect(Collectors.toList());
                String numerazioneGerarchica = "";
                for (Fascicolo f : fascicoliCorretti) {
                    numerazioneGerarchica = numerazioneGerarchica + f.getNumerazioneGerarchica() + " ";
                }
                r.setFascicoli(numerazioneGerarchica);
                dbConnection.setDefaultColumnMappings(RaccoltaManager.mapCoinvoltiRaccolta());
                LOGGER.info("Query raccolta coinvolti: " + RaccoltaManager.queryCoinvoltiRaccolta(r.getId().toString()));
                Query queryCoinvoltiRaccolta = conn.createQuery(RaccoltaManager.queryCoinvoltiRaccolta(r.getId().toString()));
                List<CoinvoltiRaccolte> coinvoltiRaccolti = (List<CoinvoltiRaccolte>) queryCoinvoltiRaccolta.executeAndFetch(CoinvoltiRaccolte.class);
                dbConnection.setDefaultColumnMappings(RaccoltaManager.mapCoinvolti());
                for (CoinvoltiRaccolte cr : coinvoltiRaccolti) {
                    LOGGER.info("Query coinvolti: " + RaccoltaManager.queryCoinvolti(cr.getIdCoinvolto().toString()));
                    Query queryCoinvolti = conn.createQuery(RaccoltaManager.queryCoinvolti(cr.getIdCoinvolto().toString()));
                    List<Coinvolto> coinvolts = (List<Coinvolto>) queryCoinvolti.executeAndFetch(Coinvolto.class);
                    for (Coinvolto c : coinvolts) {
                        r.addCoinvolto(c);
                    }
                }
                dbConnection.setDefaultColumnMappings(RaccoltaManager.mapSottoDocumenti());
                Query querySottodocumenti = conn.createQuery(RaccoltaManager.querySottoDocumenti(r.getIdGddoc()));
                Query querySottodocumentiAssociati = conn.createQuery(RaccoltaManager.querySottoDocumenti(r.getIdGddocAssociato()));
                List<Sottodocumento> documenti = (List<Sottodocumento>) querySottodocumenti.executeAndFetch(Sottodocumento.class);
                List<Sottodocumento> documentiAssociati = (List<Sottodocumento>) querySottodocumentiAssociati.executeAndFetch(Sottodocumento.class);
                documenti.addAll(documentiAssociati);
                Integer i = 1;
                for (Sottodocumento d : documenti) {
                    d.setNome(r.getDocumentoBabel() + "_Allegato" + i.toString());
                    i++;
                    r.addSottodocumento(d);
                }
                returnRaccolta.add(r);
            }
        } catch (Exception e) {
            LOGGER.error("errore nell'esecuzione della query getRaccoltaSemplice", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query getRaccoltaSemplice");
        }
        LOGGER.info("Tutto ok");
        LOGGER.info("Oggetto: " + returnRaccolta.get(0).getOggetto());
        LOGGER.info("Codice : " + returnRaccolta.get(0).getDocumentoBabel());
        LOGGER.info("Numerazione: " + returnRaccolta.get(0).getFascicoli());
        LOGGER.info("Nome sottodocumento: " + returnRaccolta.get(0).getSottodocumenti().get(0).getNome());
        return returnRaccolta;
    }

    public String getNomeUtente(String id) {

        return "444";
    }

    @RequestMapping(value = {"storico"}, method = RequestMethod.GET)
    public List<Storico> dettaglioStorico(@RequestParam(value = "id") String id_raccolta,
            @RequestParam(value = "azienda") String azienda,
            HttpServletRequest request) throws Http500ResponseException,
            Http404ResponseException, RestClientException {
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(azienda);
        JSONParser parser = new JSONParser();
        List<Storico> returnList = new ArrayList<Storico>();

        dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryStorico());
        try ( Connection conn = (Connection) dbConnection.open()) {
            Query queryWithParams = conn.createQuery(RaccoltaManager.queryGetStorico(id_raccolta));
            LOGGER.info("esecuzione query annullamento: " + queryWithParams.toString());
            String lista = (String) queryWithParams.executeAndFetchFirst(String.class);
            JSONObject jsonReq = (JSONObject) parser.parse(lista);
            JSONArray jArray = (JSONArray) jsonReq.get("storico");
            for (Object json : jArray) {
                if (json instanceof JSONObject) {
                    String utente = ((JSONObject) json).get("utente").toString();
                    String data = ((JSONObject) json).get("data").toString();
                    String motivo = ((JSONObject) json).get("motivazione").toString();
                    String stato = ((JSONObject) json).get("utente").toString();
                    Storico s = new Storico(utente, motivo, stato, data);
                    returnList.add(s);
                    LOGGER.info("Inserito annullamento del " + s.getData());
                }
            }

            return returnList;

        } catch (Exception e) {
            LOGGER.error("errore nell'esecuzione della query annullamenti", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query di storico degli annullamenti");
        }
    }

    @RequestMapping(value = {"annullamento"}, method = RequestMethod.POST)
    public String azioneAnnullamento(@RequestBody JSONObject req,
            HttpServletRequest request) throws Http500ResponseException, Http404ResponseException, RestClientException {

        String id_raccolta = req.get("id_raccolta").toString();
        String utente = req.get("utente").toString();
        String azione = req.get("azione").toString();
        String motivazione = req.get("motivazione").toString();
        String azienda = req.get("azienda").toString();
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(azienda);
        JSONParser parser = new JSONParser();
        Calendar cal = Calendar.getInstance();
        Date dateNew = cal.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String date = dateFormat.format(dateNew);

        dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryStorico());
        try ( Connection conn = (Connection) dbConnection.open()) {
            Query queryWithParams = conn.createQuery(RaccoltaManager.queryGetStorico(id_raccolta));
            LOGGER.info("esecuzione query annullamento: " + queryWithParams.toString());
            String lista = (String) queryWithParams.executeAndFetchFirst(String.class);
            JSONObject jsonReq = (JSONObject) parser.parse(lista);
            JSONArray storico = (JSONArray) jsonReq.get("storico");
            motivazione = "{\"motivazione\": \"" + motivazione + "\",\"utente\": \"" + utente + "\",\"stato\": \"" + azione + "\", \"data\": \"" + date + "\"}";
            JSONObject nuovaMotivazione = (JSONObject) parser.parse(motivazione);
            storico.add(nuovaMotivazione);
            String stringStorico = "{\"storico\": " + storico.toString() + "}";
            Query queryAggiornamento = conn.createQuery(RaccoltaManager.queryUpdateStorico(stringStorico, id_raccolta, azione));
            queryAggiornamento.executeUpdate();
            return "OK";
        } catch (Exception e) {
            LOGGER.error("errore nell'esecuzione della query annullamenti", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query di storico degli annullamenti");
        }
    }

}
