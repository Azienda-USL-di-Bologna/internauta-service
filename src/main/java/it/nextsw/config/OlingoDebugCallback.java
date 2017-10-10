package it.nextsw.config;

import org.apache.olingo.odata2.api.ODataDebugCallback;
import org.springframework.stereotype.Component;

/**
 * Created by user on 15/06/2017.
 */

@Component
public class OlingoDebugCallback implements ODataDebugCallback{


    @Override
    public boolean isDebugEnabled() {
        return true;
    }
}
