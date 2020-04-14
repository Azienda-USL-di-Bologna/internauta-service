/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.managers;

import com.taskadapter.redmineapi.CustomFieldManager;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.CustomFieldDefinition;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import java.util.List;

/**
 *
 * @author Salo
 */
public class RedmineCustmoFiledsManager extends RedmineAbstractManager {

    private List<CustomFieldDefinition> customFieldsDefinitionList;

    public RedmineCustmoFiledsManager(ParametersManagerInterface pm) {
        super(pm);
    }

    private CustomFieldManager getRedmineCustomFieldManager() {
        RedmineManager rmManager = RedmineManagerFactory.createWithApiKey(parametersManager.getRedmineBaseUrl(), parametersManager.getAdminApiKey());
        return rmManager.getCustomFieldManager();
    }

    private List<CustomFieldDefinition> getCustomFields() throws RedmineException {
        if (customFieldsDefinitionList == null) {
            CustomFieldManager manager = getRedmineCustomFieldManager();
            // QUESTO DA ECCEZIONE
            customFieldsDefinitionList = manager.getCustomFieldDefinitions();
        }
        return customFieldsDefinitionList;
    }

    public Integer getCustomFieldIdByName(String name) throws RedmineException {
        List<CustomFieldDefinition> list = getCustomFields();
        Integer id = 0;
        for (CustomFieldDefinition customFieldDefinition : list) {
            if (customFieldDefinition.getName().equalsIgnoreCase(name)) {
                id = customFieldDefinition.getId();
                return id;
            }
        }
        return id;
    }

    public CustomFieldDefinition getCustomFieldDefinitionByName(String fieldName) throws RedmineException {
        List<CustomFieldDefinition> list = getCustomFields();
        CustomFieldDefinition cfd = null;
        for (CustomFieldDefinition customFieldDefinition : list) {
            if (customFieldDefinition.getName().equalsIgnoreCase(fieldName)) {
                cfd = customFieldDefinition;
            }
        }
        return cfd;
    }

    public String getCustomFieldDefaulfValue(String fieldName) throws RedmineException {
        return getCustomFieldDefinitionByName(fieldName).getDefaultValue();
    }

    public List<String> getCustomFieldAvailableValues(String fieldName) throws RedmineException {
        CustomFieldDefinition fieldDef = getCustomFieldDefinitionByName(fieldName);
        return fieldDef.getPossibleValues();
    }
}
