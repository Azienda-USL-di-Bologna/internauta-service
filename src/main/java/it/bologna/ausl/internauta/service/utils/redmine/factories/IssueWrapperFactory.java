/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.factories;

import it.bologna.ausl.internauta.service.utils.redmine.wrappers.IssueWrapper;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;

/**
 *
 * @author Salo
 */
public class IssueWrapperFactory {

    public static IssueWrapper getIssueWrapper(ParametersManagerInterface parametersManager) {
        return new IssueWrapper(parametersManager);
    }

}
