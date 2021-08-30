package it.bologna.ausl.internauta.service.controllers.raccolta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import it.bologna.ausl.documentgenerator.exceptions.Http400ResponseException;
import it.bologna.ausl.documentgenerator.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.documentgenerator.exceptions.Sql2oSelectException;
import it.bologna.ausl.documentgenerator.utils.AziendaParamsManager;
import it.bologna.ausl.documentgenerator.utils.GeneratorUtils.SupportedSignatureType;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.argo.raccolta.CoinvoltiRaccolte;
import it.bologna.ausl.internauta.service.argo.raccolta.Coinvolto;
import it.bologna.ausl.internauta.service.argo.raccolta.DocumentoBabel;
import it.bologna.ausl.internauta.service.argo.raccolta.Fascicolo;
import it.bologna.ausl.internauta.service.argo.raccolta.PersonaRS;
import it.bologna.ausl.internauta.service.argo.raccolta.Raccolta;
import it.bologna.ausl.internauta.service.argo.raccolta.RaccoltaManager;
import it.bologna.ausl.internauta.service.argo.raccolta.RaccoltaNew;
import it.bologna.ausl.internauta.service.argo.raccolta.SottoDocumentoGdDoc;
import it.bologna.ausl.internauta.service.argo.raccolta.Sottodocumento;
import it.bologna.ausl.internauta.service.argo.raccolta.Storico;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.rubrica.maven.client.RestClientException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.sql2o.data.Row;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.DettaglioContattoRepository;
import it.bologna.ausl.internauta.service.rubrica.utils.similarity.SqlSimilarityResults;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.Indirizzo;
import it.bologna.ausl.model.entities.rubrica.Telefono;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.util.StreamUtils;

/**
 *
 * @author Matteo Next
 */
@RestController
@RequestMapping(value = "${scrivania.mapping.url.root}")
public class RaccoltaSempliceCustomController {

    private static final Logger log = LoggerFactory.getLogger(RaccoltaSempliceCustomController.class);

    private List<Raccolta> datiDocumenti = new ArrayList<Raccolta>();

    @Autowired
    private PostgresConnectionManager postgresConnectionManager;

    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ContattoRepository contattoRepository;

    @Autowired
    DettaglioContattoRepository dettaglioRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    PersonaRepository personaRepository;

//    @Autowired
    @RequestMapping(value = {"getRaccoltaSemplice"}, method = RequestMethod.GET)
    public List<Raccolta> getRaccoltaSemplice(@RequestParam("codiceAzienda") String codiceAzienda,
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam("cf") String cf,
            @RequestParam("piva") String pIva,
            @RequestParam("limit") Integer limit,
            @RequestParam("offset") Integer offeset,
            HttpServletRequest request) throws Http500ResponseException, Http404ResponseException, RestClientException {

        // Prendo la connessione dal connection manager
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryGetRaccoltaSemplice());

        List<Raccolta> datiRaccolta;
        List<Raccolta> returnRaccolta = new ArrayList<Raccolta>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        boolean isCF = cf != null && !cf.trim().equals("null") && !cf.trim().equals("");
        boolean isPiva = pIva != null && !pIva.trim().equals("null") && !pIva.trim().equals("");

        if ((from.equals("null") || to.equals("null")) && !isCF && !isPiva) {
            return returnRaccolta;
        }

        Query queryWithParams = null;
        try ( Connection conn = (Connection) dbConnection.open()) {
            if (isCF || isPiva) {
                String cfCondition = String.format("%s ", (isCF ? "AND lower(cf)=lower('" + cf + "') " : "AND (1=1) "));
                String pivaCondition = String.format("%s ", (isPiva ? "AND lower(partitaiva)=lower('" + pIva + "') " : "AND (1=1) "));
                String fromStr = "AND (1=1) ";

                if (!from.equals("null")) {
                    String pattern = "yyyy-MM-dd";
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                    String date = simpleDateFormat.format(dateFormat.parse(from));
                    fromStr = String.format("AND r.create_time::date >= '%s' ", date);
                }

                queryWithParams = conn.createQuery(RaccoltaManager.queryRaccoltaSempliceFromCFPiva(cfCondition, pivaCondition, fromStr, limit, offeset))
                        .addParameter("to", dateFormat.parse(to));
            } else {
                queryWithParams = conn.createQuery(RaccoltaManager.queryRaccoltaSemplice(limit, offeset))
                        .addParameter("from", dateFormat.parse(from))
                        .addParameter("to", dateFormat.parse(to));
            }

            log.info("esecuzione query getRaccoltaSemplice: " + queryWithParams.toString());
            datiRaccolta = (List<Raccolta>) queryWithParams.executeAndFetch(Raccolta.class);
            if (!datiRaccolta.isEmpty()) {
                for (Raccolta r : datiRaccolta) {
                    dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryCodiceBabel());
                    Query queryCodice = conn.createQuery(RaccoltaManager.queryCodiceBabel(r.getIdGddocAssociato()));
                    List<DocumentoBabel> doc = (List<DocumentoBabel>) queryCodice.executeAndFetch(DocumentoBabel.class);
                    if ((doc.isEmpty())) {
                        r.setDocumentoBabel("Non associato");
                    }
                    if ((doc == null || doc.isEmpty()) || doc.get(0).getNumero() == null || doc.get(0).getCodiceRegistro() == null
                            || doc.get(0).getAnno() == null || doc.get(0).getNumero().isEmpty()
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
                    if (fascicoliCorretti.isEmpty()) {
                        numerazioneGerarchica = "Fascicolo annuale";
                    }
                    for (Fascicolo f : fascicoliCorretti) {
                        numerazioneGerarchica = numerazioneGerarchica + f.getNumerazioneGerarchica() + " ";
                    }
                    r.setFascicoli(numerazioneGerarchica);
                    dbConnection.setDefaultColumnMappings(RaccoltaManager.mapCoinvoltiRaccolta());
                    log.info("Query raccolta coinvolti: " + RaccoltaManager.queryCoinvoltiRaccolta(r.getId().toString()));
                    Query queryCoinvoltiRaccolta = conn.createQuery(RaccoltaManager.queryCoinvoltiRaccolta(r.getId().toString()));
                    List<CoinvoltiRaccolte> coinvoltiRaccolti = (List<CoinvoltiRaccolte>) queryCoinvoltiRaccolta.executeAndFetch(CoinvoltiRaccolte.class);
                    dbConnection.setDefaultColumnMappings(RaccoltaManager.mapCoinvolti());
                    for (CoinvoltiRaccolte cr : coinvoltiRaccolti) {
                        log.info("Query coinvolti: " + RaccoltaManager.queryCoinvolti(cr.getIdCoinvolto().toString()));
                        Query queryCoinvolti = conn.createQuery(RaccoltaManager.queryCoinvolti(cr.getIdCoinvolto().toString()));
                        List<Coinvolto> coinvolts = (List<Coinvolto>) queryCoinvolti.executeAndFetch(Coinvolto.class);
                        for (Coinvolto c : coinvolts) {
                            if (c.getCap() == null) {
                                c.setCap("");
                            }
                            if (c.getVia() == null) {
                                c.setVia("");
                            }
                            if (c.getCivico() == null) {
                                c.setCivico("");
                            }
                            if (c.getNazione() == null) {
                                c.setNazione("");
                            }
                            if (c.getProvincia() == null) {
                                c.setProvincia("");
                            }
                            if (c.getComune() == null) {
                                c.setComune("");
                            }
                            r.addCoinvolto(c);
                        }
                    }
                    dbConnection.setDefaultColumnMappings(RaccoltaManager.mapSottoDocumenti());
                    Query querySottodocumenti = conn.createQuery(RaccoltaManager.querySottoDocumenti(r.getIdGddoc()));
                    Query querySottodocumentiAssociati = conn.createQuery(RaccoltaManager.querySottoDocumenti(r.getIdGddocAssociato()));
                    List<Sottodocumento> documenti = (List<Sottodocumento>) querySottodocumenti.executeAndFetch(Sottodocumento.class);
                    List<Sottodocumento> documentiAssociati = (List<Sottodocumento>) querySottodocumentiAssociati.executeAndFetch(Sottodocumento.class);
                    if (!documentiAssociati.isEmpty()) {
                        documenti.addAll(documentiAssociati);
                    }
                    Integer i = 1;
                    for (Sottodocumento d : documenti) {
                        d.setNome(r.getDocumentoBabel() + "_Allegato" + i.toString());
                        i++;
                        r.addSottodocumento(d);
                    }
                    returnRaccolta.add(r);
                }
            }
        } catch (Exception e) {
            log.error("errore nell'esecuzione della query getRaccoltaSemplice", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query getRaccoltaSemplice");
        }

        log.info("Tutto ok");

        return returnRaccolta;
    }

    public String getNomeUtente(String id) {
        return "444";
    }

    @RequestMapping(value = {"personaRS"}, method = RequestMethod.POST)
    public void dettagliPersone(@RequestBody String jsonReq,
            HttpServletRequest request) throws Http500ResponseException,
            Http404ResponseException, RestClientException {

        JSONParser jSONParser = new JSONParser();

        try {
            JSONArray jsonArray = (JSONArray) jSONParser.parse(jsonReq);
            for (Object object : jsonArray) {
                JSONObject jsonProperties = (JSONObject) object;

                String descrizione, cognome, nome, cf, pIva, email, tipologia,
                        ragioneSociale, cap, via, civico, telefono,
                        nazione, provincia, comune, indirizzoDettaglio, provenienza;

                Boolean pec, fax;

                pec = (Boolean) jsonProperties.get("pec");

                if (pec == null) {
                    pec = false;
                }

                fax = (Boolean) jsonProperties.get("fax");

                if (fax == null) {
                    fax = false;
                }

                descrizione = (String) jsonProperties.get("descrizione");

                cognome = (String) jsonProperties.get("cognome");

                nome = (String) jsonProperties.get("nome");

                cf = (String) jsonProperties.get("codice_fiscale");

                pIva = (String) jsonProperties.get("p_iva");

                email = (String) jsonProperties.get("email");

                tipologia = (String) jsonProperties.get("tipologia");

                ragioneSociale = (String) jsonProperties.get("ragione_sociale");

                cap = (String) jsonProperties.get("cap");

                via = (String) jsonProperties.get("via");

                civico = (String) jsonProperties.get("civico");

                comune = (String) jsonProperties.get("comune");

                nazione = (String) jsonProperties.get("nazione");

                provincia = (String) jsonProperties.get("provincia");

                telefono = (String) jsonProperties.get("telefono");

                indirizzoDettaglio = (String) jsonProperties.get("indirizzo");

                provenienza = (String) jsonProperties.get("provenienza");

                List<Contatto> listaContatto = new ArrayList();

                if (tipologia.equals("FISICA") && cf != null) {
                    listaContatto = contattoRepository.findByCodiceFiscale(cf);
                }
                if (tipologia.equals("GIURIDICA") && pIva != null) {
                    listaContatto = contattoRepository.findByPartitaIva(pIva);
                }
                if (tipologia == null || (cf == null && pIva == null)) {
                    if (cf != null && cf.trim().equals("") && pIva != null && pIva.trim().equals("")) {
                        throw new Http400ResponseException("400", "Tipologia necessaria");
                    }
                }

                Contatto contatto = new Contatto();

                if (!listaContatto.isEmpty()) {
                    contatto = listaContatto.get(0);
                }

                List<DettaglioContatto> dettagli = new ArrayList();

                if (listaContatto.isEmpty()) {
                    if (tipologia.equals("FISICA")) {

                        contatto.setCodiceFiscale(cf);
                        contatto.setTipo(Contatto.TipoContatto.PERSONA_FISICA);
                    }
                    if (tipologia.equals("GIURIDICA")) {
                        contatto.setRagioneSociale(ragioneSociale);
                        contatto.setPartitaIva(pIva);
                        contatto.setTipo(Contatto.TipoContatto.AZIENDA);
                    }

                    contatto.setNome(nome);
                    contatto.setCognome(cognome);
                    contatto.setModificabile(true);
                    contatto.setEliminato(false);
                    contatto.setDescrizione(descrizione);
                    contatto.setProtocontatto(true);
                    contatto.setDaVerificare(Boolean.TRUE);
                    contatto.setRiservato(false);
                    Optional<Persona> p = personaRepository.findById(1);
                    Persona pp = p.get();
                    Optional<Utente> u = utenteRepository.findById(1);
                    Utente uu = u.get();
                    contatto.setIdPersonaCreazione(pp);
                    contatto.setIdUtenteCreazione(uu);
                    Integer[] array = new Integer[]{10};
                    contatto.setIdAziende(array);
                    contatto.setProvenienza(provenienza);
                    contatto.setCategoria(Contatto.CategoriaContatto.ESTERNO);

                    if (email != null) {
                        DettaglioContatto dettaglio = new DettaglioContatto();
                        dettaglio.setIdContatto(contatto);
                        dettaglio.setDescrizione(email);
                        dettaglio.setTipo(DettaglioContatto.TipoDettaglio.EMAIL);
                        dettaglio = dettaglioRepository.save(dettaglio);
                        Email mail = new Email();
                        mail.setDescrizione(email);
                        mail.setEmail(email);
                        mail.setProvenienza(provenienza);
                        mail.setPec(pec);
                        mail.setIdContatto(contatto);
                        mail.setIdDettaglioContatto(dettaglio);
                        mail.setPrincipale(false);
                        List<Email> mails = new ArrayList<>();
                        if (contatto.getEmailList() != null) {
                            mails = contatto.getEmailList();
                        }
                        mails.add(mail);
                        contatto.setEmailList(mails);
                    }

                    if (telefono != null) {
                        DettaglioContatto dettaglio = new DettaglioContatto();
                        dettaglio.setIdContatto(contatto);
                        dettaglio.setDescrizione(telefono);
                        dettaglio.setTipo(DettaglioContatto.TipoDettaglio.TELEFONO);
                        dettaglio = dettaglioRepository.save(dettaglio);
                        Telefono tel = new Telefono();
                        tel.setFax(fax);
                        tel.setPrincipale(false);
                        tel.setNumero(telefono);
                        tel.setProvenienza(provenienza);
                        tel.setIdContatto(contatto);
                        tel.setIdDettaglioContatto(dettaglio);
                        List<Telefono> telefoni = new ArrayList<>();
                        if (contatto.getTelefonoList() != null) {
                            telefoni = contatto.getTelefonoList();
                        }
                        telefoni.add(tel);
                        contatto.setTelefonoList(telefoni);
                    }

                    if (cap != null || comune != null
                            || civico != null || via != null
                            || provincia != null
                            || nazione != null) {
                        if (cap == null) {
                            cap = "";
                        }
                        if (comune == null) {
                            comune = "";
                        }
                        if (civico == null) {
                            civico = "";
                        }
                        if (via == null) {
                            via = "";
                        }
                        if (provincia == null) {
                            provincia = "";
                        }
                        if (nazione == null) {
                            nazione = "";
                        }
                        DettaglioContatto dettaglio = new DettaglioContatto();
                        dettaglio.setIdContatto(contatto);
                        String indirizzoCompleto = via + " " + civico + " "
                                + comune + " " + cap + " "
                                + provincia + " " + nazione;
                        dettaglio.setDescrizione(indirizzoCompleto);
                        dettaglio.setTipo(DettaglioContatto.TipoDettaglio.INDIRIZZO_FISICO);
                        dettaglio = dettaglioRepository.save(dettaglio);
                        Indirizzo indirizzo = new Indirizzo();
                        indirizzo.setCap(cap);
                        indirizzo.setVia(via);
                        indirizzo.setCivico(civico);
                        indirizzo.setProvincia(provincia);
                        indirizzo.setNazione(nazione);
                        indirizzo.setComune(comune);
                        indirizzo.setIdContatto(contatto);
                        indirizzo.setIdDettaglioContatto(dettaglio);
                        indirizzo.setPrincipale(false);
                        indirizzo.setProvenienza(provenienza);
                        indirizzo.setDescrizione(indirizzoCompleto);
                        List<Indirizzo> indirizzi = new ArrayList<>();
                        if (contatto.getIndirizziList() != null) {
                            indirizzi = contatto.getIndirizziList();
                        }
                        indirizzi.add(indirizzo);
                        contatto.setIndirizziList(indirizzi);
                    }

                    if (indirizzoDettaglio != null) {
                        DettaglioContatto dettaglio = new DettaglioContatto();
                        dettaglio.setIdContatto(contatto);
                        dettaglio.setDescrizione(indirizzoDettaglio);
                        dettaglio.setTipo(DettaglioContatto.TipoDettaglio.INDIRIZZO_FISICO);
                        dettaglio = dettaglioRepository.save(dettaglio);
                    }

                    contatto = contattoRepository.save(contatto);

                } else {
                    dettagli = dettaglioRepository.findByIdContatto(contatto);
                    List<String> descrizioneDettagli = new ArrayList();

                    dettagli.forEach((d) -> descrizioneDettagli.add(d.getDescrizione()));

                    if (telefono != null) {
                        if (!descrizioneDettagli.contains(telefono)) {
                            DettaglioContatto dettaglio = new DettaglioContatto();
                            dettaglio.setIdContatto(contatto);
                            dettaglio.setDescrizione(telefono);
                            dettaglio.setTipo(DettaglioContatto.TipoDettaglio.TELEFONO);
                            dettaglio = dettaglioRepository.save(dettaglio);
                            Telefono tel = new Telefono();
                            tel.setFax(fax);
                            tel.setProvenienza(provenienza);
                            tel.setPrincipale(false);
                            tel.setNumero(telefono);
                            tel.setIdContatto(contatto);
                            tel.setIdDettaglioContatto(dettaglio);
                            List<Telefono> telefoni = new ArrayList<>();
                            if (contatto.getTelefonoList() != null) {
                                telefoni = contatto.getTelefonoList();
                            }
                            telefoni.add(tel);
                            contatto.setTelefonoList(telefoni);

                        }
                    }

                    if (email != null) {
                        if (!descrizioneDettagli.contains(email)) {
                            DettaglioContatto dettaglio = new DettaglioContatto();
                            dettaglio.setIdContatto(contatto);
                            dettaglio.setDescrizione(email);
                            dettaglio.setTipo(DettaglioContatto.TipoDettaglio.EMAIL);
                            dettaglio = dettaglioRepository.save(dettaglio);
                            Email mail = new Email();
                            mail.setDescrizione(email);
                            mail.setEmail(email);
                            mail.setProvenienza(provenienza);
                            mail.setPec(pec);
                            mail.setIdContatto(contatto);
                            mail.setPrincipale(false);
                            mail.setIdDettaglioContatto(dettaglio);
                            List<Email> mails = new ArrayList<>();
                            if (contatto.getEmailList() != null) {
                                mails = contatto.getEmailList();
                            }
                            mails.add(mail);
                            contatto.setEmailList(mails);

                        }
                    }

                    if (cap != null || comune != null
                            || civico != null || via != null
                            || provincia != null
                            || nazione != null) {
                        if (cap == null) {
                            cap = "";
                        }
                        if (comune == null) {
                            comune = "";
                        }
                        if (civico == null) {
                            civico = "";
                        }
                        if (via == null) {
                            via = "";
                        }
                        if (provincia == null) {
                            provincia = "";
                        }
                        if (nazione == null) {
                            nazione = "";
                        }
                        String indirizzoCompleto = via + " " + civico + " "
                                + comune + " " + cap + " "
                                + provincia + " " + nazione;
                        if (!descrizioneDettagli.contains(indirizzoCompleto)) {
                            DettaglioContatto dettaglio = new DettaglioContatto();
                            dettaglio.setIdContatto(contatto);
                            dettaglio.setDescrizione(indirizzoCompleto);
                            dettaglio.setTipo(DettaglioContatto.TipoDettaglio.INDIRIZZO_FISICO);
                            dettaglio = dettaglioRepository.save(dettaglio);
                            Indirizzo indirizzo = new Indirizzo();
                            indirizzo.setCap(cap);
                            indirizzo.setVia(via);
                            indirizzo.setCivico(civico);
                            indirizzo.setProvincia(provincia);
                            indirizzo.setNazione(nazione);
                            indirizzo.setComune(comune);
                            indirizzo.setIdContatto(contatto);
                            indirizzo.setIdDettaglioContatto(dettaglio);
                            indirizzo.setPrincipale(false);
                            indirizzo.setProvenienza(provenienza);
                            indirizzo.setDescrizione(indirizzoCompleto);
                            List<Indirizzo> indirizzi = new ArrayList<>();
                            if (contatto.getIndirizziList() != null) {
                                indirizzi = contatto.getIndirizziList();
                            }
                            indirizzi.add(indirizzo);
                            contatto.setIndirizziList(indirizzi);

                        }

                        if (indirizzoDettaglio != null) {
                            if (!descrizioneDettagli.contains(indirizzoCompleto)) {
                                DettaglioContatto dettaglio = new DettaglioContatto();
                                dettaglio.setIdContatto(contatto);
                                dettaglio.setDescrizione(indirizzoCompleto);
                                dettaglio.setTipo(DettaglioContatto.TipoDettaglio.INDIRIZZO_FISICO);
                                dettaglio = dettaglioRepository.save(dettaglio);
                            }
                        }
                    }
                    contatto = contattoRepository.save(contatto);
                }
            }
        } catch (Throwable e) {
            log.error("Errore nel salvataggio del contatto", e);
        }
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
            log.info("esecuzione query annullamento: " + queryWithParams.toString());
            String lista = (String) queryWithParams.executeAndFetchFirst(String.class);
            if (lista == null || lista.equals("{}")) {
                Query addingStorico = conn.createQuery("UPDATE gd.raccolte set storico = '{\"storico\": []}' where id = " + id_raccolta);
                addingStorico.executeUpdate();
                return null;
            }
            JSONObject jsonReq = (JSONObject) parser.parse(lista);
            JSONArray jArray = (JSONArray) jsonReq.get("storico");
            for (Object json : jArray) {
                if (json instanceof JSONObject) {
                    String utente, data, motivo, stato;

                    if (((JSONObject) json).get("utente") == null) {
                        utente = "Utente non inserito";
                    } else {
                        utente = ((JSONObject) json).get("utente").toString();
                    }

                    if (((JSONObject) json).get("data") == null) {
                        data = "Data non inserita";
                    } else {
                        data = ((JSONObject) json).get("data").toString();
                    }

                    if (((JSONObject) json).get("motivazione") == null) {
                        motivo = "Motivazione non inserita";
                    } else {
                        motivo = ((JSONObject) json).get("motivazione").toString();
                    }

                    if (((JSONObject) json).get("stato") == null) {
                        stato = "ATTIVO";
                    } else {
                        stato = ((JSONObject) json).get("stato").toString();
                    }
                    Storico s = new Storico(utente, motivo, stato, data);
                    returnList.add(s);
                    log.info("Inserito annullamento del " + s.getData());
                }
            }

            return returnList;

        } catch (Exception e) {
            log.error("errore nell'esecuzione della query annullamenti", e);
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
            log.info("esecuzione query annullamento: " + queryWithParams.toString());
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
            log.error("errore nell'esecuzione della query annullamenti", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query di storico degli annullamenti");
        }
    }

    @RequestMapping(value = "ricerca", method = RequestMethod.GET)
    public List<Raccolta> ricerca(
            @RequestParam(required = false, value = "numero") String numero,
            @RequestParam(required = false, value = "applicazioneChiamante") String applicazione,
            @RequestParam(required = false, value = "tipoDocumento") String tipoDocumento,
            @RequestParam(required = false, value = "oggetto") String oggetto,
            @RequestParam(required = false, value = "fascicoli") String fascicolo,
            @RequestParam(required = false, value = "documentoBabel") String documentoBabel,
            @RequestParam(required = false, value = "creatore") String creatore,
            @RequestParam(required = false, value = "descrizioneStruttura") String struttura,
            @RequestParam(required = false, value = "createTime") String data,
            @RequestParam(required = false, value = "piva") String piva,
            @RequestParam(required = false, value = "stato") String stato,
            @RequestParam(required = false, value = "cf") String cf,
            @RequestParam(required = true, value = "offset") Integer offset,
            @RequestParam(required = true, value = "limit") Integer limit,
            HttpServletRequest request) throws Http500ResponseException,
            Http404ResponseException, RestClientException {

        Sql2o dbConnection = postgresConnectionManager.getDbConnection("999");
        List<Raccolta> returnRaccolta = new ArrayList<Raccolta>();

        try ( Connection conn = (Connection) dbConnection.open()) {

            dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryGetRaccoltaSemplice());
            String query = "";

            if (numero != null) {
                if (query == "") {
                    query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE codice ilike '%" + numero + "%' ";
                } else {
                    query = query + " and r.codice ilike '%" + numero + "%' ";
                }
                log.info("Query: " + query);
            }

            if (applicazione != null) {
                if (query == "") {
                    query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE applicazione_chiamante ilike '%" + applicazione + "%' ";
                } else {
                    query = query + " and r.applicazione_chiamante ilike '%" + applicazione + "%' ";
                }
                log.info("Query: " + query);
            }

            if (stato != null) {
                if (query == "") {
                    if (stato.toUpperCase().startsWith("AN")) {
                        stato = "ANNULLATO";
                    }
                    if (stato.toUpperCase().startsWith("AT")) {
                        stato = "ATTIVO";
                    }
                    query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE r.stato = '" + stato + "' ";
                } else {
                    if (stato.toUpperCase().startsWith("AN")) {
                        stato = "ANNULLATO";
                    }
                    if (stato.toUpperCase().startsWith("AT")) {
                        stato = "ATTIVO";
                    }
                    query = query + " and r.stato = '" + stato + "' ";
                }
                log.info("Query: " + query);
            }

            if (tipoDocumento != null) {
                if (query == "") {
                    query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE r.tipo_documento ilike '%" + tipoDocumento + "%' ";
                } else {
                    query = query + " and r.tipo_documento ilike '%" + tipoDocumento + "%' ";
                }
                log.info("Query: " + query);
            }

            if (oggetto != null) {
                if (query == "") {
                    query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE oggetto ilike '%" + oggetto + "%' ";
                } else {
                    query = query + " and r.oggetto ilike '%" + oggetto + "%' ";
                }
                log.info("Query: " + query);
            }

            if (struttura != null) {
                if (query == "") {
                    query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE descrizione_struttura ilike '%" + struttura + "%' ";
                } else {
                    query = query + " and r.descrizione_struttura ilike '%" + struttura + "%' ";
                }
                log.info("Query: " + query);
            }

            if (creatore != null) {
                if (query == "") {
                    query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE creatore ilike '%" + creatore + "%' ";
                } else {
                    query = query + " and r.creatore ilike '%" + creatore + "%' ";
                }
                log.info("Query: " + query);
            }

            if (data != null) {

                Date date1 = new SimpleDateFormat("dd/MM/yyyy").parse(data);

                SimpleDateFormat sdfSQL = new SimpleDateFormat("yyyy-MM-dd");

                data = sdfSQL.format(date1);

                if (query == "") {
                    query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE create_time::date = '" + data + "'::date ";
                } else {
                    query = query + " and r.create_time::date = '" + data + "'::date ";
                }
                log.info("Query: " + query);
            }

            if (fascicolo != null) {
                String queryFascicoli = "SELECT id_fascicolo from gd.fascicoligd f WHERE numerazione_gerarchica ilike '%" + fascicolo + "%' ";
                Query queryForFascicoli = conn.createQuery(queryFascicoli);
                List<String> datiFascicoli = (List<String>) queryForFascicoli.executeAndFetch(String.class);
                List<String> listId = new ArrayList();
                for (String f : datiFascicoli) {

                    Query queryIdGddocs = conn.createQuery("SELECT id_gddoc from gd.fascicoli_gddocs WHERE id_fascicolo = '" + f + "' ");
                    List<String> datiGddoc = (List<String>) queryIdGddocs.executeAndFetch(String.class);
                    listId.addAll(datiGddoc);
                }
                int countId = 0;
                Boolean firstTime = true;
                for (String idG : listId) {
                    if (query == "") {
                        query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE  (id_gddoc = '" + idG + "' ";
                        countId++;
                        firstTime = false;
                        if (listId.size() == 1) {
                            query = query + ") ";
                            break;
                        }
                    } else {
                        if (firstTime) {
                            if (listId.size() == 1) {
                                query = query + " and id_gddoc = '" + idG + "' ";
                            } else {
                                query = query + " and ( id_gddoc = '" + idG + "' ";
                            }

                        }
                        if (countId == listId.size() - 1) {
                            //log.info("Sono all'ultimo");
                            query = query + " or id_gddoc = '" + idG + "') ";
                        }
                        if (!firstTime && countId < listId.size() - 1) {
                            //log.info(("Non sono all'ultimo"));
                            query = query + " or id_gddoc = '" + idG + "' ";
                        }
                        firstTime = false;
                        countId++;
                    }
                }
            }

            if (piva != null && !piva.trim().equals("")) {

                Query queryPiva = conn.createQuery("SELECT id FROM gd.coinvolti WHERE partitaiva = '" + piva + "' ");
                List<Integer> listCoinvolti = queryPiva.executeAndFetch(Integer.class);

                String stringQuery = "SELECT id_raccolta from gd.coinvolti_raccolte WHERE id = ";

                for (int i = 0; i < listCoinvolti.size(); i++) {
                    if (listCoinvolti.size() == 1) {
                        stringQuery = stringQuery + listCoinvolti.get(i).toString();
                    } else {
                        if (i == 0) {
                            stringQuery = stringQuery + listCoinvolti.get(i).toString();
                        } else {
                            stringQuery = stringQuery + " OR id = " + listCoinvolti.get(i).toString();
                        }
                    }
                }

                boolean firsttime = true;

                Query queryCoinvolti = conn.createQuery(stringQuery);

                List<Integer> listRaccolte = queryCoinvolti.executeAndFetch(Integer.class);

                for (int j = 0; j < listRaccolte.size(); j++) {
                    if (listRaccolte.size() == 1) {
                        if (query == "") {
                            query = "SELECT * from gd.raccolte WHERE id = " + listRaccolte.get(j) + " ";
                            firsttime = false;
                        } else {
                            query = query + " AND ( id = " + listRaccolte.get(j);
                            firsttime = false;
                        }
                    } else {
                        if (query == "") {
                            query = "SELECT * from gd.raccolte WHERE (id = " + listRaccolte.get(j) + " ";
                            firsttime = false;
                        } else {
                            if (firsttime) {
                                query = query + " AND ( id = " + listRaccolte.get(j) + " ";
                                firsttime = false;
                            }

                            if (j == listRaccolte.size() - 1) {
                                query = query + " OR id = " + listRaccolte.get(j) + " ) ";
                                firsttime = false;
                            } else {
                                query = query + " OR id = " + listRaccolte.get(j) + " ";
                                firsttime = false;
                            }
                        }
                    }
                }
            }

            if (cf != null && !cf.trim().equals("")) {
                Query queryPiva = conn.createQuery("SELECT id FROM gd.coinvolti WHERE cf = '" + cf + "' ");
                List<Integer> listCoinvolti = queryPiva.executeAndFetch(Integer.class);

                String stringQuery = "SELECT id_raccolta from gd.coinvolti_raccolte WHERE id = ";

                for (int i = 0; i < listCoinvolti.size(); i++) {
                    if (listCoinvolti.size() == 1) {
                        stringQuery = stringQuery + listCoinvolti.get(i).toString();
                        i = listCoinvolti.size() + 3;
                    } else {
                        if (i == 0) {
                            stringQuery = stringQuery + listCoinvolti.get(i).toString();
                        } else {
                            stringQuery = stringQuery + " OR id = " + listCoinvolti.get(i).toString();
                        }
                    }
                }

                Query queryCoinvolti = conn.createQuery(stringQuery);

                boolean firsttime = true;

                List<Integer> listRaccolte = queryCoinvolti.executeAndFetch(Integer.class);

                for (int j = 0; j < listRaccolte.size(); j++) {
                    if (listRaccolte.size() == 1) {
                        if (query == "") {
                            query = "SELECT * from gd.raccolte WHERE id = " + listRaccolte.get(j);
                            firsttime = false;
                        } else {
                            query = query + " AND ( id = " + listRaccolte.get(j) + " )";
                            firsttime = false;
                        }
                    } else {
                        if (query == "") {
                            query = "SELECT * from gd.raccolte WHERE ( id = " + listRaccolte.get(j);
                            firsttime = false;
                        } else {
                            if (firsttime) {
                                query = query + " AND ( id = " + listRaccolte.get(j) + " ";
                                firsttime = false;
                            }
                            if (j == listRaccolte.size() - 1) {
                                query = query + " OR id = " + listRaccolte.get(j) + " ) ";
                                firsttime = false;
                            } else {
                                query = query + " OR id = " + listRaccolte.get(j) + " ";
                                firsttime = false;
                            }
                        }
                    }
                }
            }

            if (documentoBabel != null) {
                Query queryGddocs = conn.createQuery(RaccoltaManager.queryNomeGddoc(documentoBabel));

                List<String> listGddocs = queryGddocs.executeAndFetch(String.class);

                int countId = 0;
                Boolean firstTime = true;

                for (String idG : listGddocs) {
                    if (query == "") {
                        query = "SELECT count(r.id) OVER() as rows, r.* from gd.raccolte r WHERE  (id_gddoc_associato = '" + idG + "' ";
                        countId++;
                        firstTime = false;
                        if (listGddocs.size() == 1) {
                            query = query + ") ";
                            break;
                        }
                    } else {
                        if (firstTime && listGddocs.size() == 1) {
                            query = query + " and id_gddoc_associato = '" + idG + "' ";
                            countId++;
                        }
                        if (firstTime && listGddocs.size() > 1) {
                            query = query + " and ( id_gddoc_associato = '" + idG + "' ";
                            countId++;
                        }
                        if (countId == listGddocs.size() - 1) {
                            //log.info("Sono all'ultimo");
                            query = query + " or id_gddoc_associato = '" + idG + "' ) ";
                            countId++;
                        }
                        if (!firstTime && countId < listGddocs.size() - 1) {
                            //log.info(("Non sono all'ultimo"))
                            query = query + " or id_gddoc_associato = '" + idG + "' ";
                            countId++;
                        }
                        firstTime = false;
                    }
                }
            }
            if (query.equals("")) {
                return null;
            }

            query = query + " order by create_time desc LIMIT " + limit + " OFFSET " + offset + " ";
            log.info("Query: " + query);
            Query queryWithParams = conn.createQuery(query);
            List<Raccolta> datiRaccolta = (List<Raccolta>) queryWithParams.executeAndFetch(Raccolta.class);
            for (Raccolta r : datiRaccolta) {
                dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryCodiceBabel());
                Query queryCodice = conn.createQuery(RaccoltaManager.queryCodiceBabel(r.getIdGddocAssociato()));
                List<DocumentoBabel> doc = (List<DocumentoBabel>) queryCodice.executeAndFetch(DocumentoBabel.class);
                if ((doc == null || doc.isEmpty()) || doc.get(0).getNumero() == null || doc.get(0).getCodiceRegistro() == null
                        || doc.get(0).getAnno() == null || doc.get(0).getNumero().isEmpty()
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
                log.info("Query raccolta coinvolti: " + RaccoltaManager.queryCoinvoltiRaccolta(r.getId().toString()));
                Query queryCoinvoltiRaccolta = conn.createQuery(RaccoltaManager.queryCoinvoltiRaccolta(r.getId().toString()));
                List<CoinvoltiRaccolte> coinvoltiRaccolti = (List<CoinvoltiRaccolte>) queryCoinvoltiRaccolta.executeAndFetch(CoinvoltiRaccolte.class);
                dbConnection.setDefaultColumnMappings(RaccoltaManager.mapCoinvolti());
                for (CoinvoltiRaccolte cr : coinvoltiRaccolti) {
                    log.info("Query coinvolti: " + RaccoltaManager.queryCoinvolti(cr.getIdCoinvolto().toString()));
                    Query queryCoinvolti = conn.createQuery(RaccoltaManager.queryCoinvolti(cr.getIdCoinvolto().toString()));
                    List<Coinvolto> coinvolts = (List<Coinvolto>) queryCoinvolti.executeAndFetch(Coinvolto.class);
                    for (Coinvolto c : coinvolts) {
                        if (c.getCap() == null) {
                            c.setCap("");
                        }
                        if (c.getCivico() == null) {
                            c.setCivico("");
                        }
                        if (c.getComune() == null) {
                            c.setComune("");
                        }
                        if (c.getNazione() == null) {
                            c.setNazione("");
                        }
                        if (c.getProvincia() == null) {
                            c.setProvincia("");
                        }
                        if (c.getVia() == null) {
                            c.setVia("");
                        }
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
            log.error("Errore nell'esecuzione della query di ricerca raccolta: ", e);
        }
        log.info("TUTTO OK");
        return returnRaccolta;
    }

    @RequestMapping(value = {"getFascicoliArgo"}, method = RequestMethod.GET)
    public List<Fascicolo> getFascicoliArgo(@RequestParam("azienda") String azienda,
            @RequestParam("idusr") String idUtente,
            @RequestParam("param") String param,
            HttpServletRequest request) throws Http500ResponseException, Http404ResponseException, RestClientException {

        // Prendo la connessione dal connection manager
        String codiceAzienda = azienda.substring(3);
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryGetFascicoli());

        List<Fascicolo> fascicoli;
        List<Fascicolo> returnFascicoli = new ArrayList<Fascicolo>();

        try ( Connection conn = (Connection) dbConnection.open()) {
            Query queryWithParams = conn.createQuery(RaccoltaManager.queryGetFascicoli(idUtente, param));
            log.info("esecuzione query getFascicoli: " + queryWithParams.toString());
            fascicoli = (List<Fascicolo>) queryWithParams.executeAndFetch(Fascicolo.class);
            for (Fascicolo fascicolo : fascicoli) {
                returnFascicoli.add(fascicolo);
            }
        } catch (Exception e) {
            log.error("errore nell'esecuzione della query getRaccoltaSemplice", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query getRaccoltaSemplice");
        }

        log.info("Tutto ok");

        return returnFascicoli;
    }

    @RequestMapping(value = {"getDocumentiArgo"}, method = RequestMethod.GET)
    public List<DocumentoBabel> getDocumentiArgo(@RequestParam("azienda") String azienda,
            @RequestParam("idusr") String idUtente,
            @RequestParam("reg") String codiceRegistro,
            @RequestParam("param") String param,
            HttpServletRequest request) throws Http500ResponseException, Http404ResponseException, RestClientException {

        // Prendo la connessione dal connection manager
        String codiceAzienda = azienda.substring(3);
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        dbConnection.setDefaultColumnMappings(RaccoltaManager.mapQueryGetDocumentiBabel());

        List<DocumentoBabel> documentiBabel;
        List<DocumentoBabel> returnDocumentiBabel = new ArrayList<DocumentoBabel>();

        int index = param.indexOf('/');
        Integer anno = null;
        String oggetto = null;
        String numero = null;
        if (index != -1) {
            numero = param.substring(0, param.indexOf('/'));
            String right = param.substring(param.indexOf('/') + 1);
            try {
                String annoStr = right.substring(0, 4);
                anno = Integer.valueOf(annoStr);
                oggetto = right.substring(5, right.length() - 1).trim();
            } catch (Exception e) {
                // anno non valido
            }
        } else {
            //ricerca solo numero; controllo se è un numero altrimenti sto cercando per oggetto
            try {
                Integer val = Integer.valueOf(param);
                if (val != null) {
                    numero = param;
                }
            } catch (Throwable e) {
                oggetto = param;
            }
        }

        try ( Connection conn = (Connection) dbConnection.open()) {
            Query queryWithParams = conn.createQuery(RaccoltaManager.queryGetProtocolliBabel(idUtente, numero, anno, oggetto));
            log.info("esecuzione query getProtocolli: " + queryWithParams.toString());
            documentiBabel = (List<DocumentoBabel>) queryWithParams.executeAndFetch(DocumentoBabel.class);
            for (DocumentoBabel d : documentiBabel) {
                returnDocumentiBabel.add(d);
            }
        } catch (Exception e) {
            log.error("errore nell'esecuzione della query getRaccoltaSemplice", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query getRaccoltaSemplice");
        }
        log.info("Tutto ok");

        return returnDocumentiBabel;
    }

    @RequestMapping(value = "createRS", method = RequestMethod.POST)
    public String createRS(
            @RequestPart("applicazione_chiamante") String applicazioneChiamante,
            @RequestPart("azienda") String azienda,
            @RequestPart("oggetto") String oggetto,
            @RequestPart("numero_documento_origine") Optional<String> numeroDocumentoOrigineOpt,
            @RequestPart("anno_documento_origine") Optional<String> annoDocumentoOrigineStrOpt,
            @RequestPart("codice_registro_origine") Optional<String> codiceRegistroOrigineOpt,
            @RequestPart("fascicoli_babel") String fascicoliBabelStr,
            @RequestPart("tipo_documento") String tipoDocumento,
            @RequestPart("struttura_responsabile") String strutturaResponsabile,
            @RequestPart("persone") String personeStr,
            @RequestPart("allegati") Optional<List<MultipartFile>> allegati,
            HttpServletRequest request) throws HttpInternautaResponseException, Throwable {

        // restituiamo n_rs_generato/anno_rs_generato in babel
        String result = "empty";

        String numeroDocumentoOrigine = null;
        String annoDocumentoOrigineStr = null;
        String codiceRegistroOrigine = null;

        if (numeroDocumentoOrigineOpt.isPresent()) {
            numeroDocumentoOrigine = numeroDocumentoOrigineOpt.get();
        }

        if (annoDocumentoOrigineStrOpt.isPresent()) {
            annoDocumentoOrigineStr = annoDocumentoOrigineStrOpt.get();
        }

        if (codiceRegistroOrigineOpt.isPresent()) {
            codiceRegistroOrigine = codiceRegistroOrigineOpt.get();
        }

        // controllo dati
        if (azienda == null) {
            throw new Http400ResponseException("400", "il parametro del body azienda è obbligatorio");
        }
        String codiceAzienda = azienda.substring(3);

        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente loggedUser = authenticatedUserProperties.getUser();
        String creatore = loggedUser.getIdPersona().getDescrizione();
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
//        try ( Connection conn = (Connection) dbConnection.open()) {
//            Query queryName = conn.createQuery(RaccoltaManager.queryCreatoreName(idPersona));
//            creatore = queryName.executeAndFetchFirst(String.class);
//
//        } catch(Exception e) {
//            throw new Http400ResponseException("400", "Errore nel reperimento delle info del creatore");
//        }

        if (oggetto == null) {
            throw new Http400ResponseException("400", "il parametro del body oggetto è obbligatorio");
        }

        if (tipoDocumento == null) {
            throw new Http400ResponseException("400", "il parametro del body tipo_documento è obbligatorio");
        }

        if (strutturaResponsabile == null) {
            throw new Http400ResponseException("400", "il parametro del body struttura_responsabile è obbligatorio");
        }

        Integer annoDocumentoOrigine = null;
        if (numeroDocumentoOrigine != null && !numeroDocumentoOrigine.isEmpty()) {
            try {
                annoDocumentoOrigine = Integer.valueOf(annoDocumentoOrigineStr);

            } catch (Exception ex) {
                throw new Http400ResponseException("400", "il parametro del body anno_documento_origine non è un intero");
            }
        }
        // se c'è un componente della tupla di riferimento di un doc Babel allora devono esserci tutti e tre i parametri
        boolean riferimentoDocumentoConsistente = riferimentoDocumentoConsistente(numeroDocumentoOrigine, annoDocumentoOrigine, codiceRegistroOrigine);
        if (riferimentoDocumentoConsistente == false) {
            throw new Http400ResponseException("400", "i parametri di riferimento a un documento in Babel non sono coerenti");
        }

        JSONParser parser = new JSONParser();

        JSONArray fascicoliBabel = (JSONArray) parser.parse(fascicoliBabelStr);

        // gestione persone
        JSONArray personeArray = (JSONArray) parser.parse(personeStr);

        List<PersonaRS> personeNoDescrizione = PersonaRS.parse(objectMapper, personeArray.toJSONString());
        List<PersonaRS> persone = new ArrayList<PersonaRS>();
        for (PersonaRS p : personeNoDescrizione) {
            p.createDescrizione();
            persone.add(p);
        }
        if (persone.size() == 0) {
            log.info(String.format("nessuna persona inserita nella richiesta"));
        }
        for (PersonaRS persona : persone) {
            if (persona.isSalvaContatto()) {

                log.info(String.format("salvataggio contatto in rubrica: %s", persona.getDescrizione()));
                Optional<Persona> p = personaRepository.findById(1);
                Optional<Utente> u = utenteRepository.findById(1);
                Integer idAzienda = postgresConnectionManager.getIdAzienda(codiceAzienda);
                Contatto toContact = PersonaRS.toContatto(idAzienda, persona, p.get(), u.get(), Boolean.TRUE);
                String res = contattoRepository.getSimilarContacts(persona.dumpToString(objectMapper, persona), idAzienda.toString());
                SqlSimilarityResults similarityResults = objectMapper.readValue(res, SqlSimilarityResults.class);
                contattoRepository.save(toContact);

            } else {
                log.info(String.format("contatto da non salvare in rubrica: %s", persona.getDescrizione()));
            }

            // se tipologia non è settata impostala di default a FISICA
            persona.checkTipologia();
        }

        //MongoWrapper mongo = aziendaParamsManager.getStorageConnection(codiceAzienda);
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();

        boolean rifDocumentoInBabel = riferimentoDocumentoInBabel(numeroDocumentoOrigine, annoDocumentoOrigine, codiceRegistroOrigine);
        String idGddocContenuto = null;
        if (rifDocumentoInBabel) {
            idGddocContenuto = isValidRecordInBabel(codiceAzienda, numeroDocumentoOrigine, annoDocumentoOrigine, codiceRegistroOrigine);
            if (idGddocContenuto == null || idGddocContenuto.isEmpty()) {
                log.info(String.format("riferimento al record in Babel %s%s/%d non valido", codiceRegistroOrigine, numeroDocumentoOrigine, annoDocumentoOrigine));
                throw new Http404ResponseException("404", "riferimento al record in Babel non valido");
            }
        }

        // trasformo MultipartFile in InputStream
        org.json.simple.JSONArray jsonAllegati = new org.json.simple.JSONArray();
        List<MultipartFile> allegatiList = allegati.orElse(Collections.emptyList());
        log.info("Allegati: " + allegatiList.size());

        // se non ci sono allegati allora deve esserci un riferimento a un record su Babel valido; altrimenti non è consistente la RS
        if (allegatiList.size() == 0 && !rifDocumentoInBabel) {
            throw new Http404ResponseException("404", "richiesta inconsistente: non ci sono allegati e riferimento di un record di Babel incoerente");
        }

        // non ci deve essere il caso di un riferimento a un record su Babel e allegati associati
        if (allegatiList.size() > 0 && rifDocumentoInBabel) {
            throw new Http404ResponseException("404", "richiesta inconsistente: se si fa riferimento a un record di Babel non si possono inviare allegati");
        }

        // ottenimento id struttura in argo
        Map<String, String> res = getStrutturaArgo(codiceAzienda, Integer.valueOf(strutturaResponsabile));

        // creazione del documento di RS
        try ( Connection conn = (Connection) dbConnection.beginTransaction()) {
            conn.setRollbackOnException(true);

            RaccoltaNew raccolta = null;
            // creazione gddoc
            raccolta = createGdDoc(conn, codiceAzienda, oggetto);

            // creazione sottodocumenti con relativi allegati
            insertSottoDocumenti(conn, jsonAllegati, raccolta);

            // fascicolazione
            List<String> idIndeList = new ArrayList<>();
            if (fascicoliBabel != null && !fascicoliBabel.isEmpty()) {
                for (int i = 0; i < fascicoliBabel.size(); i++) {
                    idIndeList.add(getIdFascicoliFromNumerazioneGerarchica(codiceAzienda, (String) fascicoliBabel.get(i)));
                }
                insertFascicoliGddocs(conn, idIndeList, raccolta);
            }

            //controlla se raccolta != null e procedi
            if (raccolta == null) {
                throw new Http500ResponseException("500", "errore creazione gddoc della Raccolta Semplice");
            }
            raccolta.setApplicazioneChiamante(applicazioneChiamante);
            raccolta.setCreatore(creatore);
            raccolta.setTipoDocumento(tipoDocumento);
            raccolta.setIdStrutturaResponsabileInternauta(Integer.valueOf(strutturaResponsabile));
            raccolta.setIdStrutturaResponsabileArgo(res.get("ID_STRUTTURA"));
            raccolta.setDescrizioneStruttura(res.get("NOME_STRUTTURA"));
            if (rifDocumentoInBabel) {
                raccolta.setIdGddocAssociato(idGddocContenuto);
            }
            // creazione raccolta su db
            Integer idRaccolta = createRaccolta(conn, raccolta);
            // crea coinvolti e aggiorna tabella di cross coinvolti_raccolte
            for (PersonaRS persona : persone) {
                boolean isInserted = createCoinvolto(conn, persona, idRaccolta);
                if (!isInserted) {
                    conn.rollback();
                    throw new Http500ResponseException("500", "errore nella creazione delle persone");
                }
            }
            conn.commit();
            // ritorno il riferimento del gddoc riferito alla RS appena creata
            result = String.format("%s/%d", raccolta.getNumeroRegistrazione(), raccolta.getAnnoRegistrazione());

            return result;
        }
    }

    /**
     * *
     * i riferimenti a un documento in Babel o ci sono tutti o nulla
     *
     * @param num - numero registrazione
     * @param anno - anno registrazione
     * @param codice - codice registro
     * @return TRUE se i tre campi sono consistenti tra loro, FALSE altrimenti
     */
    private boolean riferimentoDocumentoConsistente(String num, Integer anno, String codice) {
        boolean res = false;
        if ((num != null && anno != null && codice != null) || (num == null && anno == null && codice == null)) {
            res = true;
        }
        return res;
    }

    /**
     * *
     * controlla se ci si riferisce ad un documento in Babel oppure no
     *
     * @param num - numero registrazione
     * @param anno - anno registrazione
     * @param codice - codice registro
     * @return TRUE se ci si riferisce ad un documento interno a Babel, FALSE
     * altrimenti
     */
    private boolean riferimentoDocumentoInBabel(String num, Integer anno, String codice) {
        boolean res = false;
        if (num != null && anno != null && codice != null) {
            res = true;
        }
        return res;
    }

    public String isValidRecordInBabel(String codiceAzienda, String numero, Integer anno, String codice) throws Http500ResponseException {

        String res = null;

        String sql = "SELECT id_gddoc "
                + "FROM gd.gddocs "
                + "WHERE numero_registrazione = :numero "
                + "AND anno_registrazione = :anno "
                + "AND codice_registro = :codice ";

        List<Row> rows = null;
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        try ( Connection conn = (Connection) dbConnection.open()) {
            Query q = conn.createQuery(sql)
                    .addParameter("numero", numero)
                    .addParameter("anno", anno)
                    .addParameter("codice", codice);

            rows = q.executeAndFetchTable().rows();
            if (rows == null || rows.isEmpty()) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO);
            } else if (rows.size() > 1) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.PIU_RISULTATI);
            }

            String idGdDoc = rows.get(0).getString("id_gddoc");
            if (idGdDoc != null && !idGdDoc.isEmpty()) {
                res = idGdDoc;
            }
        } catch (Throwable e) {
            throw new Http500ResponseException("500", "errore reperimento record in Babel", e);
        }
        return res;
    }

    public Map<String, String> getStrutturaArgo(String codiceAzienda, Integer idStrutturaInternauta) throws Http500ResponseException {

        Map<String, String> res = null;
        List<Row> rows = null;
        String sql = "SELECT id_struttura, nome_struttura "
                + "FROM procton.strutture "
                + "WHERE id_struttura_internauta = :id_struttura "
                + "AND attiva != 0 "
                + "ORDER BY data_attivazione DESC LIMIT 1";

        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        try ( Connection conn = (Connection) dbConnection.open()) {
            Query q = conn.createQuery(sql)
                    .addParameter("id_struttura", idStrutturaInternauta);

            log.info("query: " + q.toString());
            rows = q.executeAndFetchTable().rows();
            if (rows == null || rows.isEmpty()) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO);
            } else if (rows.size() > 1) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.PIU_RISULTATI);
            }
            res = new HashMap<>();
            res.put("ID_STRUTTURA", rows.get(0).getString("id_struttura"));
            res.put("NOME_STRUTTURA", rows.get(0).getString("nome_struttura"));
        } catch (Throwable e) {
            log.error("errore creazione raccolta", e);
            throw new Http500ResponseException("500", "errore creazione raccolta", e);
        }
        return res;
    }

    @SuppressWarnings("empty-statement")
    private static boolean signatureFileAccepted(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        System.out.println("QWERTY signatureFileAccepted " + sb.toString() + " con questa condizione " + SupportedSignatureType.contains(sb.toString()));
        //verifico che sia tra gli enum accettati 7z msg

        if (SupportedSignatureType.contains(sb.toString())) {
            return true;

        }
        return false;
    }

    public RaccoltaNew createGdDoc(Connection conn, String codiceAzienda, String oggetto) throws IOException, UnsupportedEncodingException, Http500ResponseException, Sql2oSelectException {

        RaccoltaNew res = null;
        Map<String, String> indeIdAndGuid = getIndeIdAndGuid(codiceAzienda, 1).get(0);
        int anno = Calendar.getInstance().get(Calendar.YEAR);

        try {
            String idOggettoOrigine = String.format("babel_suite_%s", indeIdAndGuid.get("document_guid"));
            String codice = String.format("babel_%s", indeIdAndGuid.get("document_guid"));
            String numeroRegistrazione = staccaNumero(conn, anno, "rs", codiceAzienda);
            String nomeGddoc = String.format("RS%s/%s:%s", String.valueOf(numeroRegistrazione), String.valueOf(anno), oggetto);

            // dalla query il nome del campo è gd_sequences_nextval
            String sql = "INSERT INTO gd.gddocs "
                    + "(id_gddoc, nome_gddoc, "
                    + "tipo_gddoc, data_ultima_modifica, "
                    + "stato_gd_doc, data_gddoc, guid_gddoc, codice_registro, "
                    + "data_registrazione, numero_registrazione, "
                    + "anno_registrazione, "
                    + "tipo_oggetto_origine, id_oggetto_origine, oggetto, "
                    + "codice, numerazione_automatica, "
                    + "applicazione, id_utente_creazione, tipologia_documentale) "
                    + "VALUES( "
                    + ":id_gddoc, :nome_gddoc, "
                    + "'r', now(), "
                    + "1, now(), :guid_gddoc, 'RS', "
                    + "now(), :numero_registrazione, "
                    + ":anno_registrazione, "
                    + "'DocumentoRS', :id_oggetto_origine, :oggetto, "
                    + ":codice, 0, "
                    + "'GEDI', 'internauta_bridge', 'RaccoltaSemplice') ";

            Query q = conn.createQuery(sql)
                    .addParameter("id_gddoc", indeIdAndGuid.get("document_id"))
                    .addParameter("nome_gddoc", nomeGddoc)
                    .addParameter("guid_gddoc", indeIdAndGuid.get("document_guid"))
                    .addParameter("numero_registrazione", numeroRegistrazione)
                    .addParameter("anno_registrazione", anno)
                    .addParameter("id_oggetto_origine", idOggettoOrigine)
                    .addParameter("oggetto", oggetto)
                    .addParameter("codice", codice);

            log.info("query: " + q.toString());
            int result = q.executeUpdate().getResult();
            log.info("Righe coinvolte: " + result);
            if (result == 0) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO, "Errore: nessun gddoc creato");
            }
            // ritorna il guid del gddoc appena creato
            res = new RaccoltaNew();
            res.setIdGdDoc(indeIdAndGuid.get("document_id"));
            res.setNumeroRegistrazione(String.valueOf(numeroRegistrazione));
            res.setAnnoRegistrazione(anno);
            res.setOggetto(oggetto);
            res.setCodiceAzienda(codiceAzienda);

        } catch (Throwable e) {
            log.error("errore creazione documento gddoc RS", e);
            throw new Http500ResponseException("500", "errore creazione documento gddoc RS", e);
        }
        return res;
    }

    public void insertSottoDocumenti(Connection conn, org.json.simple.JSONArray jsonAllegati, RaccoltaNew r) throws Http500ResponseException {
        Integer idAzienda = postgresConnectionManager.getIdAzienda(r.getCodiceAzienda());
        MongoWrapper mongo = aziendeConnectionManager.getRepositoryWrapper(idAzienda);

        List<String> idSottoDocumenti = new ArrayList<>();

        try {

            for (int i = 0; i < jsonAllegati.size(); i++) {
                org.json.simple.JSONObject jsonAllegato = (org.json.simple.JSONObject) jsonAllegati.get(i);

                String filename = (String) jsonAllegato.get("nome_file");
                String uuid = (String) jsonAllegato.get("uuid_file");
                String mimetype = (String) jsonAllegato.get("mime_type");
                Boolean daConvertire = (Boolean) jsonAllegato.get("da_convertire");

                SottoDocumentoGdDoc sd = new SottoDocumentoGdDoc();
                Map<String, String> indeIdAndGuid = getIndeIdAndGuid(r.getCodiceAzienda(), 1).get(0);

                sd.setId(indeIdAndGuid.get("document_id"));
                sd.setGuid(indeIdAndGuid.get("document_guid"));
                sd.setIdGdDoc(r.getIdGdDoc());
                sd.setNome(filename);
                sd.setUuidMongoOriginale(uuid);
                sd.setMimetypeFileOriginale(mimetype);
                sd.setDimensioneOriginale(mongo.getSizeByUuid(uuid));

                sd.setConvertibilePdf(daConvertire ? -1 : 0);
                sd.setCodice("babel_suite_allegati_" + sd.getGuid());
                sd.setTipo("allegati");

                mongo.move(uuid, "/RS/Documenti/" + r.getAnnoRegistrazione() + "/" + r.getNumeroRegistrazione() + "/" + sd.getNome());

                String sql = "INSERT INTO gd.sotto_documenti "
                        + "(id_sottodocumento, id_gddoc, nome_sottodocumento, "
                        + "uuid_mongo_originale, data_ultima_modifica, "
                        + "guid_sottodocumento, dimensione_originale, "
                        + "convertibile_pdf, mimetype_file_originale, "
                        + "tipo_sottodocumento, codice_sottodocumento) "
                        + "VALUES(:id_sottodocumento, :id_gddoc, :nome_sottodocumento, "
                        + ":uuid_mongo_originale, now(), "
                        + ":guid_sottodocumento, :dimensione_originale, "
                        + ":convertibile_pdf, :mimetype_file_originale, "
                        + ":tipo_sottodocumento, :codice_sottodocumento) ";

                Query q = conn.createQuery(sql)
                        .addParameter("id_sottodocumento", sd.getId())
                        .addParameter("id_gddoc", sd.getIdGdDoc())
                        .addParameter("nome_sottodocumento", sd.getNome())
                        .addParameter("uuid_mongo_originale", sd.getUuidMongoOriginale())
                        .addParameter("guid_sottodocumento", sd.getGuid())
                        .addParameter("dimensione_originale", sd.getDimensioneOriginale())
                        .addParameter("convertibile_pdf", sd.getConvertibilePdf())
                        .addParameter("mimetype_file_originale", sd.getMimetypeFileOriginale())
                        .addParameter("tipo_sottodocumento", sd.getTipo())
                        .addParameter("codice_sottodocumento", sd.getCodice());

                log.info("query: " + q.toString());
                q.executeUpdate();
                idSottoDocumenti.add(sd.getId());
            }

            log.info("tutti gli allegati sono stati spostati");
        } catch (Throwable e) {
            log.error("errore inserimenti sotto documenti Raccolta Semplice", e);
            throw new Http500ResponseException("500", "errore inserimenti sotto documenti Raccolta Semplice", e);
        }
    }

    /**
     * creazione record di Raccolta Semplice
     *
     * @param conn
     * @return
     */
    public Integer createRaccolta(Connection conn, RaccoltaNew raccolta) throws Http500ResponseException {

        Integer res = null;
        String codice = String.format("%s/%d", raccolta.getNumeroRegistrazione(), raccolta.getAnnoRegistrazione());
        String sql = "INSERT INTO gd.raccolte "
                + "(id_gddoc, codice, applicazione_chiamante, "
                + "additional_data, creatore, "
                + "id_struttura_responsabile_internauta, id_struttura_responsabile_argo, "
                + "descrizione_struttura, stato, "
                + "create_time, tipo_documento, oggetto, id_gddoc_associato) "
                + "VALUES(:id_gddoc, :codice, :applicazione_chiamante, "
                + "cast(:additional_data AS jsonb), :creatore, "
                + ":id_struttura_responsabile_internauta, :id_struttura_responsabile_argo, "
                + ":descrizione_struttura, 'ATTIVO', "
                + "now(), :tipo_documento, :oggetto, :id_gddoc_associato) ";

        try {
            Query q = conn.createQuery(sql)
                    .addParameter("id_gddoc", raccolta.getIdGdDoc())
                    .addParameter("codice", codice)
                    .addParameter("applicazione_chiamante", raccolta.getApplicazioneChiamante())
                    .addParameter("additional_data", raccolta.getAdditionalData())
                    .addParameter("creatore", raccolta.getCreatore())
                    .addParameter("id_struttura_responsabile_internauta", raccolta.getIdStrutturaResponsabileInternauta())
                    .addParameter("id_struttura_responsabile_argo", raccolta.getIdStrutturaResponsabileArgo())
                    .addParameter("descrizione_struttura", raccolta.getDescrizioneStruttura())
                    .addParameter("tipo_documento", raccolta.getTipoDocumento())
                    .addParameter("oggetto", raccolta.getOggetto())
                    .addParameter("id_gddoc_associato", raccolta.getIdGddocAssociato() != null ? raccolta.getIdGddocAssociato() : null);

            log.info("query: " + q.toString());
            res = (int) q.executeUpdate().getKey();
        } catch (Throwable e) {
            log.error("errore creazione raccolta", e);
            throw new Http500ResponseException("500", "errore creazione raccolta", e);
        }
        return res;
    }

    public List<Map<String, String>> getIndeIdAndGuid(String codiceAzienda, Integer size) throws UnsupportedEncodingException, IOException, Http500ResponseException, Sql2oSelectException {

        String urlChiamata = "";
        //urlChiamata = "https://gdml.internal.ausl.bologna.it/Indeutilities/GetIndeId";

        String queryIndeIdUrl = "SELECT val_parametro from bds_tools.parametri_pubblici "
                + "WHERE nome_parametro = :nome_parametro";
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        try ( Connection conn = (Connection) dbConnection.open()) {
            urlChiamata = conn.createQuery(queryIndeIdUrl)
                    .addParameter("nome_parametro", "getIndeUrlServiceUri")
                    .executeAndFetchFirst(String.class);
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento dell'url di chiamata per generare gli ID del fascicolo", e);
        }

        if ("".equals(urlChiamata) || urlChiamata == null) {
            throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO);
        }

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("generateidnumber", size.toString());

        okhttp3.RequestBody formBody = formBuilder.build();

        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        Request requestg = new Request.Builder()
                .url(urlChiamata)
                .post(formBody)
                .build();

        Response responseg = client.newCall(requestg).execute();

        if (!responseg.isSuccessful()) {
            log.error("Errore nella chiamata alla Web-api");
            throw new Http500ResponseException("500", "Errore nella chiamata alla Web-api");
        }

        List<Map<String, String>> idGuidList = new ArrayList<>();

        idGuidList = objectMapper.readValue(responseg.body().string(), List.class);

        return idGuidList;
    }

    public String staccaNumero(Connection conn, Integer anno, String sequence, String codiceAzienda) throws Sql2oSelectException {
        String res = null;
        String sqlReg = "select lpad(cast(gd_sequences_nextval as varchar), 7, '0') from gd_tools.gd_sequences_nextval('rs', :anno)";
        try {
            Query q = conn.createQuery(sqlReg);
            if (anno != null) {
                q = q.addParameter("anno", anno);
            }
            List<String> result = q.executeAndFetch(String.class);
            if (result.size() == 0) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO, "Fascicolo non trovato!!");
            } else if (result.size() > 1) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.PIU_RISULTATI, "Trovati più fascicoli!");
            } else {
                res = result.get(0);
            }
        } catch (Throwable e) {
            throw new Sql2oSelectException("Errore nello staccamento del numero", e);
        }
        return res;
    }

    public void insertFascicoliGddocs(Connection conn, List<String> fascicoli, RaccoltaNew r) throws Http500ResponseException {

        try {
            for (String fascicolo : fascicoli) {

                Map<String, String> indeIdAndGuid = getIndeIdAndGuid(r.getCodiceAzienda(), 1).get(0);

                String sql = "INSERT INTO gd.fascicoli_gddocs "
                        + "(id_fascicolo_gddoc, id_gddoc, id_fascicolo, data_assegnazione) "
                        + "VALUES(:id_fascicolo_gddoc, :id_gddoc, :id_fascicolo, now()) ";

                Query q = conn.createQuery(sql)
                        .addParameter("id_fascicolo_gddoc", indeIdAndGuid.get("document_id"))
                        .addParameter("id_gddoc", r.getIdGdDoc())
                        .addParameter("id_fascicolo", fascicolo);

                log.info("query: " + q.toString());
                q.executeUpdate();
            }
            log.info("fascicolazione andata a buon fine");
        } catch (Throwable e) {
            log.error("errore fascicolazione di Raccolta Semplice", e);
            throw new Http500ResponseException("500", "errore fascicolazione di Raccolta Semplice", e);
        }
    }

    /**
     * *
     * restituisce Id del fascicolo su gd passando la numerazione gerarchica
     *
     * @param numGerarchica
     * @return Id del fascicolo se presente, NULL altrimenti
     */
    public String getIdFascicoliFromNumerazioneGerarchica(String codiceAzienda, String numGerarchica) throws Sql2oSelectException {

        String res = null;
        String query = "select id_fascicolo from gd.fascicoligd where numerazione_gerarchica = :numerazione_gerarchica";

        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);

        try ( Connection conn = (Connection) dbConnection.open()) {
            Query q = conn.createQuery(query);
            if (numGerarchica != null) {
                q = q.addParameter("numerazione_gerarchica", numGerarchica);
            }
            List<String> listaIdFascicoli = q.executeAndFetch(String.class);
            if (listaIdFascicoli.size() == 0) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO, "Fascicolo non trovato!!");
            } else if (listaIdFascicoli.size() > 1) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.PIU_RISULTATI, "Trovati più fascicoli!");
            } else {
                res = listaIdFascicoli.get(0);
            }
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento del fascicolo con numerazione gerarchica: " + numGerarchica, e);
        }

        return res;
    }

    public boolean createCoinvolto(Connection conn, PersonaRS p, Integer idRaccolta) throws Http500ResponseException {

        Integer idCoinvolto = null;
        Integer idCoinvoltoRaccolta = null;
        String sql = "INSERT INTO gd.coinvolti "
                + "(nome, cognome, ragione_sociale, descrizione, "
                + "cf, partitaiva, tipologia, id_contatto_internauta, "
                + "mail, telefono, via, civico, cap, comune, provincia, nazione) "
                + "VALUES(:nome, :cognome, :ragione_sociale, :descrizione, "
                + ":cf, :partitaiva, cast(:tipologia AS gd.tipo), :id_contatto_internauta, "
                + " :mail, :telefono, :via, :civico, :cap, :comune, :provincia, :nazione)";

        try {
            Query q = conn.createQuery(sql, true)
                    .addParameter("nome", p.getNome())
                    .addParameter("cognome", p.getCognome())
                    .addParameter("ragione_sociale", p.getRagioneSociale())
                    .addParameter("descrizione", p.getDescrizione())
                    .addParameter("cf", p.getCf())
                    .addParameter("partitaiva", p.getPartitaIva())
                    .addParameter("tipologia", p.getTipologia().name())
                    .addParameter("id_contatto_internauta", 1)
                    .addParameter("mail", p.getMail())
                    .addParameter("telefono", p.getTelefono())
                    .addParameter("via", p.getVia())
                    .addParameter("civico", p.getCivico())
                    .addParameter("cap", p.getCap())
                    .addParameter("comune", p.getComune())
                    .addParameter("provincia", p.getProvincia())
                    .addParameter("nazione", p.getNazione());

            log.info("query: " + q.toString());
            idCoinvolto = (int) q.executeUpdate().getKey();

            if (idCoinvolto != null) {
                String sqlCross = "INSERT INTO gd.coinvolti_raccolte "
                        + "(id_coinvolto, id_raccolta) "
                        + "VALUES(:id_coinvolto, :id_raccolta) ";

                Query qCross = conn.createQuery(sqlCross, true)
                        .addParameter("id_coinvolto", idCoinvolto)
                        .addParameter("id_raccolta", idRaccolta);

                log.info("queryCross: " + qCross.toString());
                idCoinvoltoRaccolta = (int) qCross.executeUpdate().getKey();

            }
        } catch (Throwable e) {
            log.error("errore nella creazione coinvolto", e);
            throw new Http500ResponseException("500", "errore nella creazione coinvolto", e);
        }
        return (idCoinvolto != null && idCoinvoltoRaccolta != null);
    }

    @RequestMapping(value = "downloadAllegato", method = RequestMethod.GET)
    public void downloadAttached(
            @RequestParam(value = "azienda", required = true) String codiceAzienda,
            @RequestParam(value = "id", required = true) String id,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException, UnsupportedEncodingException, BadParamsException {

        InputStream is;
        try {
            is = download(id, codiceAzienda);
            StreamUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error("Eccezione: ", e);
        }
    }

    @RequestMapping(value = "getTipologia", method = RequestMethod.GET)
    public List<String> getTipologia(@RequestParam(value = "azienda", required = true) String codiceAzienda,
            HttpServletResponse response,
            HttpServletRequest request) {
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        List<String> tipologie = new ArrayList<>();
        try ( Connection conn = (Connection) dbConnection.open()) {
            Query query = conn.createQuery("SELECT tipo from gd.tipologia_rs where data_disattivazione is null");
            tipologie = query.executeAndFetch(String.class);
            if (tipologie.isEmpty()) {
                log.error("Tipologie non trovate");
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("Causa errore: ", e);
            return null;
        }
        log.info("Tipologie ritornate correttamente");
        return tipologie;
    }

    public InputStream download(String id, String codiceAzienda) throws BadParamsException, FileNotFoundException, IOException {

        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        dbConnection.setDefaultColumnMappings(RaccoltaManager.mapSottoDocumenti());
        List<Sottodocumento> documento = new ArrayList<Sottodocumento>();

        try ( Connection conn = (Connection) dbConnection.open()) {

            Query query = conn.createQuery(RaccoltaManager.queryInfoSottoDocumenti(id));
            documento = (List<Sottodocumento>) query.executeAndFetch(Sottodocumento.class);
            String extension = documento.get(0).getEstensione();
            String fileName = documento.get(0).getNomeOriginale() + "." + extension;
            File file = new File(System.getProperty("java.io.tmpdir"), fileName);
            Integer idAzienda = postgresConnectionManager.getIdAzienda(codiceAzienda);
            MongoWrapper mongoWrapper = aziendeConnectionManager.getRepositoryWrapper(idAzienda);
            InputStream is = null;
            DataOutputStream dataOs = new DataOutputStream(new FileOutputStream(file));
            is = mongoWrapper.get(documento.get(0).getUuidMongo());
            if (is == null) {
                throw new MongoException("File non trovato");
            }
            //StreamUtils.copy(is, dataOs);
            return is;
        } catch (Exception e) {
            log.info("Errore nel reperimento del file: ", e);
        }
        return null;
    }

}
