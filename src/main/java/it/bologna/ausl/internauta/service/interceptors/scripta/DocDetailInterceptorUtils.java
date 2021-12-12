package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.google.common.base.CaseFormat;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimpleExpression;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.QPersonaVedente;
import it.bologna.ausl.model.entities.scripta.views.QDocDetailView;
import it.nextsw.common.interceptors.NextSdrControllerInterceptor;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class DocDetailInterceptorUtils {
    
    @Autowired
    InternautaUtils internautaUtils;
    
    @Autowired
    UserInfoService userInfoService;
    
    /**
     * La variabile threadlocal filterDescriptor è una mappa. Le sue chiavi sono
     * tutti i fields filtrati dal frontend. La funzione torna true se almeno
     * uno dei fields in esame è ritenuto un campo sensisbile. L'elenco dei
     * campi sensibili è passatto come parametro.
     * @param specialFields
     * @return
     */
    public Boolean isFilteringSpecialFields(String[] specialFields) {
        Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
        if (!filterDescriptorMap.isEmpty()) {
            Pattern pattern = Pattern.compile("\\.(.*?)(\\.|$)");
            Set<Path<?>> pathSet = filterDescriptorMap.keySet();
            for (Path<?> path : pathSet) {
                Matcher matcher = pattern.matcher(path.toString());
                matcher.find();
                String fieldName = matcher.group(1);
                if (Arrays.stream(specialFields).anyMatch(fieldName::equals)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Metodo chiamato in after select. Si occupa di fare dei controlli sul
     * risultato, eventualmente nascondendo dei campi.
     * @param entities 
     */
    public void manageAfterCollection(Collection<Object> entities, AuthenticatedSessionData authenticatedSessionData, BiFunction<Object,Persona,Boolean> fnPienaVisibilita) throws IOException {
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();
        List<Integer> listaIdAziendaOsservatore = userInfoService.getListaIdAziendaOsservatore(persona);
        Boolean isSuperDemiurgo = userInfoService.isSD(user);
        for (Object entity : entities) {
            DocDetailInterface doc = (DocDetailInterface) entity;
            securityHiding(doc, persona, isSuperDemiurgo, listaIdAziendaOsservatore, fnPienaVisibilita);
            buildUrlComplete(doc, persona, authenticatedSessionData);
        }
    }
    
    /**
     * Metodo chiamato a seguito di una select. Se il doc è riservato e l'utente
     * connesso non è autorizzato nascondo i campi sensibili.
     * @param doc
     */
    private void securityHiding(DocDetailInterface doc, Persona persona, Boolean isSuperDemiurgo, List<Integer> listaIdAziendaOsservatore, BiFunction<Object,Persona,Boolean> fnPienaVisibilita) {
        if ((doc.getRiservato() || (doc.getVisibilitaLimitata() && !listaIdAziendaOsservatore.contains(doc.getIdAzienda().getId())))
                && !isSuperDemiurgo
                && !fnPienaVisibilita.apply(doc, persona)) {
            
            doc.setFirmatari(null);
            doc.setFascicolazioni(null);
            doc.setFascicolazioniTscol(null);
            doc.setTscol(null);
            
            if (doc.getRiservato()) {
                doc.setOggetto("[RISERVATO]");
                doc.setOggettoTscol(null);
                doc.setDestinatari(null);
                doc.setDestinatariTscol(null);
                doc.setIdPersonaRedattrice(null);
            }
        }
    }
    
    private void buildUrlComplete(DocDetailInterface doc, Persona persona, AuthenticatedSessionData authenticatedSessionData) throws IOException {
        if (doc.getCommandType() == DocDetail.CommandType.URL) {
            doc.setUrlComplete(
                internautaUtils.getUrl(
                    authenticatedSessionData, 
                    doc.getOpenCommand(), 
                    getIdApplicazione(doc), 
                    doc.getIdAzienda()
                )
            );
        }
    }
    
    private String getIdApplicazione(DocDetailInterface doc) {
        String idApplicazione = null;
        switch (doc.getTipologia()) {
            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                idApplicazione = Applicazione.Applicazioni.procton.toString();
                break;
            case DETERMINA:
                idApplicazione = Applicazione.Applicazioni.dete.toString();
                break;    
            case DELIBERA:
                idApplicazione = Applicazione.Applicazioni.deli.toString();
                break;
        }
        return idApplicazione;
    }
    
    public BooleanExpression duplicateFiltersPerPartition(Class entityClass, String dataCreazioneNameField) {
        BooleanExpression filter = Expressions.TRUE.eq(true);
        Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
        // QPersonaVedente qPersonaVedente = QPersonaVedente.personaVedente;
        PathBuilder<?> qEntity = new PathBuilder(entityClass, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,entityClass.getSimpleName()));
        if (!filterDescriptorMap.isEmpty()) {
            Pattern pattern = Pattern.compile("\\.(.*?)(\\.|$)");
            Set<Path<?>> pathSet = filterDescriptorMap.keySet();
            System.out.println(pathSet.toString());
            for (Path<?> path : pathSet) {
                Matcher matcher = pattern.matcher(path.toString());
                matcher.find();
                String fieldName = matcher.group(1);
                if (fieldName.equals("idAzienda")) {
                    List<Object> ids = filterDescriptorMap.get(path);
                    for (Object id : ids) {
                        PathBuilder<Azienda> qAzienda = qEntity.get("idAzienda", Azienda.class);
                        filter = filter.and(qAzienda.get("id").eq((Integer) id));
                    }
                } else if (fieldName.equals("dataCreazione")) {
//                     if (List.class.isAssignableFrom(filterDescriptorMap.get(path).getClass())) {
                    DateTimePath<ZonedDateTime> dataCreazionePath = qEntity.getDateTime(dataCreazioneNameField, ZonedDateTime.class);
                    
                    if (filterDescriptorMap.get(path).size() == 2) {
                        ZonedDateTime data1 = (ZonedDateTime) filterDescriptorMap.get(path).get(0);
                        ZonedDateTime data2 = (ZonedDateTime) filterDescriptorMap.get(path).get(1);
                        if (data1.isBefore(data2)) {
                            
                            filter = filter.and(dataCreazionePath.goe(data1).and(dataCreazionePath.lt(data2)));
                        } else {
                            filter = filter.and(dataCreazionePath.goe(data2).and(dataCreazionePath.lt(data1)));
                        }
                    } else {
                        ZonedDateTime data = (ZonedDateTime) filterDescriptorMap.get(path).get(0);
                        ZonedDateTime startDate = data.toLocalDate().atTime(0, 0, 0).atZone(data.getZone());
                        ZonedDateTime endDate = startDate.plusDays(1);
                        filter = filter.and(dataCreazionePath.goe(startDate).and(dataCreazionePath.lt(endDate)));
                    }
                }
            }
        }
        return filter;
    }
}
