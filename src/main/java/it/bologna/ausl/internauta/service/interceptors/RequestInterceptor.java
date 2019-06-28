package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.internauta.service.repositories.logs.KrintRepository;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.logs.Krint;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class RequestInterceptor extends HandlerInterceptorAdapter{
    
    
    
    private final HttpSessionData httpSessionData;
    private final KrintRepository krintRepository;

    public RequestInterceptor(HttpSessionData httpSessionData, KrintRepository krintRepository) {
        this.httpSessionData = httpSessionData;
        this.krintRepository = krintRepository;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest hsr, HttpServletResponse hsr1, Object o, Exception excptn) throws Exception {
        
        Krint krint = (Krint)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ROW);
        
        if(krint != null){
            try {
                krintRepository.save(krint);         
            } catch (Exception e) {
                // TODO: loggare l'errore
            }
   
            httpSessionData.resetDataMap();
        }
        
        
        super.afterCompletion(hsr, hsr1, o, excptn);
    }
    
    
}
