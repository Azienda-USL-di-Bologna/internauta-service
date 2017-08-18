package it.bologna.ausl.baborg.odata.processor;


import org.apache.log4j.Logger;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.processor.ODataErrorCallback;
import org.apache.olingo.odata2.api.processor.ODataErrorContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;

public class CustomOdataDebugCallback implements ODataErrorCallback{

    final static Logger logger=Logger.getLogger(CustomOdataDebugCallback.class);

    @Override
    public ODataResponse handleError(ODataErrorContext context) throws ODataApplicationException {
        context.getException().printStackTrace();
        logger.error(context.getException().getClass().getName()+":"+context.getMessage());
        return EntityProvider.writeErrorDocument(context);
    }
}
