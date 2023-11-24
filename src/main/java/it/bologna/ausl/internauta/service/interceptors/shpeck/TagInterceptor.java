package it.bologna.ausl.internauta.service.interceptors.shpeck;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "tag-interceptor")
public class TagInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagInterceptor.class);
    
    @Autowired
    private KrintShpeckService krintShpeckService;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Override
    public Class getTargetEntityClass() {
        return Tag.class;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Tag tag = (Tag) entity;
        
        if (mainEntity && krintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeTag(tag, OperazioneKrint.CodiceOperazione.PEC_TAG_CREAZIONE);
        }

        return tag;
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Tag tag = (Tag) entity;
        Tag beforeUpdateTag;
        try {
            beforeUpdateTag = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, Tag.class);

        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }
        if (mainEntity && !tag.getDescription().equals(beforeUpdateTag.getDescription()) && krintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeTag(tag, OperazioneKrint.CodiceOperazione.PEC_TAG_RINOMINA);
        }
        
        return tag;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Tag tag = (Tag) entity;
        
        if (mainEntity && krintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeTag(tag, OperazioneKrint.CodiceOperazione.PEC_TAG_ELIMINAZIONE);
        }
    }  
}
