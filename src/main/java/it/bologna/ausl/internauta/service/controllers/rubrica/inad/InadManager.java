package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.DettaglioContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.EmailRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.QDettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.QEmail;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 *
 * @author MicheleD'Onza
 */
@Component
public class InadManager {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InadManager.class);

    @Autowired
    private ParametriAziendeReader parametriAziendeReader;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContattoRepository contattoRepository;

    @Autowired
    private DettaglioContattoRepository dettaglioContattoRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private CachedEntities cachedEntities;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private HttpClientManager httpClientManager;
    
    private enum DomicilioDigitalePath{
        extract("/extract/"),
        verify("/verify/"),
        listDigitalAddress("/listDigitalAddress/"),
        listDigitalAddressState("/listDigitalAddress/state/"),
        listDigitalAddressResponse("/listDigitalAddress/response/");
        
        private final String pathInad;

        DomicilioDigitalePath(String pathInad) {
            this.pathInad = pathInad;
        }

        public String getDomicilioDigitalePath() {
            return pathInad;
        }
    }
    
    public List<Email> getAndSaveEmailDomicilioDigitale(Integer idContatto, Azienda azienda) throws AuthorizationUtilsException {

        Contatto contattoDaVerificare = contattoRepository.getById(idContatto);
        String codiceFiscaleContatto = contattoDaVerificare.getCodiceFiscale();

        if (codiceFiscaleContatto != null
                && !"".equals(codiceFiscaleContatto)
                && !contattoDaVerificare.getCategoria().equals(Contatto.CategoriaContatto.GRUPPO)
                && !contattoDaVerificare.getProvenienza().equals("INTERNO")
                && (contattoDaVerificare.getTipo() == null || Arrays.asList(new Contatto.TipoContatto[]{TipoContatto.FORNITORE, TipoContatto.FORNITORE, TipoContatto.VARIO}).contains(contattoDaVerificare.getTipo()))
                && !contattoDaVerificare.getProvenienza().equals("trigger_contatto_from_struttura")
                && !contattoDaVerificare.getProvenienza().equals("ribaltorg_strutture")
                && !contattoDaVerificare.getProvenienza().equals("ribaltorg_persone")) {

            List<Email> emailContattoDaRitornare = getDomicilioDigitaleFromCF(
                    azienda,
                    contattoDaVerificare,
                    dettaglioContattoRepository,
                    emailRepository);

            return emailContattoDaRitornare;
        }
        return null;
    }

    public Email getAlwaysAndSaveDomicilioDigitale(Integer idContatto) throws BlackBoxPermissionException, AuthorizationUtilsException {
        QEmail qEmail = QEmail.email1;
        QDettaglioContatto qDettaglioContatto = QDettaglioContatto.dettaglioContatto;
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        Email domicilioDigitale = jPAQueryFactory
                .select(qEmail)
                .from(qEmail).join(qDettaglioContatto).on(qEmail.idDettaglioContatto.id.eq(qDettaglioContatto.id))
                .where(qEmail.idContatto.id.eq(idContatto).and(qDettaglioContatto.domicilioDigitale.eq(true)))
                .fetchOne();
        if (domicilioDigitale == null) {
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
            Utente utente = authenticatedUserProperties.getUser();
            Azienda azienda = cachedEntities.getAzienda(utente.getIdAzienda().getId());
            List<Email> domiciliDigitali = getAndSaveEmailDomicilioDigitale(idContatto, azienda);
            if (domiciliDigitali != null && !domiciliDigitali.isEmpty()) {
                Optional<Email> domicilioDigitaleOp = domiciliDigitali.stream().filter(dd -> dd.getIdDettaglioContatto().getDomicilioDigitale().equals(true)).findFirst();
                if (domicilioDigitaleOp.isPresent()) {
                    domicilioDigitale = domicilioDigitaleOp.get();
                }
            }
        }
        return domicilioDigitale;
    }

    public List<Email> getDomicilioDigitaleFromCF(
            Azienda azienda,
            Contatto contattoDaVerificare,
            DettaglioContattoRepository dettaglioContattoRepository,
            EmailRepository emailRepository) throws AuthorizationUtilsException {
            //chiedo a inad i contatti del codice fiscale 
            
            InadExtractResponse responseObj = extract( azienda.getId(),contattoDaVerificare.getCodiceFiscale());
            
            updateOrCreateDettaglioContattoFromInadExtractResponse(responseObj);
            Contatto contattoVerificato = contattoRepository.findById(contattoDaVerificare.getId()).get();
        return contattoVerificato.getEmailList();
    }
    /**
     * 
     * funzione che dato un codice fiscale e l'azienda abilitata all'inad restituisce un domicilio digitale
     * @param idAzienda
     * @param codiceFiscale
     * @return ritorna un InadExtractResponse
     * @throws AuthorizationUtilsException 
     */
    public InadExtractResponse extract(Integer idAzienda, String codiceFiscale) throws AuthorizationUtilsException {

        try {
            //inizio a generare il clientAssertion
            InadParameters inadParameters = InadParameters.buildParameters(idAzienda, parametriAziendeReader, objectMapper);
            String clientAssertion = inadParameters.generateClientAssertion(idAzienda);
            //inizio a generare il jwt da mandare a 
            String tokenJWT = inadParameters.getToken(clientAssertion);
            URI uri = new URIBuilder(
                    inadParameters.getConnection().getUrlDocomicilioDigitale() + 
                    DomicilioDigitalePath.extract + 
                    codiceFiscale
                ).addParameter("practicalReference", "abc")
                .build();
            
            Request request = new Request.Builder()
                .get()
                .url(uri.toString())
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Bearer " + tokenJWT)
                .build();
            
            OkHttpClient httpClient = httpClientManager.getHttpClient();
            InadExtractResponse inadExtractResponse = new InadExtractResponse();
            inadExtractResponse.setCodiceFiscale(clientAssertion);
            Call call = httpClient.newCall(request);
            try (Response response = call.execute();) {
                int responseCode = response.code();
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    inadExtractResponse = objectMapper.readValue(body.byteStream(), InadExtractResponse.class);
                } else {
                    LOGGER.error("errore nella extract dei domicili digitali chiamata fallita");
                }
            }
            return inadExtractResponse;
        } catch (Exception ex) {
            LOGGER.error("errore nella extract dei domicili digitali", ex);
        }
        return null;
    }
    
    /**
     * 
     * Funzione che fa la richiesta per ottenere la lista di domicili digitali dai loro codifi fiscali
     * @param codiciFiscali per i quali si vuole avere il relativo domicilio digitale
     * @param idAzienda
     * @return null se la chiamata non da 202
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException
     * @throws java.security.KeyStoreException
     * @throws java.security.spec.InvalidKeySpecException
     * @throws java.security.cert.CertificateException
     * @throws java.security.UnrecoverableKeyException
     * @throws java.io.FileNotFoundException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.net.URISyntaxException
     * 
     */
    public InadListDigitalAddressResponse requestToExtractDomiciliDigitaliFromCodiciFiscali(List<String> codiciFiscali, Integer idAzienda) throws JsonProcessingException, AuthorizationUtilsException, KeyStoreException, InvalidKeySpecException, UnrecoverableKeyException, IOException, FileNotFoundException, CertificateException, NoSuchAlgorithmException, URISyntaxException{
        
        InadParameters inadParameters = InadParameters.buildParameters(idAzienda, parametriAziendeReader, objectMapper);
        String clientAssertion = inadParameters.generateClientAssertion(idAzienda);
        //inizio a generare il jwt da mandare a 
        String tokenJWT = inadParameters.getToken(clientAssertion);
        //FACCIO LA httpcall
        URI uri = new URIBuilder(inadParameters.getConnection().getUrlDocomicilioDigitale() + 
                    DomicilioDigitalePath.listDigitalAddress
                ).build();
        MediaType mediaType = MediaType.parse("application/json");
        Map<String,Object> bodyMap = new HashMap();
        bodyMap.put("codiciFiscali", codiciFiscali);
        bodyMap.put("praticalReference","abd");
        RequestBody body = RequestBody.create(mediaType,objectMapper.writeValueAsBytes(bodyMap));
        Request request = new Request.Builder()
                .post(body)
                .url(uri.toString())
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + tokenJWT)
                .build();
        
        OkHttpClient httpClient = httpClientManager.getHttpClient();
        Call call = httpClient.newCall(request);
        InadListDigitalAddressResponse inadListDigitalAddressResponse = null;
        try (Response response = call.execute();) {
            Integer responseCode = response.code();
            if (responseCode == 202) {
                inadListDigitalAddressResponse = objectMapper.readValue(response.body().byteStream(), InadListDigitalAddressResponse.class);
            } else {
                LOGGER.error("errore nella extract dei domicili digitali chiamata fallita");
            }
        }catch (Exception ex) {
            LOGGER.error("errore nella extract dei domicili digitali chiamata fallita",ex);
        }
        
        return inadListDigitalAddressResponse;
    } 
    
    /**
     * 
     * Funzione che restituisce lo stato di avanzamento della richiesta di estrazione di n domicili digitali
     * quando lo status è DISPONIBILE allora si puo procedere con l'effettiva estrazione
     * @param idInadListDigitalAddressResponse id ritornato dalla requestToExtractDomiciliDigitaliFromCodiciFiscali
     * @param idAzienda
     * @return
     * @throws JsonProcessingException
     * @throws AuthorizationUtilsException
     * @throws KeyStoreException
     * @throws InvalidKeySpecException
     * @throws UnrecoverableKeyException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws URISyntaxException 
     */
    public InadListDigitalAddressResponse statusRequestToExtractDomiciliDigitali(String idInadListDigitalAddressResponse, Integer idAzienda) throws JsonProcessingException, AuthorizationUtilsException, KeyStoreException, InvalidKeySpecException, UnrecoverableKeyException, IOException, FileNotFoundException, CertificateException, NoSuchAlgorithmException, URISyntaxException, InadException{
        InadParameters inadParameters = InadParameters.buildParameters(idAzienda, parametriAziendeReader, objectMapper);
        String clientAssertion = inadParameters.generateClientAssertion(idAzienda);
        //inizio a generare il jwt da mandare a 
        String tokenJWT = inadParameters.getToken(clientAssertion);
        //FACCIO LA httpcall
        
        URI uri = new URIBuilder(inadParameters.getConnection().getUrlDocomicilioDigitale() + 
                    DomicilioDigitalePath.listDigitalAddressState + idInadListDigitalAddressResponse
                ).build();
        
        Request request = new Request.Builder()
                .get()
                .url(uri.toString())
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Bearer " + tokenJWT)
                .build();
        
        OkHttpClient httpClient = httpClientManager.getHttpClient();
        Call call = httpClient.newCall(request);
        InadListDigitalAddressResponse inadListDigitalAddressResponse = null;
        try (Response response = call.execute();) {
            if (response.isSuccessful() || response.code() == 303) {
                inadListDigitalAddressResponse = objectMapper.readValue(response.body().byteStream(), InadListDigitalAddressResponse.class);
            } else {
                String errore = "errore nella extract dei domicili digitali chiamata fallita";
                LOGGER.error(errore);
                throw new InadException(errore);
            }
        }catch (Exception ex) {
            String errore = "errore nella extract dei domicili digitali chiamata fallita";
            LOGGER.error(errore,ex);
            throw new InadException(errore, ex);
        }
        return inadListDigitalAddressResponse;
    }
    
    /**
     * 
     * @param idInadListDigitalAddressResponse id ritornato dalla requestToExtractDomiciliDigitaliFromCodiciFiscali
     * @param idAzienda
     * @return lista di domicli digitali in formato InadExtractResponse
     * @throws JsonProcessingException
     * @throws AuthorizationUtilsException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws URISyntaxException 
     */
    public List<InadExtractResponse> extractMultiDomiciliDigitaliFromCodiciFiscali(String idInadListDigitalAddressResponse, Integer idAzienda) throws JsonProcessingException, AuthorizationUtilsException, KeyStoreException, UnrecoverableKeyException, IOException, FileNotFoundException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException {
        
        InadParameters inadParameters = InadParameters.buildParameters(idAzienda, parametriAziendeReader, objectMapper);
        String clientAssertion = inadParameters.generateClientAssertion(idAzienda);
        //inizio a generare il jwt da mandare a 
        String tokenJWT = inadParameters.getToken(clientAssertion);
        //FACCIO LA httpcall
        
        URI uri = new URIBuilder(inadParameters.getConnection().getUrlDocomicilioDigitale() + 
                    DomicilioDigitalePath.listDigitalAddressResponse + idInadListDigitalAddressResponse
                ).build();
        
        Request request = new Request.Builder()
                .get()
                .url(uri.toString())
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Bearer " + tokenJWT)
                .build();
        
        OkHttpClient httpClient = httpClientManager.getHttpClient();
        Call call = httpClient.newCall(request);
        List<InadExtractResponse> inadExtractResponseList = null;
        try (Response response = call.execute();) {
            if (response.isSuccessful()) {
                inadExtractResponseList = objectMapper.readValue(response.body().byteStream(), List.class);
            } else {
                LOGGER.error("errore nella extract dei domicili digitali chiamata fallita");
            }
        }catch (Exception ex) {
            LOGGER.error("errore nella extract dei domicili digitali chiamata fallita",ex);
        }
        
        return inadExtractResponseList;
    }
    
    /**
     * Funzione che dato un InadExtractResponse attraverso il codice fiscale 
     * aggiorna o crea il dettaglio contatto corrispondente
     * @param inadExtractResponse 
     */
    public void updateOrCreateDettaglioContattoFromInadExtractResponse(InadExtractResponse inadExtractResponse){
        if (inadExtractResponse != null) {
            List<DigitalAddress> digitalAddresses = inadExtractResponse.getDigitalAddresses();

            List<Email> emailContattoDaRitornare = new ArrayList<>();

            Contatto contatto = contattoRepository.findByCodiceFiscale(inadExtractResponse.getCodiceFiscale()).get(0);

            //se trovo dei domini digitali li metto dentro una lista di indirizzi che poi confronto
            //con i dettagli contatto già presenti sulla rubrica
            //se l'indirizzo esiste già, controllo che sia già segnato come contatto digitale
            //se l'indirizzo non esiste, creo il dettagli contatto giusto 
            if (contatto!= null) {
                String indirizzoDomicilioDigitale = digitalAddresses.get(0).getDigitalAddress();

                List<DettaglioContatto> dettagliContatto = contatto.getDettaglioContattoList();
                //l'unico caso in cui non è da aggiungere è se lo abbiamo già
                Boolean isIndirizzoDaAggiungere = true;

                if (!dettagliContatto.isEmpty()) {
                    for (DettaglioContatto dc : dettagliContatto) {

                        //controllo che sia già un domicilio digitale, sennò lo rendo tale
                        if (indirizzoDomicilioDigitale.equals(dc.getDescrizione())) {
                            isIndirizzoDaAggiungere = false;

                            if (!dc.getDomicilioDigitale()) {
                                dc.setDomicilioDigitale(Boolean.TRUE);
                                emailContattoDaRitornare.add(dc.getEmail());
                            }
                        } else {

                            //controllo non ci sia un altro dettaglio che è un domicilio digitale,
                            //nel caso lo setto come non dominio digitale
                            if (dc.getDomicilioDigitale()) {
                                dc.setDomicilioDigitale(Boolean.FALSE);
                                emailContattoDaRitornare.add(dc.getEmail());
                            }
                        }
                    }
                }

                if (!emailContattoDaRitornare.isEmpty()) {
                    for (Email emailContatto : emailContattoDaRitornare) {
                        dettaglioContattoRepository.save(emailContatto.getIdDettaglioContatto());
                    }
                }
                //aggiungo il dettaglio del domicilio digitale al contatto
                if (isIndirizzoDaAggiungere) {

                    Email emailDaAggiungere = new Email();
                    emailDaAggiungere.setEmail(indirizzoDomicilioDigitale);
                    emailDaAggiungere.setDescrizione(indirizzoDomicilioDigitale);
                    emailDaAggiungere.setIdContatto(contatto);
                    emailDaAggiungere.setPec(Boolean.TRUE);
                    emailDaAggiungere.setProvenienza("inad");
                    emailDaAggiungere.setPrincipale(Boolean.FALSE);

                    DettaglioContatto dettaglioDomicilioDigitale = new DettaglioContatto();
                    dettaglioDomicilioDigitale.setTipo(DettaglioContatto.TipoDettaglio.EMAIL);
                    dettaglioDomicilioDigitale.setDescrizione(indirizzoDomicilioDigitale);
                    dettaglioDomicilioDigitale.setIdContatto(contatto);
                    dettaglioDomicilioDigitale.setDomicilioDigitale(Boolean.TRUE);
                    dettaglioDomicilioDigitale.setEmail(emailDaAggiungere);
                    emailDaAggiungere.setIdDettaglioContatto(dettaglioDomicilioDigitale);

                    emailRepository.save(emailDaAggiungere);
                    dettaglioContattoRepository.save(dettaglioDomicilioDigitale);

                    emailContattoDaRitornare.add(emailDaAggiungere);
                }

            } else {
                for (DettaglioContatto dc : contatto.getDettaglioContattoList()) {
                    if (dc.getDomicilioDigitale()) {
                        dc.setDomicilioDigitale(Boolean.FALSE);
                        dettaglioContattoRepository.save(dc);
                        emailContattoDaRitornare.add(dc.getEmail());
                    }
                }

            }

        }
    }

}
