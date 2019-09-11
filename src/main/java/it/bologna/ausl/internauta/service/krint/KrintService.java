
package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.logs.OperazioneVersionataKrinRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint.CodiceOperazione;
import it.bologna.ausl.model.entities.logs.OperazioneVersionataKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintInformazioniRealUser;
import it.bologna.ausl.model.entities.logs.projections.KrintInformazioniUtente;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author guido
 */
@Service
public class KrintService {
    
    @Autowired
    ProjectionFactory factory;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    protected CachedEntities cachedEntities;
    
    @Autowired
    protected HttpSessionData httpSessionData;
    
    @Autowired
    protected OperazioneVersionataKrinRepository operazioneVersionataKrinRepository;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InternautaBaseInterceptor.class);
    
    
    public void writeKrintRow(
            String idOggetto, 
            Krint.TipoOggettoKrint tipoOggetto, 
            String descrizioneOggetto, 
            String informazioniOggetto,
            String idOggettoContenitore,
            Krint.TipoOggettoKrint tipoOggettoContenitore, 
            String descrizioneOggettoContenitore, 
            String informazioniOggettocontenitore, 
            OperazioneKrint.CodiceOperazione codiceOperazione) {
        
        try {
            Utente utente = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getUser();

            Integer idSessione = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getIdSessionLog(); // TODO: mettere idSessione corretto
            KrintInformazioniUtente krintInformazioniUtente = factory.createProjection(KrintInformazioniUtente.class, utente);
            String jsonKrintInformazioniUtente = objectMapper.writeValueAsString(krintInformazioniUtente);                
            
            Krint krint = new Krint(idSessione, authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getApplicazione(), utente.getId(), utente.getIdPersona().getDescrizione(), jsonKrintInformazioniUtente);

            // recupero l'operazioneVersionata con quel codiceOperazione e con la versione più alta
            OperazioneKrint operazioneKrint = cachedEntities.getOperazioneKrint(codiceOperazione);
            OperazioneVersionataKrint operazioneVersionataKrint = 
                            operazioneVersionataKrinRepository.findFirstByIdOperazioneOrderByVersioneDesc(operazioneKrint).orElse(null);
            
            krint.setIdOggetto(idOggetto);
            krint.setTipoOggetto(tipoOggetto);
            krint.setInformazioniOggetto(informazioniOggetto);
            krint.setDescrizioneOggetto(descrizioneOggetto);
            krint.setIdOggettoContenitore(idOggettoContenitore);
            krint.setTipoOggettoContenitore(tipoOggettoContenitore);
            krint.setInformazioniOggettoContenitore(informazioniOggettocontenitore);
            krint.setDescrizioneOggettoContenitore(descrizioneOggettoContenitore);
            krint.setIdOperazioneVersionata(operazioneVersionataKrint);

            Utente utenteReale = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getUser().getUtenteReale();
            if(utenteReale != null){
                krint.setIdRealUser(utenteReale.getId());
                Persona personaReale = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getRealPerson();
                if(personaReale != null){                        
                    krint.setDescrizioneRealUser(personaReale.getDescrizione());
                }
                KrintInformazioniRealUser krintInformazioniRealUser = factory.createProjection(KrintInformazioniRealUser.class, utenteReale);
                String jsonKrintInformazioniRealUser = objectMapper.writeValueAsString(krintInformazioniRealUser);
                krint.setInformazioniRealUser(jsonKrintInformazioniRealUser);
            }
            
            List<Krint> krintList = (List<Krint>)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ROWS);
            if (krintList == null || krintList.isEmpty()) {
                krintList = new ArrayList();
            }
            krintList.add(krint);
            httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.KRINT_ROWS, krintList);
            
        }  catch (Exception ex) {
            // TODO: log
        } 
        
    }
    
    public void writeKrintError(Integer idOggetto, String functionName, CodiceOperazione codiceOperazione) {
        List<KrintError> krintErrorList = (List<KrintError>)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ERRORS);
        if (krintErrorList == null || krintErrorList.isEmpty()) {
            krintErrorList = new ArrayList();
        }
        
        KrintError krintError = new KrintError();
        try {
            Utente utente = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getUser();
            krintError.setIdUtente(utente.getId());
        } catch (Exception ex) {}
        try {
            Utente utenteReale = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getUser().getUtenteReale();
            krintError.setIdRealUser(utenteReale.getId());
        } catch (Exception ex) {}
        krintError.setIdOggetto(idOggetto);
        krintError.setFunctionName(functionName);
        krintError.setCodiceOperazione(codiceOperazione);

        krintErrorList.add(krintError);
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.KRINT_ERRORS, krintErrorList);
    }
}
