/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.factories;

import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineAbstractManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineCustmoFiledsManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineIssueStatusManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineNewIssueManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineProjectManager;
import it.bologna.ausl.internauta.service.utils.redmine.managers.RedmineTrackerManager;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;

/**
 *
 * @author Salo
 */
public class RedmineManagerFactory {

    public static RedmineAbstractManager getRedmineCustomFieldManager(ParametersManagerInterface parametersManager) {
        return new RedmineCustmoFiledsManager(parametersManager);
    }

    public static RedmineAbstractManager getRedmineNewIssueManager(ParametersManagerInterface parametersManager) {
        return new RedmineNewIssueManager(parametersManager);
    }

    public static RedmineAbstractManager getRedmineProjectManager(ParametersManagerInterface parametersManager) {
        return new RedmineProjectManager(parametersManager);
    }

    public static RedmineAbstractManager getRedmineIssueStatusManager(ParametersManagerInterface parametersManager) {
        return new RedmineIssueStatusManager(parametersManager);
    }

    public static RedmineAbstractManager getRedmineTrakManager(ParametersManagerInterface parametersManager) {
        return new RedmineTrackerManager(parametersManager);
    }
}
