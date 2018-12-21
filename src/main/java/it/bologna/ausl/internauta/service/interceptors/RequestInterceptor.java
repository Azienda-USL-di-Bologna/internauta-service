package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class RequestInterceptor extends HandlerInterceptorAdapter{
    
    private final HttpSessionData httpSessionData;

    public RequestInterceptor(HttpSessionData httpSessionData) {
        this.httpSessionData = httpSessionData;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest hsr, HttpServletResponse hsr1, Object o, Exception excptn) throws Exception {
        httpSessionData.resetDataMap();
        super.afterCompletion(hsr, hsr1, o, excptn);
    }
    
    
}
