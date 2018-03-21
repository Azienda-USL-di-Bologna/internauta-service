package it.bologna.ausl.baborg.odata.context;

import it.nextsw.olingo.context.CustomOdataJpaContextImpl;
import org.apache.olingo.odata2.api.processor.ODataContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe che estende {@link CustomOdataJpaContextImpl} aggiunge una mappa per eventuali parametri da tenere durante le esecuzioni di batch
 * Created by f.longhitano on 18/07/2017.
 */
public class CustomOdataJpaContextBase extends CustomOdataJpaContextImpl {


    private Map<String, Object> contextAdditionalData;

    public CustomOdataJpaContextBase() {
        super();
        contextAdditionalData = new HashMap<>();
    }

    public CustomOdataJpaContextBase(ODataContext oDataContext) {
        super(oDataContext);
        contextAdditionalData = new HashMap<>();
    }


    public Map<String, Object> getContextAdditionalData() {
        return contextAdditionalData;
    }

    public void setContextAdditionalData(Map<String, Object> contextAdditionalData) {
        this.contextAdditionalData = contextAdditionalData;
    }
}
