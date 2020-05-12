package it.bologna.ausl.internauta.service.controllers.baborg;

import com.google.common.collect.Lists;
import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.controllers.shpeck.ShpeckCustomController;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecUtente;
import it.bologna.ausl.model.entities.baborg.Permesso;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecUtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdUtente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithPlainFields;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${internauta.mapping.url.debug}")
public class BaborgDebugController {

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    ProjectionFactory factory;
    
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
                                value = ZonedDateTime.ofInstant(((java.sql.Timestamp)value).toInstant(), ZoneId.systemDefault());
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
    public Object test(HttpServletRequest request) throws EmlHandlerException, UnsupportedEncodingException { //26839
        projectionsInterceptorLauncher.setRequestParams(new HashMap<String, String>(), request);
//        List<UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom> res;
        List<Map<String, Object>>  utentiStrutturaSottoResponsabili = strutturaRepository.getIdUtentiStruttureWithSottoResponsabiliByIdStruttura(26839);

        List<UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom> res = utentiStrutturaSottoResponsabili.stream().map(utenteStrutturaMap -> {
            UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom utenteStruttura = this.getUtenteStruttura(utenteStrutturaMap);
            //return factory.createProjection(UtenteStrutturaWithIdUtente.class, utenteStruttura);
            return utenteStruttura;
        }).collect(Collectors.toList());
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

}
