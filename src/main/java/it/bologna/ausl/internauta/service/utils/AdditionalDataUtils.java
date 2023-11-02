package it.bologna.ausl.internauta.service.utils;

import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class AdditionalDataUtils {

    @Autowired
    private HttpServletRequest request;
    
    // es: additionalData=dataRiferimento=1586854870098,param2=val2
    public Map<String, String> getAdditionalData() {
        if (request != null) {
            String additionaDataString = request.getParameter("$additionalData");
            if (StringUtils.hasText(additionaDataString)) {
                return parseAdditionalDataIntoMap(additionaDataString);
            }
        }
        return null;
    }    
   
    public Map<String, String> parseAdditionalDataIntoMap(String additionalData) {
        if (additionalData != null && !additionalData.isEmpty()) {
            return Splitter.on(",").withKeyValueSeparator("=").split(additionalData);
        } else {
            return new HashMap<>();
        }
    }
}
