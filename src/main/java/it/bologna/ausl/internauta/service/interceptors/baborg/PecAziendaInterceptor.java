package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.model.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "pecazienda-interceptor")
public class PecAziendaInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PecAziendaInterceptor.class);

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    private PermissionManager permissionManager;

    @Autowired
    private StrutturaRepository strutturaRepository;

    @Override
    public Class getTargetEntityClass() {
        return PecAzienda.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        // TODO: Se non sono ne CA ne CI posso vedere le associazioni pec-aziende?
        return super.beforeSelectQueryInterceptor(initialPredicate, additionalData, request, mainEntity, projectionClass);
    }

    /*
     * Condizioni per l'INSERT.
     * Il CI può inserire qualsiasi associazioni.
     * Il CA può inserire solo associazioni con la/e sua/e azienda/e.
     */
    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        LOGGER.info("in: beforeCreateEntityInterceptor di PecAzienda");
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();

        if (!userInfoService.isCI(authenticatedSessionData.getUser())) {
            Persona persona = personaRepository.getOne(authenticatedSessionData.getPerson().getId());
            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(persona).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());

            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                // Non sono ne CA ne CI fermo tutto.
                throw new AbortSaveInterceptorException();
            } else {
                PecAzienda pa = (PecAzienda) entity;

                if (!idAziendeCA.contains(pa.getIdAzienda().getId())) {
                    // Pur essendo CA non lo sono di questa azienda.
                    throw new AbortSaveInterceptorException();
                }
            }
        }

        return entity;
    }

    /*
     * Condizioni per l'UPDATE.
     * L'UPDATE è permesso solo se è un finto UDPDATE.
     * L'azienda e la PEC dell'entity devono essere le stesse del beforeUpdateEntity.
     */
    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        PecAzienda pecDopo = (PecAzienda) entity;
        PecAzienda pecPrima;
        try {
            pecPrima = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, PecAzienda.class);

        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }
        if (!(pecDopo.getIdAzienda().getId().equals(pecPrima.getIdAzienda().getId())) || !(pecDopo.getIdPec().getId().equals(pecPrima.getIdPec().getId()))) {
            throw new AbortSaveInterceptorException();
        }

        return entity;
    }

    /*
     * Condizioni per la DELETE.
     * Il CI può cancellare qualsiasi associazione.
     * Il CA può cancellare solo associazioni con la/e sua/e azienda/e.
     */
    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        
        PecAzienda pa = (PecAzienda) entity;

        if (!userInfoService.isCI(authenticatedSessionData.getUser())) {
            Persona persona = personaRepository.getOne(authenticatedSessionData.getPerson().getId());
            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(persona).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());

            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                // Non sono ne CA ne CI fermo tutto.
                throw new AbortSaveInterceptorException();
            } else {
                pa = (PecAzienda) entity;

                if (!idAziendeCA.contains(pa.getIdAzienda().getId())) {
                    // Pur essendo CA non lo sono di questa azienda.
                    throw new AbortSaveInterceptorException();
                }
            }
        }

        // Sto togliendo una associazione pec-azienda.
        // Devo quindi spegnere i permessi pec-struttura (SPEDISCE, SPEDISCE_PRINCIPALE) delle strutture di quella azienda
        // E devo spegnere i permessi pec-persona (ELIMINA, RISPONDE, LEGGE) delle persone che hanno un utente attivo in quella azienda
        // Per spegnere i permessi devo prima chiederli alla blackbox.
        // La funzione che mi interessa è quella che si chiama getSubjectWithPermissionOnObject
        // Una volta che ho i permessi li ciclo e li spengo (il che significa toglierli dall'array) e li rimando alla blackbox la quale capirà di doverli spegnere
        try {
            // Esempio di richiesta dei permessi pec-struttura di una certa pec
            List<PermessoEntitaStoredProcedure> oggettoneListStrutturePec = permissionManager.getSubjectsWithPermissionsOnObject(
                    Arrays.asList(new Pec[]{pa.getIdPec()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.SPEDISCE.toString(), InternautaConstants.Permessi.Predicati.SPEDISCE_PRINCIPALE.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}),
                    false,false);

            // Come detto, qui ciclo il permessone e capisco quali permessi eliminare in modo che poi vengano spoenti dalla balckbox
            if (oggettoneListStrutturePec != null) {
                for (int i = 0; i < oggettoneListStrutturePec.size(); i++) {
                    Integer idStruttura = oggettoneListStrutturePec.get(i).getSoggetto().getIdProvenienza();
                    Azienda aziendaStruttura = strutturaRepository.getById(idStruttura).getIdAzienda();
                    if (pa.getIdAzienda() == aziendaStruttura) {
                        List<CategoriaPermessiStoredProcedure> categorie = oggettoneListStrutturePec.get(i).getCategorie();
                        for (int a = 0; a < categorie.size(); a++) {
                            List<PermessoStoredProcedure> listaPermessiVuota = Collections.emptyList();
                            categorie.get(a).setPermessi(listaPermessiVuota);
                            oggettoneListStrutturePec.get(i).setCategorie(categorie);
                        }
                    }
                }
                permissionManager.managePermissions(oggettoneListStrutturePec, null);
            }
            //faccio la stessa cosa con i gestori di quell'azienda
            List<PermessoEntitaStoredProcedure> oggettoneListGestoriPec = permissionManager.getSubjectsWithPermissionsOnObject(
                    Arrays.asList(new Pec[]{pa.getIdPec()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.LEGGE.toString(), InternautaConstants.Permessi.Predicati.RISPONDE.toString(), InternautaConstants.Permessi.Predicati.ELIMINA.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}),
                    false,false);

            if (oggettoneListGestoriPec != null) {
                for (int i = 0; i < oggettoneListGestoriPec.size(); i++) {
                    Integer idPersona = oggettoneListGestoriPec.get(i).getSoggetto().getIdProvenienza();
                    if (userInfoService.getAziendePersona(personaRepository.getById(idPersona)).contains(pa.getIdAzienda())) {
                        List<CategoriaPermessiStoredProcedure> categorie = oggettoneListGestoriPec.get(i).getCategorie();
                        for (int a = 0; a < categorie.size(); a++) {
                            List<PermessoStoredProcedure> listaPermessiVuota = Collections.emptyList();
                            categorie.get(a).setPermessi(listaPermessiVuota);
                            oggettoneListGestoriPec.get(i).setCategorie(categorie);
                        }
                    }
                }
                permissionManager.managePermissions(oggettoneListGestoriPec, null);
            }
            // Esempio di come mando i permessi alla blackbox perché li gestisca/spenga

        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
            throw new AbortSaveInterceptorException("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);
        }

    }
}
