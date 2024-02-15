package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import static com.querydsl.jpa.JPAExpressions.select;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDiInteresseRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.ArchivioDiInteresse;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QArchivioDiInteresse;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrControllerInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * La visibilità degli archivi è più semplice della visibilità dei documenti.
 * Non abbiamo qui permessi di "non piena visibilità". Anche il cosiddetto
 * "permesso di transito" nell'archivio permette di vedere tutto di
 * quell'archivio, il blocco riguarda solo il contenuto dello stesso.
 *
 * L'ArchivioDetail viene usata quando si vogliono trovare tutti gli archivi,
 * neri compresi. Gli archivi neri sono sempre trovabili a meno che non siano
 * riservati.
 *
 * Questo interceptor si occupa quindi di: 1- Aggiungere il controllo di
 * sicurezza tale per cui l'utente loggato abbia permesso sull'archivio cercato
 * qualora quest'ultimo sia riservato. 2- Settare, nell'after select, la
 * proprietà transient forbidden che indica l'assenza di permessi su quel
 * fascicolo.
 *
 * Il controllo di sicurezza non viene inserito nel caso che l'utente reale sia
 * un demiurgo.
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "archiviodetail-interceptor")
public class ArchivioDetailInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchivioDetailInterceptor.class);

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private ArchivioDiInteresseRepository archivioDiInteresseRepository;

    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Override
    public Class getTargetEntityClass() {
        return ArchivioDetail.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();
        ArchivioDiInteresse archivioDiInteresse = null;
        //initialPredicate = safetyFilters().and(initialPredicate);

        Boolean safetyFiltersNonNecessari = false; // Ci sono dei casi in cui non voglio aggiungere filtri di sicurezza. Questi casi sono quelli in cui l'utente vuole vedere archivi che ha già usato e sono in archiviDiInteresse
        
        QArchivioDetail qArchivioDetail = QArchivioDetail.archivioDetail;
        BooleanExpression noBozze = qArchivioDetail.stato.ne(Archivio.StatoArchivio.BOZZA.toString());
        BooleanExpression bitAnomalieNotNull = qArchivioDetail.bitAnomalie.isNotNull();
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case VisualizzaTabPreferiti:
                        safetyFiltersNonNecessari = true;
                        archivioDiInteresse = getArchivioDiInteresse(persona);
                        if (archivioDiInteresse != null) {
                            Integer[] idArchiviPreferiti = archivioDiInteresse.getIdArchiviPreferiti();
                            initialPredicate = getFilterDiInteresse(idArchiviPreferiti).and(initialPredicate);
                        } else {
                            BooleanExpression filter = Expressions.TRUE.eq(false);
                            initialPredicate = filter.and(initialPredicate);
                        }

                        break;
//                    case VisualizzaTabFrequenti:
//                        safetyFiltersNonNecessari = true;
//                        archivioDiInteresse = getArchivioDiInteresse(persona);
//                        if (archivioDiInteresse != null) {
//                            Integer[] idArchiviFrequenti = archivioDiInteresse.getIdArchiviFrequenti();
//                            initialPredicate = getFilterDiInteresse(idArchiviFrequenti).and(initialPredicate);
//                        }
//                        break;
                    case VisualizzaTabRecenti:
                        safetyFiltersNonNecessari = true;
                        //Optional<Integer[]> res = archiviRecentiRepository.getArchiviFromPersona(persona.getId());
                        //if (res.isPresent()) {
                        //Integer[] idArchiviRecenti = res.get();
                        //initialPredicate = getFilterDiInteresse(idArchiviRecenti).and(initialPredicate);
                        //}
                        //archivioDiInteresse = getArchivioDiInteresse(persona);
                        //if (archivioDiInteresse != null) {
                        //Integer[] idArchiviRecenti = archivioDiInteresse.getIdArchiviRecenti();
                        //initialPredicate = getFilterDiInteresse(idArchiviRecenti).and(initialPredicate);
                        //}
                        break;
                    case VisualizzaTabTutti:
                        initialPredicate = noBozze.and(initialPredicate);
                        break;
                    case VisualizzaTabAnomalie:
                        initialPredicate = noBozze.and(initialPredicate);
                        initialPredicate = bitAnomalieNotNull.and(initialPredicate);
                        break;
                }
            }
        }

        if (!safetyFiltersNonNecessari) {
            initialPredicate = safetyFilters().and(initialPredicate);
        }

        return initialPredicate;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return super.afterSelectQueryInterceptor(entities, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return super.afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Questa funzione si occupa di generare il predicato di sicurezza per far
     * si che l'utente trovi solo archivi non riservati o riservati su cui ha
     * permesso. Inoltre, non voglio vedere le bozze se non sono mie. Inoltre,
     * voglio vedere solo archivi della mia azienda.
     *
     * Se sono demiurgo non servono filtri si sicurezza.
     */
    private BooleanExpression safetyFilters() throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Utente realUser = authenticatedSessionData.getRealUser();
        Persona persona = user.getIdPersona();
        BooleanExpression filter = Expressions.TRUE.eq(true);
        QArchivioDetail archivioDetail = QArchivioDetail.archivioDetail;
        QPermessoArchivio permessoArchivio = QPermessoArchivio.permessoArchivio;

        if (!userInfoService.isSD(user) 
                && !userInfoService.isCA(user) 
                && !userInfoService.isAG(user) 
                && !userInfoService.isOS(user)) {

            // Se nel filtro c'è una azienda parlante devo lanciare eccezione
            List<ParametroAziende> fascicoliParlanti = cachedEntities.getParameters("fascicoliParlanti");
            if (fascicoliParlanti != null && !fascicoliParlanti.isEmpty()) {
                for (ParametroAziende parametro : fascicoliParlanti) {
                    if (parametriAziende.getValue(parametro, Boolean.class)) {
                        Integer[] idAziendeParlanti = parametro.getIdAziende();
                        List<Integer> idAziendeParlantiList = new ArrayList(Arrays.asList(idAziendeParlanti));//IntStream.of(Arrays.stream(idAziendeParlanti).mapToInt(Integer::intValue).toArray()).boxed().collect(Collectors.toCollection(ArrayList::new));
//                idAziendeParlantiList.addAll(Arrays.asList(idAziendeParlanti));
//                List<Integer> idAziendeParlantiList = Arrays.stream(idAziendeParlanti).boxed().collect(Collectors.toList());

                        // Dalla lsita di aziende Parlanti devo togliere glie ventuali elementi in cui sono un AG. Dato che un AG deve sempre poter vedere gli archivi della sua azienda.
                        List<Integer> idAziendaListDoveAG = userInfoService.getIdAziendaListDovePersonaHaRuolo(persona, Ruolo.CodiciRuolo.AG);
                        idAziendeParlantiList.removeAll(idAziendaListDoveAG);

                        List<Integer> idAziendaFiltranti = getIdAziendeFiltranti();
                        if (idAziendaFiltranti == null && !idAziendeParlantiList.isEmpty()) {
                            throw new AbortLoadInterceptorException("Si sta cercando su una azienda con fascicoli parlanti. Questo non è permesso");
                        }
                        idAziendaFiltranti.retainAll(idAziendeParlantiList);
                        if (!idAziendaFiltranti.isEmpty()) {
                            throw new AbortLoadInterceptorException("Si sta cercando su una azienda con fascicoli parlanti. Questo non è permesso");
                        }
                    }
                }
            }

            List<Integer> listaIdAziendaUtenteAttivo = userInfoService.getAziendePersona(persona).stream().map(aziendaPersona -> aziendaPersona.getId()).collect(Collectors.toList());

            SubQueryExpression<Long> queryPersonaConPermesso
                    = select(permessoArchivio.id)
                            .from(permessoArchivio)
                            .where(
                                    permessoArchivio.idPersona.id.eq(persona.getId()),
                                    archivioDetail.id.eq(permessoArchivio.idArchivioDetail.id),
                                    archivioDetail.idAzienda.id.eq(permessoArchivio.idAzienda.id),
                                    archivioDetail.dataCreazione.eq(permessoArchivio.dataCreazione)
                            );
            BooleanExpression personaConPermesso
                    = archivioDetail.riservato.eq(Expressions.FALSE).and(archivioDetail.livello.eq(1))
                            .or(archivioDetail.permessiArchivioList.any().id.eq(queryPersonaConPermesso));
//            BooleanExpression mieBozze =
//                    archivioDetail.stato.eq(Archivio.StatoArchivio.BOZZA.toString())
//                    .and(archivioDetail.idPersonaCreazione.id.eq(persona.getId()));
            BooleanExpression soloMieAziende
                    = archivioDetail.idAzienda.id.in(listaIdAziendaUtenteAttivo);

            filter = filter.and(personaConPermesso);
            filter = filter.and(soloMieAziende);
        }

        return filter;
    }

    /**
     * Recupero dal filterDescriptor su quali aziende sta avvenendo il filtro
     *
     * @return la lista di id delle azinede che sto filtrando
     */
    private List<Integer> getIdAziendeFiltranti() {
        Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
        if (!filterDescriptorMap.isEmpty()) {
            Pattern pattern = Pattern.compile("\\.(.*?)(\\.|$)");
            Set<Path<?>> pathSet = filterDescriptorMap.keySet();
            for (Path<?> path : pathSet) {
                Matcher matcher = pattern.matcher(path.toString());
                matcher.find();
                String fieldName = matcher.group(1);
//                System.out.println("fieldName " + fieldName);
                if (fieldName.equals("idAzienda")) {
                    return filterDescriptorMap.get(path).stream().map(o -> (Integer) o).collect(Collectors.toList());
                }
            }
        }
        return null;
    }

    /**
     * Data una persona ritorna il suo ArchivioDiInteresse
     *
     * @param persona
     * @return
     */
    private ArchivioDiInteresse getArchivioDiInteresse(Persona persona) {
        ArchivioDiInteresse archivioDiInteresse = null;
        QArchivioDiInteresse qArchivioDiInteresse = QArchivioDiInteresse.archivioDiInteresse;
        BooleanExpression filter = qArchivioDiInteresse.idPersona.id.eq(persona.getId());
        Optional<ArchivioDiInteresse> res = archivioDiInteresseRepository.findOne(filter);
        if (res.isPresent()) {
            archivioDiInteresse = res.get();
        }
        return archivioDiInteresse;
    }

    /**
     * Torno il filtro sugli id di interesse
     *
     * @param idArchiviDiInteresse
     * @return
     */
    private BooleanExpression getFilterDiInteresse(Integer[] idArchiviDiInteresse) {
        QArchivioDetail qArchivioDetail = QArchivioDetail.archivioDetail;
        if (idArchiviDiInteresse != null && idArchiviDiInteresse.length > 0) {
            return qArchivioDetail.id.in(idArchiviDiInteresse);
        } else {
            return Expressions.TRUE.eq(false); // Non ho interessi. Quindi non troverò nulla.
        }
    }
}
