package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

    public List<Email> getAndSaveEmailDomicilioDigitale(Integer idContatto, Azienda azienda) {

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

            List<Email> emailContattoDaRitornare = InadManager.getDomicilioDigitaleFromCF(
                    contattoDaVerificare,
                    dettaglioContattoRepository,
                    emailRepository);

            return emailContattoDaRitornare;
        }
        return null;
    }

    public Email getAlwaysAndSaveDomicilioDigitale(Integer idContatto) throws BlackBoxPermissionException {
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

    public static List<Email> getDomicilioDigitaleFromCF(
            Contatto contattoDaVerificare,
            DettaglioContattoRepository dettaglioContattoRepository,
            EmailRepository emailRepository) {
        //chiedo a inad i contatti del codice fiscale 
//            InadExtractResponse responseObj = InadManager.extract(codiceFiscaleContatto, azienda.getId());

//          finchè non funziona la chiamata ad inad ne faccio una finta
//        InadExtractResponse responseObj = new InadExtractResponse();
//        DigitalAddress digitalAddress = new DigitalAddress();
//        UsageInfo usageInfo = new UsageInfo();
//        digitalAddress.setDigitalAddress("pippopiudipippobaudo@pec.it");
//        digitalAddress.setPracticedProfession("POCOPROFESSIONISTAMOLTOLIBERO");
//        usageInfo.setDateEndValidity(ZonedDateTime.now());
//        usageInfo.setMotivation("CESSAZIONE_VOLONTARIA");
//        digitalAddress.setUsageInfo(usageInfo);
//        responseObj.setCodiceFiscale("LZZCHR97M43A944X");
//        List<DigitalAddress> digitalAddresses2 = new ArrayList<>();
//        digitalAddresses2.add(digitalAddress);
//        responseObj.setDigitalAddresses(digitalAddresses2);

//        List<DigitalAddress> digitalAddresses = responseObj.getDigitalAddresses();

        List<Email> emailContattoDaRitornare = new ArrayList<>();

        //se trovo dei domini digitali li metto dentro una lista di indirizzi che poi confronto
        //con i dettagli contatto già presenti sulla rubrica
        //se l'indirizzo esiste già, controllo che sia già segnato come contatto digitale
        //se l'indirizzo non esiste, creo il dettagli contatto giusto 
//        if (!digitalAddresses.isEmpty()) {
//            String indirizzoDomicilioDigitale = digitalAddresses.get(0).getDigitalAddress();
//
//            List<DettaglioContatto> dettagliContatto = contattoDaVerificare.getDettaglioContattoList();
//            //l'unico caso in cui non è da aggiungere è se lo abbiamo già
//            Boolean isIndirizzoDaAggiungere = true;
//
//            if (!dettagliContatto.isEmpty()) {
//                for (DettaglioContatto dc : dettagliContatto) {
//
//                    //controllo che sia già un domicilio digitale, sennò lo rendo tale
//                    if (indirizzoDomicilioDigitale.equals(dc.getDescrizione())) {
//                        isIndirizzoDaAggiungere = false;
//
//                        if (!dc.getDomicilioDigitale()) {
//                            dc.setDomicilioDigitale(Boolean.TRUE);
//                            emailContattoDaRitornare.add(dc.getEmail());
//                        }
//                    } else {
//
//                        //controllo non ci sia un altro dettaglio che è un domicilio digitale,
//                        //nel caso lo setto come non dominio digitale
//                        if (dc.getDomicilioDigitale()) {
//                            dc.setDomicilioDigitale(Boolean.FALSE);
//                            emailContattoDaRitornare.add(dc.getEmail());
//                        }
//                    }
//                }
//            }
//
//            if (!emailContattoDaRitornare.isEmpty()) {
//                for (Email emailContatto : emailContattoDaRitornare) {
//                    dettaglioContattoRepository.save(emailContatto.getIdDettaglioContatto());
//                }
//            }
//            //aggiungo il dettaglio del domicilio digitale al contatto
//            if (isIndirizzoDaAggiungere) {
//
//                Email emailDaAggiungere = new Email();
//                emailDaAggiungere.setEmail(indirizzoDomicilioDigitale);
//                emailDaAggiungere.setDescrizione(indirizzoDomicilioDigitale);
//                emailDaAggiungere.setIdContatto(contattoDaVerificare);
//                emailDaAggiungere.setPec(Boolean.TRUE);
//                emailDaAggiungere.setProvenienza("inad");
//                emailDaAggiungere.setPrincipale(Boolean.FALSE);
//
//                DettaglioContatto dettaglioDomicilioDigitale = new DettaglioContatto();
//                dettaglioDomicilioDigitale.setTipo(DettaglioContatto.TipoDettaglio.EMAIL);
//                dettaglioDomicilioDigitale.setDescrizione(indirizzoDomicilioDigitale);
//                dettaglioDomicilioDigitale.setIdContatto(contattoDaVerificare);
//                dettaglioDomicilioDigitale.setDomicilioDigitale(Boolean.TRUE);
//                dettaglioDomicilioDigitale.setEmail(emailDaAggiungere);
//                emailDaAggiungere.setIdDettaglioContatto(dettaglioDomicilioDigitale);
//
//                emailRepository.save(emailDaAggiungere);
//                dettaglioContattoRepository.save(dettaglioDomicilioDigitale);
//
//                emailContattoDaRitornare.add(emailDaAggiungere);
//            }
//
//        } else {
//            for (DettaglioContatto dc : contattoDaVerificare.getDettaglioContattoList()) {
//                if (dc.getDomicilioDigitale()) {
//                    dc.setDomicilioDigitale(Boolean.FALSE);
//                    dettaglioContattoRepository.save(dc);
//                    emailContattoDaRitornare.add(dc.getEmail());
//                }
//            }
//
//        }
        return emailContattoDaRitornare;
    }

    public InadExtractResponse extract(Integer idAzienda, String codiceFiscale) throws AuthorizationUtilsException {

        try {
            //inizio a generare il clientAssertion
            InadParameters inadParameters = InadParameters.build(idAzienda, parametriAziendeReader, objectMapper);
            String clientAssertion = inadParameters.generateClientAssertion(idAzienda);
            //inizio a generare il jwt da mandare a 
            String tokenJWT = inadParameters.getToken(clientAssertion);

            InadExtractResponse inadExtractResponse = new InadExtractResponse();
            inadExtractResponse.setCodiceFiscale(clientAssertion);
            return inadExtractResponse;
        } catch (Exception ex) {
            Logger.getLogger(InadManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
