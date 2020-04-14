/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.managers;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;

/**
 *
 * @author Salo
 */
public class RedmineAbstractManager {

    protected ParametersManagerInterface parametersManager;

    public RedmineAbstractManager(ParametersManagerInterface pm) {
        parametersManager = pm;
    }

}
