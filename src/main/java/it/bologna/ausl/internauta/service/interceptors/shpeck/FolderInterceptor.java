package it.bologna.ausl.internauta.service.interceptors.shpeck;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
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
    
    @Override
    public Class getTargetEntityClass() {
        return Folder.class;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Folder folder = (Folder) entity;
        
        if (mainEntity && KrintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeFolder(folder, OperazioneKrint.CodiceOperazione.PEC_FOLDER_CREAZIONE);
        }
        
        return folder;
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Folder folder = (Folder) entity;
        Folder beforeUpdateFolder = (Folder) beforeUpdateEntity;
        
        if (mainEntity && !folder.getDescription().equals(beforeUpdateFolder.getDescription()) && KrintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeFolder(folder, OperazioneKrint.CodiceOperazione.PEC_FOLDER_RINOMINA);
        }
        
        return folder;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Folder folder = (Folder) entity;
        
        if (mainEntity && KrintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeFolder(folder, OperazioneKrint.CodiceOperazione.PEC_FOLDER_ELIMINAZIONE);
        }
    }  
}
