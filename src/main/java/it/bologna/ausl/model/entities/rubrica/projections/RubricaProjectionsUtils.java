package it.bologna.ausl.model.entities.rubrica.projections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadManager;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadParameters;
import it.bologna.ausl.internauta.service.repositories.rubrica.DettaglioContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.EmailRepository;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.persona.PersonaWithUtentiAndStruttureAndAfferenzeCustom;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaContatto;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaDettaglioContatto;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaGruppoContatto;
import it.bologna.ausl.model.entities.permessi.Entita;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithPlainFields;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class RubricaProjectionsUtils {
    
    @Autowired
    protected ProjectionFactory projectionFactory;
    
    @Autowired
    private StrutturaRepository strutturaRepository;
    
    @Autowired
    private PermissionManager permissionManager;
    
    @Autowired
    private PersonaRepository personaRepository;
    
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    private DettaglioContattoRepository dettaglioContattoRepository;
    
    @Autowired
    private EmailRepository emailRepository;    
    
    @Autowired
    private InadManager inadManager;
    
    @Autowired
    private InadParameters inadParameters;
    
    @Autowired
    private ParametriAziendeReader parametriAziendeReader;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RubricaProjectionsUtils.class);
    
    public String getElencoCodiciAziendeAttualiPersona(Persona persona) {
        String codiciAziende = "";
        List<Utente> utenteList = persona.getUtenteList();
        if (utenteList != null) {
            for (Utente utente : utenteList) {
                utente = utenteRepository.findById(utente.getId()).get();
                if (utente.getAttivo()) {
                    Azienda azienda = aziendaRepository.findById(utente.getIdAzienda().getId()).get();
                    codiciAziende = codiciAziende + (codiciAziende.length() == 0 ? "" : ", ") + azienda.getNome();
                }
            }
        }
        return codiciAziende;
    }
    
    public List<PermessoEntitaStoredProcedure> getPermessiContatto(Contatto contatto) throws BlackBoxPermissionException {

        List<String> predicati = new ArrayList<>();
        predicati.add("ACCESSO");
        List<String> ambiti = new ArrayList<>();
        ambiti.add("RUBRICA");
        List<String> tipi = new ArrayList<>();
        tipi.add("CONTATTO");

        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = new ArrayList<>();
        subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(contatto, predicati, ambiti, tipi, Boolean.FALSE,Boolean.FALSE);
        if (subjectsWithPermissionsOnObject != null) {
            for (PermessoEntitaStoredProcedure permessoEntitaStoredProcedure : subjectsWithPermissionsOnObject) {
                if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.strutture.toString())) {
                    Struttura strutturaSoggetto = strutturaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(strutturaSoggetto.getNome()
                            + " [ " + strutturaSoggetto.getIdAzienda().getNome() + (strutturaSoggetto.getCodice() != null ? " - " + strutturaSoggetto.getCodice() : "") + " ]");
                    permessoEntitaStoredProcedure.getSoggetto().setAdditionalData(
                            strutturaRepository.getCountUtentiStruttura(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza())
                    );
                } else if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.persone.toString())) {
                    Persona personaSoggetto = personaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(personaSoggetto.getDescrizione() + " [ " + getElencoCodiciAziendeAttualiPersona(personaSoggetto) + " ]");
                }
            }
        }

        return subjectsWithPermissionsOnObject;
    }
    
    public List<CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda(Contatto contatto) {
        List<CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda> res = null;
        List<DettaglioContatto> dettaglioContattoList = contatto.getDettaglioContattoList();
        if (dettaglioContattoList != null && !dettaglioContattoList.isEmpty()) {
            res = dettaglioContattoList.stream().filter(dettaglioContatto -> dettaglioContatto.getEliminato() == false).map(dettaglioContatto -> {
                return projectionFactory.createProjection(CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda.class, dettaglioContatto);                
            }).collect(Collectors.toList());
        }
        return res;
    }
    
    public CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAziendaByGruppoContatto(GruppiContatti gruppoContatto) {
        DettaglioContatto dettaglioContatto = gruppoContatto.getIdDettaglioContatto();
        if (dettaglioContatto != null) {
            return projectionFactory.createProjection(CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda.class, dettaglioContatto);
        }
        return null;
    }
    
    public PersonaWithUtentiAndStruttureAndAfferenzeCustom getPersonaWithUtentiAndStruttureAndAfferenzeCustom(Contatto contatto) {
        PersonaWithUtentiAndStruttureAndAfferenzeCustom res = null;
        Persona idPersona = contatto.getIdPersona();
        if (idPersona != null) {
            res = projectionFactory.createProjection(PersonaWithUtentiAndStruttureAndAfferenzeCustom.class, idPersona);
        }
        return res;
    }
    
    public List<KrintRubricaGruppoContatto> getCustomKrintContattiDelGruppoList(Contatto gruppo) {
        List<GruppiContatti> contattiDelGruppoList = gruppo.getContattiDelGruppoList();
        List<KrintRubricaGruppoContatto> res = null;
        if (contattiDelGruppoList != null && !contattiDelGruppoList.isEmpty()) {
            res = contattiDelGruppoList.stream().map(gruppoContatto -> {
                return projectionFactory.createProjection(KrintRubricaGruppoContatto.class, gruppoContatto);
            }).collect(Collectors.toList());
        }
        return res;
    }
    
    public KrintRubricaContatto getCustomKrintContatto(Contatto contatto) {
        if (contatto != null) {
            return projectionFactory.createNullableProjection(KrintRubricaContatto.class, contatto);
        }
        return null;
    }
    
    public KrintRubricaDettaglioContatto getCustomKrintDettaglioContatto(DettaglioContatto dettaglioContatto) {
        if (dettaglioContatto != null) {
            return projectionFactory.createNullableProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
        }
        return null;
    }
    
    public CustomContattoWithIdStrutturaAndIdPersona getContattoWithIdStrutturaAndIdPersonaByGruppoContatto(GruppiContatti gruppoContatto) {
        Contatto idContatto = gruppoContatto.getIdContatto();
        if (idContatto != null) {
            return projectionFactory.createProjection(CustomContattoWithIdStrutturaAndIdPersona.class, idContatto);
        }
        return null;
    }
    
    public List<DettaglioContattoWithPlainFields> getDettaglioContattoListWithDomicilioDigitale(Contatto contatto) throws AuthorizationUtilsException {
        AuthenticatedSessionData authenticatedUserProperties = null;
        try{
            authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("errore nel reperimento delle AuthenticatedUserProperties", ex);
        }
        if (authenticatedUserProperties == null){
            return null;
        }else {
            
            Boolean controllaDomiciliDigitali = false;
            
            try {
                InadParameters buildParameters = inadParameters.buildParameters(authenticatedUserProperties.getUser().getIdAzienda().getId(), parametriAziendeReader, objectMapper);
                controllaDomiciliDigitali = buildParameters.getEnabled();
            } catch (JsonProcessingException ex) {
                LOGGER.error("errore nel reperimento dei parametri inad",ex);
            }
            
            if (controllaDomiciliDigitali){
                inadManager.getDomicilioDigitaleFromCF(
                        authenticatedUserProperties.getUser().getIdAzienda(),
                        contatto, 
                        dettaglioContattoRepository, 
                        emailRepository);
            }
            
            List<DettaglioContatto> dettagliContattiList = contatto.getDettaglioContattoList();
            List<DettaglioContattoWithPlainFields> dettaglioContattoWithPlainFieldsList = dettagliContattiList.stream().map(dc -> projectionFactory.createProjection(DettaglioContattoWithPlainFields.class, dc)).collect(Collectors.toList());    
            return dettaglioContattoWithPlainFieldsList;
        }
        
    }
}
