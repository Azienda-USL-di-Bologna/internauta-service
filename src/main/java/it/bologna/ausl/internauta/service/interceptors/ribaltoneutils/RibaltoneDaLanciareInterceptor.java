package it.bologna.ausl.internauta.service.interceptors.ribaltoneutils;

import it.bologna.ausl.internauta.service.interceptors.baborg.*;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.ribaltoneutils.RibaltoneDaLanciareRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.ribaltoneutils.QRibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
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
@NextSdrInterceptor(name = "ribaltonedalanciare-interceptor")
public class RibaltoneDaLanciareInterceptor extends InternautaBaseInterceptor{
    private static final Logger LOGGER = LoggerFactory.getLogger(RibaltoneDaLanciareInterceptor.class);
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    RibaltoneDaLanciareRepository ribaltoneDaLanciareRepository;
    
    @Override
    public Class getTargetEntityClass() {
        return RibaltoneDaLanciare.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        //getAuthenticatedUserProperties();  
        
        // sarebbe giusto che solo gli utenti superaziendali vedessero tutte le aziende. 
        // questo però cozza con le strutture unificate. L'albero nell'organigramma ha bisogno
        // di quest'informazione -> quindi non posso mettere nessun filtro in base ai ruoli
        //
        // solo i CI possono vedere tutte le aziende o tutti i ruoli superaziendali
//        if (!(isCI(user) || isAS(user) || isSD(user))) {
//            Persona persona = personaRepository.getOne(person.getId());
//            List<Integer> idAziendePersona = userInfoService.getAziendePersona(persona)
//                    .stream().map(azienda -> azienda.getId()).collect(Collectors.toList());
//            
//            BooleanExpression customFilter = QAzienda.azienda.id.in(idAziendePersona);
//            initialPredicate = customFilter.and(initialPredicate);                                   
//        }
        
//        /**
//         * Gestistisco eventuali additional data
//         */
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case GetUltimoStatoRibaltone:
                    List<Integer> ultimi = ribaltoneDaLanciareRepository.getUltimoStato();
                        BooleanExpression ultimoStato = QRibaltoneDaLanciare.ribaltoneDaLanciare.id.in(
                            ultimi
                        );
//                        BooleanExpression ultimoStato = QAzienda.azienda.strutturaList.any().id.in(ultimi);
//                        BooleanExpression ultimoStato = QAzienda.azienda.attivitaList.any().id.in(ultimi);
                        initialPredicate = ultimoStato.and(initialPredicate);
                    break;
                }
            }
        }
        return initialPredicate;
    }
            
}
    

