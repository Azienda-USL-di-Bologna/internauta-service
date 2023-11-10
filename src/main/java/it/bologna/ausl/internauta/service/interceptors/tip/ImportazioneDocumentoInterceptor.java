package it.bologna.ausl.internauta.service.interceptors.tip;

import it.bologna.ausl.internauta.service.controllers.tip.TipUtils;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
@NextSdrInterceptor(name = "allegato-interceptor")
public class ImportazioneDocumentoInterceptor extends InternautaBaseInterceptor {

    @Override
    public Class getTargetEntityClass() {
        return ImportazioneDocumento.class;
    }
    
    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        ImportazioneDocumento importazioneDocumento = (ImportazioneDocumento) entity;
        if(additionalData != null && additionalData.get("valida") != null && Boolean.parseBoolean(additionalData.get("valida"))){
            importazioneDocumento = TipUtils.validateRow(importazioneDocumento.getIdSessioneImportazione(), importazioneDocumento);       
        }
        return importazioneDocumento;
    }
}
