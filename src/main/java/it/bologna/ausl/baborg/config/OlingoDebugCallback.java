package it.bologna.ausl.baborg.config;

import org.apache.olingo.odata2.api.ODataDebugCallback;
import org.springframework.stereotype.Component;

@Component
public class OlingoDebugCallback implements ODataDebugCallback {

    @Override
    public boolean isDebugEnabled() {
        return true;
    }
}
