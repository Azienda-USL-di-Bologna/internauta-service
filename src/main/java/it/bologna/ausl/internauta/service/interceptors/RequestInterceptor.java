package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.internauta.service.krint.KrintError;
import it.bologna.ausl.internauta.service.repositories.logs.KrintRepository;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.logs.Krint;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class RequestInterceptor extends HandlerInterceptorAdapter{
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestInterceptor.class);
    
    
    private final HttpSessionData httpSessionData;
    private final KrintRepository krintRepository;

    public RequestInterceptor(HttpSessionData httpSessionData, KrintRepository krintRepository) {
        this.httpSessionData = httpSessionData;
        this.krintRepository = krintRepository;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest hsr, HttpServletResponse hsr1, Object o, Exception excptn) throws Exception {
        
        // Se è arrivato una eccezione non ho fatto niente e quindi non voglio loggare niente
        if (excptn == null) {
        
            List<Krint> krintList = (List<Krint>)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ROWS);

            if(krintList != null && krintList.size() > 0) {
                try {
                    krintList.forEach(k -> krintRepository.save(k) );
                } catch (Exception e) {
                    LOGGER.error("KRINT ERROR: errore nel salvataggio di una riga di krint");
                }
            }
            
            List<KrintError> krintErrorList = (List<KrintError>)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ERRORS);
            
            if(krintErrorList != null && krintErrorList.size() > 0) {
                try {
                    krintErrorList.forEach(k -> LOGGER.error(
                            String.format("KRINT ERROR: idUtente: %s, idRealUser: %s, idOggetto: %s, functionName: %s, codiceOperazione: %s", 
                                k.getIdUtente() == null ? "null" : k.getIdUtente().toString(),
                                k.getIdRealUser() == null ? "null" : k.getIdRealUser().toString(),
                                k.getIdOggetto() == null ? "null" : k.getIdOggetto().toString(),
                                k.getFunctionName() == null ? "null" : k.getFunctionName(),
                                k.getCodiceOperazione() == null ? "null" : k.getCodiceOperazione().toString())) );
                } catch (Exception e) {
                    LOGGER.error("KRINT ERROR: errore nella scrittura del log del krint error");
                }
            }
            
        } else {
            LOGGER.error("Rilevata eccezione nel afterCompletion del RequestInterceptor");
            LOGGER.error(excptn.toString());
        }
        
        // TODO: capire se il reset map va qui o dentro l'if
        httpSessionData.resetDataMap();
        
        super.afterCompletion(hsr, hsr1, o, excptn);
    }
}
