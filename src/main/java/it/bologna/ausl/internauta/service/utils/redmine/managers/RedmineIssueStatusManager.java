/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.managers;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.IssueStatus;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import java.util.List;

/**
 *
 * @author Salo
 */
public class RedmineIssueStatusManager extends RedmineAbstractManager {

    public RedmineIssueStatusManager(ParametersManagerInterface pm) {
        super(pm);
    }

    public List<IssueStatus> getIssueStatus() throws RedmineException {
        RedmineManager rmManager = RedmineManagerFactory.createWithApiKey(parametersManager.getRedmineBaseUrl(), parametersManager.getAdminApiKey());
        return rmManager.getIssueManager().getStatuses();
    }

    public IssueStatus getIssueStatusByName(String name) throws RedmineException {
        List<IssueStatus> issueStatuses = getIssueStatus();
        for (IssueStatus issueStatus : issueStatuses) {
            if (issueStatus.getName().equals(name)) {
                return issueStatus;
            }
        }
        return null;
    }

    public Integer getIssueStatusIdByName(String name) throws RedmineException {
        return getIssueStatusByName(name).getId();
    }

}
