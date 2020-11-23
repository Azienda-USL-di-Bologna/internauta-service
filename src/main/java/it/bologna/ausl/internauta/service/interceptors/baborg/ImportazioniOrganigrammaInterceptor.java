package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.ribaltoneutils.RibaltoneDaLanciareRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.ImportazioniOrganigramma;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QImportazioniOrganigramma;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
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
@NextSdrInterceptor(name = "importazioniorganigramma-interceptor")
public class ImportazioniOrganigrammaInterceptor extends InternautaBaseInterceptor{
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportazioniOrganigrammaInterceptor.class);
    
    @Autowired
    CachedEntities cachedEntities;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    RibaltoneDaLanciareRepository ribaltoneDaLanciareRepository;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Override
    public Class getTargetEntityClass() {
        return ImportazioniOrganigramma.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Persona persona;
        Utente utente;
        if (authenticatedSessionData.getRealPerson() != null)  {
            persona = authenticatedSessionData.getRealPerson();
            utente = authenticatedSessionData.getRealUser();
        } else {
            persona = authenticatedSessionData.getPerson();
            utente = authenticatedSessionData.getUser();
        }
        
        if (!userInfoService.isSD(utente) && !userInfoService.isCI(utente)) {
            Map<String, Map<String, List<String>>> ruoliUtentiPersona = utente.getRuoliUtentiPersona();
            if (ruoliUtentiPersona.containsKey("CA")) {
                List<String> codiciAziendaDiCuiSonoCA = ruoliUtentiPersona.get(Ruolo.CodiciRuolo.CA.toString()).get(Ruolo.ModuliRuolo.GENERALE.toString());
                List<Integer> idAziendaDiCuiSonoCA = codiciAziendaDiCuiSonoCA.stream().map(codiceAzienda -> cachedEntities.getAziendaFromCodice(codiceAzienda).getId()).collect(Collectors.toList());
                initialPredicate = QImportazioniOrganigramma.importazioniOrganigramma.idAzienda.id.in(idAziendaDiCuiSonoCA).and(initialPredicate);
            } else {
                initialPredicate = Expressions.FALSE.eq(true);
            }
        }
        
        return initialPredicate;
    }
}
    

