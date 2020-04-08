/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.managers;

import com.taskadapter.redmineapi.ProjectManager;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Project;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;

/**
 *
 * @author Salo
 */
public class RedmineProjectManager extends RedmineAbstractManager {

    final String BABEL_PROJECT_KEY = "babel";

    public RedmineProjectManager(ParametersManagerInterface pm) {
        super(pm);
    }

    public Project getBabelProject() throws RedmineException {
        RedmineManager rmManager = RedmineManagerFactory.createWithApiKey(parametersManager.getRedmineBaseUrl(), parametersManager.getAdminApiKey());
        return rmManager.getProjectManager().getProjectByKey(BABEL_PROJECT_KEY);

    }

    public Integer getBabelProjectId() throws RedmineException {
        return getBabelProject().getId();
    }

}
