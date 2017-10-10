package it.nextsw.odata.bean;


import it.nextsw.olingo.context.CustomOdataJpaContextImpl;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.jpa.processor.api.ODataJPAContext;
import org.springframework.security.core.context.SecurityContext;

/**
 * Created by f.longhitano on 23/06/2017.
 */
public class CustomExtendOperationBase implements WithODataJPAContext {

    private ODataJPAContext oDataJPAContext;



//    public SecurityContext getSecurityContext(){
//        if(oDataJPAContext!=null && oDataJPAContext.getClass().isAssignableFrom(CustomOdataJpaContextImpl.class))
//            return ((CustomOdataJpaContextImpl) oDataJPAContext).getSecurityContext();
//        return null;
//    }

    @Override
    public ODataJPAContext getoDataJPAContext() {
        return oDataJPAContext;
    }

    @Override
    public void setoDataJPAContext(ODataJPAContext oDataJPAContext) {
        this.oDataJPAContext=oDataJPAContext;
    }
}
