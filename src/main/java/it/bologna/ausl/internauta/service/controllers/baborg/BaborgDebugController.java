package it.bologna.ausl.internauta.service.controllers.baborg;

import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.repositories.baborg.StoricoRelazioneRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.utils.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import it.bologna.ausl.internauta.service.repositories.scripta.DocDetailRepository;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${internauta.mapping.url.debug}")
public class BaborgDebugController {

    @Autowired
    ProjectionFactory projectionFactory;
    
    @Autowired
    StrutturaRepository strutturaRepository;
    
    @Autowired
    StoricoRelazioneRepository storicoRelazioneRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    ProjectionFactory factory;
    
    @Autowired
    ParametriAziendeReader parametriAziende;
    
    @Autowired
    DocDetailRepository docDetailRepository;

    @Autowired
    ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    @RequestMapping(value = "ping", method = RequestMethod.GET)
    public String ping() {
        return "pong";
    }

    private UtenteStruttura buildUtenteStruttura(Map<String, Object> map) {
        UtenteStruttura res = new UtenteStruttura();
        Field[] fields = UtenteStruttura.class.getDeclaredFields();
        for (Field field : fields) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            try {
                Method setMethod = EntityReflectionUtils.getSetMethod(UtenteStruttura.class, field.getName());
                if (columnAnnotation != null) {
                    try {
                        Object value = map.get(columnAnnotation.name());
                        if (value != null) {
                            if (value.getClass().isAssignableFrom(java.sql.Timestamp.class)) {
                                value = ZonedDateTime.ofInstant(((java.sql.Timestamp) value).toInstant(), ZoneId.systemDefault());
                            }
                            setMethod.invoke(res, value);
                        }
                    } catch (Exception ex) {
                        //ex.printStackTrace();
                    }
                } else {
                    JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
                    if (joinColumnAnnotation != null) {
                        Class<?> fkClass = field.getType();
                        try {
                            Object value = map.get(joinColumnAnnotation.name());
                            Object fkObject = fkClass.getDeclaredConstructor().newInstance();
                            Method primaryKeySetMethod = EntityReflectionUtils.getPrimaryKeySetMethod(fkClass);
                            primaryKeySetMethod.invoke(fkObject, value);

                            setMethod.invoke(res, fkObject);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }

        return res;
    }

    private UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom getUtenteStruttura(Map<String, Object> map) {
        UtenteStruttura utenteStruttura = utenteStrutturaRepository.getOne((Integer) map.get("id"));
        UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom res = factory.createProjection(UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom.class, utenteStruttura);
        return res;
    }

    @RequestMapping(value = "test", method = RequestMethod.GET)
//    @Transactional(rollbackFor = Throwable.class)
    public Object test(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException { //26839
        projectionsInterceptorLauncher.setRequestParams(new HashMap<String, String>(), request);
//        List<UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom> res;
//        List<Map<String, Object>> utentiStrutturaSottoResponsabili = strutturaRepository.getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(34513, null);
        List<Map<String, Object>> utentiStrutturaSottoResponsabili = strutturaRepository.getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(133143, LocalDateTime.of(2017, Month.JUNE, 14, 0, 0, 0));

        List<UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom> res = utentiStrutturaSottoResponsabili.stream().map(utenteStrutturaMap -> {
            UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom utenteStruttura = this.getUtenteStruttura(utenteStrutturaMap);
            //return factory.createProjection(UtenteStrutturaWithIdUtente.class, utenteStruttura);
            return utenteStruttura;
        }).collect(Collectors.toList());
        res.stream().forEach(u -> System.out.println(u.getIdUtente().getId()));
//        UtenteStruttura one = utenteStrutturaRepository.findById(14454160).get();
//        if (true)
//        return Lists.newArrayList(factory.createProjection(UtenteStrutturaWithIdAfferenzaStruttura.class, one));
        /*
        "attivo_al": null,
        "data_inserimento_riga": "2020-05-06T14:00:32.927+0000",
        "id_utente": 367520,
        "version": "2020-05-06T14:00:32.927+0000",
        "id_afferenza_struttura": 3,
        "incarico": true,
        "bit_ruoli": 256,
        "id_azienda_derivazione_unificazione": null,
        "id_dettaglio_contatto": 3788686,
        "attivo": true,
        "id": 14453746,
        "attributi": [
            "R"
        ],
        "attivo_dal": "2020-05-07T09:29:19.377+0000",
        "id_struttura": 26850
         */
        return res;

//       return EmlHandler.handleEml("C:\\Users\\mdonza\\Desktop\\Eml\\test_mail.eml");
        //return EmlHandler.handleEml("C:\\Users\\mdonza\\Desktop\\Eml\\donazione sig.ra Giliola Grillini.eml");
    }

    @RequestMapping(value = "test2", method = RequestMethod.GET)
    public Object test2(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException, SQLException, IOException {
        
//        
//        QDocList qDocList = QDocList.docList;
//        
//        JPQLQuery<Integer> where = JPAExpressions.select(qDocList.id)
//                .from(QDocList.docList)
//                .innerJoin(QPersoneVedenti.personeVedenti)
//                .on(QDocList.docList.id.eq(QPersoneVedenti.personeVedenti.idDocList.id))
//                .where(QPersoneVedenti.personeVedenti.idPersona.eq(245948))
//                .orderBy(QDocList.docList.ranking.asc())
//                .limit(0)
//                .offset(0);
//        
//        BooleanExpression filter = QDocList.docList.id.in(where);
        
//        Iterable<DocList> findAll = docListRepository.findAll(filter);
//        .innerJoin(media.dimensions, dimension )
//        .on(dimension.dimensionType.id.eq(Long.valueOf(inputDimensionType))
        
//            JPASubQuery
//                    .from(qDocList);
//            .where(attribute.in(person.attributes),
//                   attribute.attributeName().name.toLowerCase().eq("eye color"),
//                   attribute.attributeValue.toLowerCase().eq("blue"))
//             .exists()
        
//        Expression<Integer> value = Expressions.asNumber(16);
//        NumberTemplate<Integer> numberTemplate = Expressions.numberTemplate(Integer.class, "function('bitand', {0}, {1})", QUtenteStruttura.utenteStruttura.bitRuoli, value);
//        
//        BooleanExpression filter = QUtenteStruttura.utenteStruttura.id.eq(28075300).and(numberTemplate.gt(0));
//        Iterable<UtenteStruttura> res = utenteStrutturaRepository.findAll(filter);
//        
//        List<ParametroAziende> parameters1 = parametriAziende.getParameters("FiltraResponsabiliMatrint", new Integer[] {2});
//        List<ParametroAziende> parameters2 = parametriAziende.getParameters("FiltraResponsabiliMatrint", new Integer[] {3});
//        
//        if (parameters1 != null && !parameters1.isEmpty())
//            System.out.println(parametriAziende.getValue(parameters1.get(0), Boolean.class));
//        else 
//            System.out.println("parameters1 empty");
//        if (parameters2 != null && !parameters2.isEmpty())
//            System.out.println(parametriAziende.getValue(parameters2.get(0), Boolean.class));
//        else 
//            System.out.println("parameters2 empty");
//        Object struttureRuolo = storicoRelazioneRepository.getStruttureRuolo(256, 351272);

        
//        final long userId = 245948;
//        final Specification<DocDetail> spec = new Specification<DocDetail>() {
//            @Override
//            public Predicate toPredicate(Root<DocDetail> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
//                query.distinct(true);
//                query.select(root.get("id"));
//                query.orderBy(cb.desc(root.get("dataCreazione")));
//                Join<Object, Object> join = root.join("personeVedentiList", JoinType.LEFT);
//                return cb.equal(join.get("idPersona"), userId);
//            }
//
//            @Override
//            public Specification<DocDetail> and(Specification<DocDetail> other) {
//                return Specification.super.and(other); //To change body of generated methods, choose Tools | Templates.
//            }
//            
//            
//        };

        /**
         * Idea: La query la mettiamo nel beforeSelect di scripta. Ci servirà solo a tirara fuori gli id (come con la blackbox)
         * 1- Serve la paginazione. come si mette? E possibile leggerla nell'interceptor?
         * 2- Servirebbe se possibile prendere solo l'id. 
         *  Se non ci si riescie, come mi è parso di capire, allora la facciamo basata su PersoneVedenti e non doclist
         * 3- vanno messi tutti i filtri e gli ordinamenti chiesti dal frontend..
         * 
         * Si però è na merda. si andrebbe a fare due volte la stessa query.. quindi è una idea di merda.
         * Piuttosto servirebbe che a seconda di quello che si vuole ci fosse una sorta di seconda versione del framework con tanto 
         * di interceptor per questa modalità di query "Specification".. 
         */
//        BooleanExpression eq = QDocList.docList.personeVedentiList.any().idPersona.eq(spec);
//        BooleanExpression eq = QDocList.docList.personeVedentiList.any().idPersona.eq(245948);
//          Page<DocList> pageResult = docListRepository.findAll(spec, pageRequest);

//        Iterable<DocList> findAll = docListRepository.findAll(filter);
//        Iterable<DocList> findAll = docListRepository.findAll(spec);
//        Stream<DocList> stream = StreamSupport.stream(findAll.spliterator(), false);
//        Stream<DocListWithPersoneVedentiList> map = stream.map(a -> projectionFactory.createProjection(DocListWithPersoneVedentiList.class, a));
//        return map.collect(Collectors.toList());
        return new Struttura();
    }
}
