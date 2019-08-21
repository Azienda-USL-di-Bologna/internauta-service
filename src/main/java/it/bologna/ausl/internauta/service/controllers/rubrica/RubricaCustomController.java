package it.bologna.ausl.internauta.service.controllers.rubrica;

import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.authorization.utils.UtenteProcton;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.configuration.utils.RubricaRestClientConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.rubrica.maven.client.RestClient;
import it.bologna.ausl.rubrica.maven.client.RestClientException;
import it.bologna.ausl.rubrica.maven.resources.EmailResource;
import it.bologna.ausl.rubrica.maven.resources.FullContactResource;
import it.nextsw.common.utils.CommonUtils;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${rubrica.mapping.url.root}")
public class RubricaCustomController implements ControllerHandledExceptions {

    private static final Logger LOG = LoggerFactory.getLogger(RubricaCustomController.class);
    
    @Autowired
    CommonUtils commonUtils;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    PostgresConnectionManager postgresConnectionManager;
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    RubricaRestClientConnectionManager rubricaRestClientConnectionManager;
    
    /**
     * Effettua la ricerca sulla rubrica locale dell'utente indicato sull'azienda indicata
     * @param toSearch
     * @param azienda
     * @param idUtente
     * @param idStruttura
     * @return
     * @throws RestClientException 
     */
//    public List<FullContactResource> searchContact(String toSearch, Azienda azienda, String idUtente, String idStruttura) throws RestClientException {
//        RestClient restClient = rubricaRestClientConnectionManager.getConnection(azienda.getId());
//        return restClient.searchContact(toSearch, idUtente, idStruttura, false);
//    }
    
    /**
     * Questa funzione cerca nelle rubriche dei db argo usando principalmente la rubricarest.
     * La fuznione è specifica per la ricerca della mail.
     * Restituirà un oggetto composto da descrizione contatto e sua email.
     * @param toSearch
     * @param request
     * @return
     * @throws EmlHandlerException
     * @throws UnsupportedEncodingException
     * @throws Http500ResponseException
     * @throws Http404ResponseException
     * @throws RestClientException 
     */
    @RequestMapping(value = "searchEmailContact", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchEmailContact(
            @RequestParam("toSearch") String toSearch,
            HttpServletRequest request
    ) throws EmlHandlerException, UnsupportedEncodingException, Http500ResponseException, Http404ResponseException, RestClientException {
        // Prendo l'azienda da cui viene la richiesta
        String path = commonUtils.getHostname(request);
        Azienda azienda = userInfoService.loadAziendaByPath(path);
                
        // Prendo le informazioni che mi servono per chiamare la rubrica
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        UtenteProcton utenteProcton = (UtenteProcton) userInfoService.getUtenteProcton(utente.getIdPersona().getId(), azienda.getCodice());
        
        // Faccio la chiamata alla rubrica
        RestClient restClient = rubricaRestClientConnectionManager.getConnection(azienda.getId());
        List<FullContactResource> searchContact = restClient.searchContact(toSearch, utenteProcton.getIdUtente(), utenteProcton.getIdStruttura(), false);
        
        List<FullContactResource> contattiTrovati = new ArrayList();
        Set<Integer> idContatti = new HashSet();
        
        searchContact.forEach((c) -> {
            List<EmailResource> emails = c.getEmails();
            if (emails != null && !emails.isEmpty()) {
                contattiTrovati.add(c);
                idContatti.add(c.getContatto().getId());
            }
        });
        
        // Devo aggiungere anche una ricerca diretta sul db, nel caso che la stringa cercata sia proprio la pec
        List<Integer> contatti = searchContactByEmail(utenteProcton.getIdUtente(), toSearch, azienda.getCodice());
        if (!contatti.isEmpty()) {
            for (Integer contatto : contatti) {
                // Controllo di non aver già aggiunto questo contatto
                if (!idContatti.contains(contatto)) {
                    idContatti.add(contatto);
                    FullContactResource contact = restClient.getContact(contatto);
                    if (contact != null) {
                        contattiTrovati.add(contact);
                    }
                }
            }
        }
        
        // Torno il risultato della ricerca
        return new ResponseEntity(contattiTrovati, HttpStatus.OK);
    }
  
    
    /**
     * Questa funzione si collega al db locale argo per fare una ricerca tra le mail. Query copiata da inde e non controllata.
     * @param idUtente
     * @param toSearch
     * @param codiceAzienda
     * @return 
     */
    private List<Integer> searchContactByEmail(String idUtente, String toSearch, String codiceAzienda) throws Http500ResponseException {
        String query = "with mia_rubrica as\n" +
            "(\n" +
            "select r.id, r.idrubricasuperiore, rs.idstruttura as id_struttura\n" +
            "from rubrica.rubrica r\n" +
            "join rubrica.rubrica rs on rs.id = r.idrubricasuperiore\n" +
            "where r.nome = :idUtente\n" +
            "),\n" +
            "rubriche_della_mia_struttura as\n" +
            "(\n" +
            "select id\n" +
            "from rubrica.rubrica\n" +
            "where nome in (\n" +
            "	select id_utente from procton.utenti where id_struttura = (select id_struttura from mia_rubrica)\n" +
            "	union\n" +
            "	select id_utente from procton.appartenenze_funzionali where id_struttura = (select id_struttura from mia_rubrica)\n" +
            ")\n" +
            "), \n" +
            "rubriche_pubbliche as\n" +
            "(\n" +
            "select id\n" +
            "from rubrica.rubrica\n" +
            "where tipo = 'pubblica'\n" +
            ")\n" +
            "select idcontatto\n" +
            "from rubrica.email e\n" +
            "join rubrica.contatto c on c.id = e.idcontatto\n" +
            "where email ilike :toSearch\n" +
            "and c.idrubrica = (select id from mia_rubrica)\n" +
            "--limit 50\n" +
            "union\n" +
            "select idcontatto\n" +
            "from rubrica.email e\n" +
            "join rubrica.contatto c on c.id = e.idcontatto\n" +
            "where email ilike :toSearch\n" +
            "and c.privato = 'false'\n" +
            "and c.idrubrica in (select id from rubriche_della_mia_struttura)\n" +
            "--limit 50\n" +
            "union\n" +
            "select idcontatto\n" +
            "from rubrica.email e\n" +
            "join rubrica.contatto c on c.id = e.idcontatto\n" +
            "where email ilike :toSearch\n" +
            "and c.idrubrica in (select id from rubriche_pubbliche)\n" +
            "limit 50";
        
        // Prendo la connessione dal connection manager
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        List<Integer> contatti;
        
        try (Connection conn = (Connection) dbConnection.open()) {
            Query addParameter = conn.createQuery(query)
                    .addParameter("idUtente", idUtente)
                    .addParameter("toSearch", toSearch);
            LOG.info("esecuzione query: " + addParameter.toString());
            contatti = addParameter.executeAndFetch(Integer.class);
        } catch (Exception e) {
            LOG.error("errore nell'esecuzione della query", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query");
        }
        return contatti;
    }
    
}


