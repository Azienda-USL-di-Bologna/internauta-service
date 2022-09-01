/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.factories;

import it.bologna.ausl.internauta.service.utils.redmine.middlemine.communications.MiddleMineNewIssueManager;
import it.bologna.ausl.middelmine.builders.LoadableParametersManagerBuilder;
import it.bologna.ausl.middelmine.factories.ParametersManagerFactory;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.configuration.LoadableParametersManager;
import java.io.IOException;

/**
 *
 * @author Salo
 */
public class MiddleMineManagerFactory {

    public static MiddleMineNewIssueManager getAndBuildMiddleMineNewIssueManager() throws Exception {
        MiddleMineNewIssueManager managerToReturn;
        try {
            ParametersManagerInterface loadableParametersManager = ParametersManagerFactory.getLoadableParametersManager();
            LoadableParametersManagerBuilder.buildParams((LoadableParametersManager) loadableParametersManager);
            managerToReturn = new MiddleMineNewIssueManager(loadableParametersManager);
        } catch (Exception ex) {
            throw new Exception("Errore nella costruzione del MiddleMineNewIssueManager. " + ex.getMessage(), ex);
        }
        return managerToReturn;
    }
}
