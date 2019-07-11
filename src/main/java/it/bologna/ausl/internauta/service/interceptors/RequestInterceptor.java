package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.internauta.service.repositories.logs.KrintRepository;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.logs.Krint;
import java.util.List;
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
        
        List<Krint> krintList = (List<Krint>)httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ROWS);
        
        if(krintList != null && krintList.size() > 0) {
            try {
                krintList.forEach(k -> krintRepository.save(k) );
            } catch (Exception e) {
                System.out.println("ciao ciao");
                // TODO: loggare l'errore
            }
   
            httpSessionData.resetDataMap();
        }
        
        
        super.afterCompletion(hsr, hsr1, o, excptn);
    }
    
    
}
