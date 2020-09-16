package it.bologna.ausl.internauta.service.interceptors.rubrica;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.rubrica.utils.similarity.SqlSimilarityResults;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QPec;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.Arrays;
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
@NextSdrInterceptor(name = "contatto-interceptor")
public class ContattoInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContattoInterceptor.class);

    @Autowired
    private ContattoRepository contattoRepository;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private KrintRubricaService krintRubricaService;

    @Autowired
    private PermissionManager permissionManager;

    @Autowired
    private ParametriAziende parametriAziende;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Class getTargetEntityClass() {
        return Contatto.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(user.getIdPersona());
        // List<Integer> idAziendePersona = aziendePersona.stream().map(a -> a.getId()).collect(Collectors.toList());

        // QUESTO E' IL FILTRO PER FAR SI CHE UNO VEDA SOLO I CONTATTI DELLE SUE AZIENDE
        BooleanExpression permessoAziendaleFilter = QContatto.contatto.idAziende.isNull().or(
                Expressions.booleanTemplate("tools.array_overlap({0}, tools.string_to_integer_array({1}, ','))=true",
                        QContatto.contatto.idAziende, org.apache.commons.lang3.StringUtils.join(aziendePersona.stream().map(a -> a.getId()).collect(Collectors.toList()), ",")
                ).or(
                        Expressions.booleanTemplate("cardinality({0}) = 0",
                                QContatto.contatto.idAziende
                        )
                ));
        initialPredicate = permessoAziendaleFilter.and(initialPredicate);

        // QUESTO E' IL FILTRO PER FAR SI CHE UNO VEDA SOLO I CONTATTI RISERVATI SU CUI HA UN PERMESSO UTENTE
        List<PermessoEntitaStoredProcedure> contattiWithStandardPermissions;
        try {
            contattiWithStandardPermissions = permissionManager.getPermissionsOfSubjectActualFromDate(
                    authenticatedSessionData.getPerson(),
                    null,
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.ACCESSO.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.RUBRICA.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.CONTATTO.toString()}), false, null);
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("Errore nel caricamento dei contatti accessibili dalla BlackBox", ex);
            throw new AbortLoadInterceptorException("Errore nel caricamento dei contatti accessibili dalla BlackBox", ex);
        }
        BooleanExpression contactFilter = QContatto.contatto.id.in(
                contattiWithStandardPermissions
                        .stream()
                        .map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList()))
                .or(QContatto.contatto.idPersonaCreazione.id.eq(user.getIdPersona().getId()))
                .or(
                        QContatto.contatto.riservato.eq(false));

        initialPredicate = contactFilter.and(initialPredicate);

        return initialPredicate;
    }

//    @Override
//    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
//        Contatto c = (Contatto) entity;
//        if (c.getCategoria().equals(Contatto.CategoriaContatto.GRUPPO)) {
//            this.httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.ContattoGruppoAppenaCreato, c);
//        }
//        
//        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
//    }
    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Contatto contatto = (Contatto) entity;
        if (KrintUtils.doIHaveToKrint(request)) {
            if (contatto.getCategoria().equals(Contatto.CategoriaContatto.GRUPPO)) {
                // TODO chiamare writeGroupCreation
                this.httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.ContattoGruppoAppenaCreato, contatto);
                krintRubricaService.writeGroupCreation(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_CREATION);
            } else if (contatto.getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                krintRubricaService.writeContactCreation(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_CREATION);
            }
        }
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Contatto contatto = (Contatto) entity;
        Contatto contattoOld = (Contatto) beforeUpdateEntity;
        boolean isEliminato = (contatto.getEliminato() && (contattoOld.getEliminato() == false));
        boolean isModificato = isContactModified(contatto, contattoOld);
        if (KrintUtils.doIHaveToKrint(request)) {
            if (contatto.getCategoria().equals(Contatto.CategoriaContatto.GRUPPO)) {
                if (isModificato) {
                    krintRubricaService.writeGroupUpdate(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_UPDATE);
                }
                if (isEliminato) {
                    krintRubricaService.writeGroupDelete(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_GROUP_DELETE);
                }
            } else if (contatto.getCategoria().equals(Contatto.CategoriaContatto.ESTERNO)) {
                if (isModificato) {
                    krintRubricaService.writeContactUpdate(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_UPDATE);
                }
                if (isEliminato) {
                    krintRubricaService.writeContactDelete(contatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DELETE);
                }
            }
        }

        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean isContactModified(Contatto contatto, Contatto contattoOld) {
        if (contatto.getDescrizione() == null ? contattoOld.getDescrizione() != null : !contatto.getDescrizione().equals(contattoOld.getDescrizione())) {
            return true;
        }
        if (contatto.getNome() == null ? contattoOld.getNome() != null : !contatto.getNome().equals(contattoOld.getNome())) {
            return true;
        }
        if (contatto.getCognome() == null ? contattoOld.getCognome() != null : !contatto.getCognome().equals(contattoOld.getCognome())) {
            return true;
        }
        if (contatto.getCodiceFiscale() == null ? contattoOld.getCodiceFiscale() != null : !contatto.getCodiceFiscale().equals(contattoOld.getCodiceFiscale())) {
            return true;
        }
        if (contatto.getPartitaIva() == null ? contattoOld.getPartitaIva() != null : !contatto.getPartitaIva().equals(contattoOld.getPartitaIva())) {
            return true;
        }
        if (contatto.getRagioneSociale() == null ? contattoOld.getRagioneSociale() != null : !contatto.getRagioneSociale().equals(contattoOld.getRagioneSociale())) {
            return true;
        }
        return false;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Contatto contatto = (Contatto) entity;
        try {
            AuthenticatedSessionData authenticatedUserProperties = getAuthenticatedUserProperties();
            String contattoString = objectMapper.writeValueAsString(contatto);
            List<Azienda> aziendePersona = userInfoService.getAziendePersona(authenticatedUserProperties.getPerson());
            List<Integer> collect = aziendePersona.stream().map(p -> p.getId()).collect(Collectors.toList());
            String idAziendeStr = UtilityFunctions.getArrayString(objectMapper, collect);
            String res = contattoRepository.getSimilarContacts(contattoString, idAziendeStr);

            List<ParametroAziende> parameters = parametriAziende.getParameters("protocontatti", new Integer[]{authenticatedUserProperties.getUser().getIdAzienda().getId()}, new String[]{Applicazione.Applicazioni.rubrica.toString()});
            contatto.setDaVerificare(false);
            if (parameters != null && !parameters.isEmpty() && parametriAziende.getValue(parameters.get(0), Boolean.class) == true) {
                contatto.setDaVerificare(true);
            } else {
                SqlSimilarityResults similarityResults = objectMapper.readValue(res, SqlSimilarityResults.class);
                similarityResults.filterByPermission(authenticatedUserProperties.getPerson(), permissionManager);
                if (similarityResults.similaritiesNumber() > 0) {
                    contatto.setDaVerificare(true);
                }
            }
            if (contatto.getIdUtenteCreazione() == null) {
                contatto.setIdUtenteCreazione(authenticatedUserProperties.getUser());
            }
            if (contatto.getIdPersonaCreazione() == null) {
                contatto.setIdPersonaCreazione(authenticatedUserProperties.getPerson());
            }
            Integer[] idAziende = userInfoService.getAziendePersona(authenticatedUserProperties.getPerson()).stream().map(a -> a.getId()).toArray(Integer[]::new);
            if (contatto.getIdAziende() == null) {
                contatto.setIdAziende(idAziende);
            }

        } catch (Exception ex) {
            throw new AbortSaveInterceptorException("fallito controllo similarità", ex);
        }

        return contatto;
    }

}
