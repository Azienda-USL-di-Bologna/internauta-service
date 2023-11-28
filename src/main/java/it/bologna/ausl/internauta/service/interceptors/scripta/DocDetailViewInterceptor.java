package it.bologna.ausl.internauta.service.interceptors.scripta;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData.OperationsRequested.FilterTraDocumentiRegistrati;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData.OperationsRequested.VisualizzaTabRegistrazioni;
import it.bologna.ausl.model.entities.baborg.Persona; 
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.views.DocDetailView;
import it.bologna.ausl.model.entities.scripta.views.QDocDetailView;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors; 
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "docdetailview-interceptor")
public class DocDetailViewInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocDetailViewInterceptor.class);

    @Override
    public Class getTargetEntityClass() {
        return DocDetailView.class;
    }

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private DocDetailInterceptorUtils docDetailInterceptorUtils;
    
    @Autowired
    private ScriptaInterceptorUtils scriptaInterceptorUtils;

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        QDocDetailView qdoclist = QDocDetailView.docDetailView;
//        QDocDetailView qdoclistView = QDocDetailView.docDetailView;
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        
        initialPredicate = safetyFilters().and(initialPredicate);
        
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
           
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                case FilterTraDocumentiRegistrati:
                    initialPredicate = qdoclist.dataRegistrazione.isNotNull().and(initialPredicate);
                    break;}
            }
        }
        initialPredicate = scriptaInterceptorUtils.duplicateFiltersPerPartition(DocDetailView.class, "dataCreazioneDoc", "idAziendaDoc").and(initialPredicate);

//        InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);

//        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
//        if (operationsRequested != null && !operationsRequested.isEmpty()) {
//            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
//                switch (operationRequested) {
//                    
//                }
//            }
//        }
        
        return super.beforeSelectQueryInterceptor(initialPredicate, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        List<Object> entities = new ArrayList();
        entities.add(entity);
        try {
            AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
            BiFunction<Object, Persona, Boolean> fnPienaVisibilita = (o, p) -> pienaVisibilita((DocDetailView) o, p);
            docDetailInterceptorUtils.manageAfterCollection(entities, authenticatedSessionData, fnPienaVisibilita);
        } catch (IOException ex) {
            throw new AbortLoadInterceptorException("Errore nella generazione dell'url", ex);
        }
        return entity;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        try {
            AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
            BiFunction<Object, Persona, Boolean> fnPienaVisibilita = (o, p) -> pienaVisibilita((DocDetailView) o, p);
            docDetailInterceptorUtils.manageAfterCollection(entities, authenticatedSessionData, fnPienaVisibilita);
        } catch (IOException ex) {
            throw new AbortLoadInterceptorException("Errore nella generazione dell'url", ex);
        }
        return entities;
    }

    /**
     * Controlla se l'utente connesso ha pienaVisbilita: true nella colonna
     * personeVedenti del doc passato.
     *
     * @param doc
     * @return
     */
    private Boolean pienaVisibilita(DocDetailView doc, Persona persona) {
        return doc.getIdPersona().getId().equals(persona.getId()) && doc.getPienaVisibilita();
    }

    /**
     * Questa funzione si occupa di generare un predicato che contenga tutti i
     * filtri di sicurezza che riguardano docDetailView Essi sono: 
     *  1- Se demiurgo vede tutto 
     *  2- Gli altri vedono solo documenti delle aziende su cui sono attivi 
     *  3- Se osservatore vede tutto delle aziende su cui è osservatore tranne i 
     *     riservati 
     *  4- Se utente generico vede solo le sue proposte 
     *  5- Se segretario vede anche proposte non sue purché dei suoi "superiori"
     *  6- Se utente sta cercando per campi sensibili e non ha piena
     *     visibilità non vede riservati/vis lim
     */
    private BooleanExpression safetyFilters() {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Utente realUser = authenticatedSessionData.getRealUser();
        Persona persona = user.getIdPersona();
        QDocDetailView qdocdetailview = QDocDetailView.docDetailView;
        BooleanExpression filter = Expressions.TRUE.eq(true);
        
        // Filtro 1 -  Se demiurgo vede tutto 
        if (!userInfoService.isSD(user)) { 
            
            String[] visLimFields = {"firmatari", "idArchivi", "tscol"};
            String[] reservedFields = {"oggetto", "oggettoTscol", "destinatari", "destinatariTscol", "tscol", "firmatari", "idPersonaRedattrice", "idArchivi"};
            List<Integer> listaIdAziendaUtenteAttivo = userInfoService.getAziendePersona(persona).stream().map(aziendaPersona -> aziendaPersona.getId()).collect(Collectors.toList());
            List<Integer> listaIdAziendaOsservatore = userInfoService.getListaIdAziendaOsservatore(persona);
            List<Integer> listaIdAziendaResponsabileVersamenti = userInfoService.getIdAziendaListDovePersonaHaRuolo(persona, Ruolo.CodiciRuolo.RV);
            List<Integer> listaIdAziendaCA = userInfoService.getIdAziendaListDovePersonaHaRuolo(persona, Ruolo.CodiciRuolo.CA);

            Integer[] idStruttureSegretario = userInfoService.getStruttureDelSegretario(persona);
            BooleanExpression pienaVisibilita = qdocdetailview.idPersona.id.eq(persona.getId()).and(qdocdetailview.pienaVisibilita.eq(Expressions.TRUE));
            BooleanExpression personaVedente = qdocdetailview.idPersona.id.eq(persona.getId());
            BooleanExpression sonoSegretario = null;
            if (idStruttureSegretario != null && idStruttureSegretario.length > 0) {
                sonoSegretario = Expressions.booleanTemplate(
                        String.format("FUNCTION('array_operation', '%s', '%s', {0}, '%s')= true", StringUtils.join(idStruttureSegretario, ","), "integer[]", "&&"),
                        qdocdetailview.idStruttureSegreteria
                );
            } else {
                sonoSegretario = Expressions.FALSE.eq(true);
            }
            
            /* Filtro 4 Se utente generico vede solo le sue proposte
               Filtro 5 Se segretario vede anche proposte non sue purché dei suoi "superiori"
            */
            BooleanExpression filtroStandard = qdocdetailview.numeroRegistrazione.isNotNull()
                    .or(personaVedente) 
                    .or(sonoSegretario);
                    
            
             // Filtro 6 Riservato
            filtroStandard = filtroStandard.and(
                    qdocdetailview.riservato.eq(Boolean.FALSE)
                            .or(Expressions.FALSE.eq(docDetailInterceptorUtils.isFilteringSpecialFields(reservedFields)))
                            .or(pienaVisibilita)
            );

            //Filtro 6 Visibilità limitata
            filtroStandard = filtroStandard.and(
                    qdocdetailview.visibilitaLimitata.eq(Boolean.FALSE) 
                            .or(Expressions.FALSE.eq(docDetailInterceptorUtils.isFilteringSpecialFields(visLimFields)))
                            .or(pienaVisibilita)
            );
            
            
          
            BooleanExpression filtroResponsabileVersamento = qdocdetailview.idAzienda.id.in(listaIdAziendaResponsabileVersamenti)
                .and(qdocdetailview.idAziendaDoc.id.in(listaIdAziendaResponsabileVersamenti));


            // Filtro 3 - Se osservatore vede tutto delle aziende su cui è osservatore tranne i riservati 
            BooleanExpression filtroOsservatore = qdocdetailview.idAzienda.id.in(listaIdAziendaOsservatore)
                    .and(qdocdetailview.idAziendaDoc.id.in(listaIdAziendaOsservatore))
                    .and(qdocdetailview.riservato.eq(Boolean.FALSE)); 

             // Filtro 2 - Gli altri vedono solo documenti delle aziende su cui sono attivi
            filter = qdocdetailview.idAzienda.id.in(listaIdAziendaUtenteAttivo)
                    .and(qdocdetailview.idAziendaDoc.id.in(listaIdAziendaUtenteAttivo));
            
            //filtri sui ruoli
            filter = filter.and(
                    filtroOsservatore
                        .or(filtroStandard)
                        .or(filtroResponsabileVersamento)
                    );
            
            filter = (qdocdetailview.tipologia.ne(DocDetail.TipologiaDoc.DOCUMENT_REGISTRO)
                      .or(qdocdetailview.idAzienda.id.in(listaIdAziendaCA))
                      .or(filtroResponsabileVersamento)
                    ).and(filter);
        }

        return filter;
    }

//    public BooleanExpression duplicateFiltersPerPartition() {
//        BooleanExpression filter = Expressions.TRUE.eq(true);
//        Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
//        QDocDetailView qdocdetailview = QDocDetailView.docDetailView;
//        if (!filterDescriptorMap.isEmpty()) {
//            Pattern pattern = Pattern.compile("\\.(.*?)(\\.|$)");
//            Set<Path<?>> pathSet = filterDescriptorMap.keySet();
//            System.out.println(pathSet.toString());
//            for (Path<?> path : pathSet) {
//                Matcher matcher = pattern.matcher(path.toString());
//                matcher.find();
//                String fieldName = matcher.group(1);
//                if (fieldName.equals("idAzienda")) {
//                    List<Object> ids = filterDescriptorMap.get(path);
//                    for (Object id : ids) {
//                        filter = filter.and(qdocdetailview.idAziendaDoc.id.eq((Integer) id));
//                    }
//                } else if (fieldName.equals("dataCreazione")) {
////                     if (List.class.isAssignableFrom(filterDescriptorMap.get(path).getClass())) {
//                    if (filterDescriptorMap.get(path).size() == 2) {
//                        ZonedDateTime data1 = (ZonedDateTime) filterDescriptorMap.get(path).get(0);
//                        ZonedDateTime data2 = (ZonedDateTime) filterDescriptorMap.get(path).get(1);
//                        if (data1.isBefore(data2)) {
//                            filter = filter.and(qdocdetailview.dataCreazioneDoc.goe(data1).and(qdocdetailview.dataCreazioneDoc.lt(data2)));
//                        } else {
//                            filter = filter.and(qdocdetailview.dataCreazioneDoc.goe(data2).and(qdocdetailview.dataCreazioneDoc.lt(data1)));
//                        }
//                    } else {
//                        ZonedDateTime data = (ZonedDateTime) filterDescriptorMap.get(path).get(0);
//                        ZonedDateTime startDate = data.toLocalDate().atTime(0, 0, 0).atZone(data.getZone());
//                        ZonedDateTime endDate = startDate.plusDays(1);
//                        filter = filter.and(qdocdetailview.dataCreazioneDoc.goe(startDate).and(qdocdetailview.dataCreazioneDoc.lt(endDate)));
//                    }
//                }
//            }
//        }
//        return filter;
//    }
}
