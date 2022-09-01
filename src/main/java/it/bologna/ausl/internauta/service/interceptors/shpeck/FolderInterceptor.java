package it.bologna.ausl.internauta.service.interceptors.shpeck;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.nextsw.common.annotations.NextSdrInterceptor;
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
@NextSdrInterceptor(name = "folder-interceptor")
public class FolderInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FolderInterceptor.class);
    
    @Autowired
    private KrintShpeckService krintShpeckService;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Override
    public Class getTargetEntityClass() {
        return Folder.class;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Folder folder = (Folder) entity;
        
        if (mainEntity && krintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeFolder(folder, OperazioneKrint.CodiceOperazione.PEC_FOLDER_CREAZIONE);
        }
        
        return folder;
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Folder folder = (Folder) entity;
        Folder beforeUpdateFolder;
        try {
            beforeUpdateFolder = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, Folder.class);

        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }
        
        if (mainEntity && !folder.getDescription().equals(beforeUpdateFolder.getDescription()) && krintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeFolder(folder, OperazioneKrint.CodiceOperazione.PEC_FOLDER_RINOMINA);
        }
        
        return folder;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Folder folder = (Folder) entity;
        
        if (mainEntity && krintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeFolder(folder, OperazioneKrint.CodiceOperazione.PEC_FOLDER_ELIMINAZIONE);
        }
    }  
}
