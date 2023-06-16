package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.logs.OperazioneVersionataKrinRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint.CodiceOperazione;
import it.bologna.ausl.model.entities.logs.OperazioneVersionataKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintInformazioniRealUser;
import it.bologna.ausl.model.entities.logs.projections.KrintInformazioniUtente;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author guido
 */
@Service
public class KrintService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProjectionFactory factory;

    @Autowired
    protected CachedEntities cachedEntities;

    @Autowired
    protected HttpSessionData httpSessionData;

    @Autowired
    protected NonCachedEntities nonCachedEntities;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    protected OperazioneVersionataKrinRepository operazioneVersionataKrinRepository;

    @Autowired
    UserInfoService infoService;

    private static final Logger LOGGER = LoggerFactory.getLogger(InternautaBaseInterceptor.class);

    public void writeKrintRow(
            String idOggetto,
            Krint.TipoOggettoKrint tipoOggetto,
            String descrizioneOggetto,
            HashMap<String, Object> informazioniOggetto,
            String idOggettoContenitore,
            Krint.TipoOggettoKrint tipoOggettoContenitore,
            String descrizioneOggettoContenitore,
            HashMap<String, Object> informazioniOggettocontenitore,
            OperazioneKrint.CodiceOperazione codiceOperazione) throws Exception {

        try {
            Utente utente = nonCachedEntities.getUtente(authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getUser().getId());

            Integer idSessione = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getIdSessionLog(); // TODO: mettere idSessione corretto
            KrintInformazioniUtente krintInformazioniUtente = factory.createProjection(KrintInformazioniUtente.class, utente);
            HashMap<String, Object> mapKrintInformazioniUtente = objectMapper.convertValue(utente, new TypeReference<HashMap<String, Object>>() {
            });
            Krint krint = new Krint(idSessione, authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getApplicazione(), utente.getId(), utente.getIdPersona().getDescrizione(), mapKrintInformazioniUtente);

            // recupero l'operazioneVersionata con quel codiceOperazione e con la versione più alta
            OperazioneKrint operazioneKrint = cachedEntities.getOperazioneKrint(codiceOperazione);
            OperazioneVersionataKrint operazioneVersionataKrint
                    = operazioneVersionataKrinRepository.findFirstByIdOperazioneIdOrderByVersioneDesc(operazioneKrint.getId()).orElse(null);

            krint.setIdOggetto(idOggetto);
            krint.setTipoOggetto(tipoOggetto);
            krint.setInformazioniOggetto(informazioniOggetto);
            krint.setDescrizioneOggetto(descrizioneOggetto);
            if (StringUtils.hasText(idOggettoContenitore)) {
                krint.setIdOggettoContenitore(idOggettoContenitore);
                krint.setTipoOggettoContenitore(tipoOggettoContenitore);
                krint.setDescrizioneOggettoContenitore(descrizioneOggettoContenitore);
            }

            krint.setInformazioniOggettoContenitore(informazioniOggettocontenitore);

            krint.setIdOperazioneVersionata(operazioneVersionataKrint);

            Utente utenteReale = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getRealUser() != null
                    ? nonCachedEntities.getUtente(authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getRealUser().getId())
                    : null;
            if (utenteReale != null) {
                krint.setIdRealUser(utenteReale.getId());
                Persona personaReale = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getRealPerson();
                if (personaReale != null) {
                    krint.setDescrizioneRealUser(personaReale.getDescrizione());
                }
                KrintInformazioniRealUser krintInformazioniRealUser = factory.createProjection(KrintInformazioniRealUser.class, utenteReale);
                HashMap<String, Object> mapKrintInformazioniRealUser = objectMapper.convertValue(krintInformazioniRealUser, new TypeReference<HashMap<String, Object>>() {
                });
                krint.setInformazioniRealUser(mapKrintInformazioniRealUser);
            }

            List<Krint> krintList = (List<Krint>) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ROWS);
            if (krintList == null || krintList.isEmpty()) {
                krintList = new ArrayList();
            }
            krintList.add(krint);
            httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.KRINT_ROWS, krintList);

        } catch (Exception ex) {
            LOGGER.error("errore nella writeKrintRow", ex);
            throw ex;
        }
    }

    /**
     * Magnigico commento
     *
     * @param idOggetto
     * @param functionName
     * @param codiceOperazione
     */
    public void writeKrintError(Integer idOggetto, String functionName, CodiceOperazione codiceOperazione) {
        List<KrintError> krintErrorList = (List<KrintError>) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ERRORS);
        if (krintErrorList == null || krintErrorList.isEmpty()) {
            krintErrorList = new ArrayList();
        }

        KrintError krintError = new KrintError();
        try {
            Utente utente = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getUser();
            krintError.setIdUtente(utente.getId());
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("errore nella writeKrintError", ex);
        }
        try {
            Utente utenteReale = authenticatedSessionDataBuilder.getAuthenticatedUserProperties().getRealUser();
            krintError.setIdRealUser(utenteReale.getId());
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("errore nella writeKrintError", ex);
        }
        krintError.setIdOggetto(idOggetto);
        krintError.setFunctionName(functionName);
        krintError.setCodiceOperazione(codiceOperazione);

        krintErrorList.add(krintError);
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.KRINT_ERRORS, krintErrorList);
    }
}
