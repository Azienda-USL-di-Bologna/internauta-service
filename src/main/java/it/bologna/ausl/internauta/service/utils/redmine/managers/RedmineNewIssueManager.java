/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.managers;

import it.bologna.ausl.middelmine.builders.LoadableParametersManagerBuilder;
import it.bologna.ausl.middelmine.factories.ParametersManagerFactory;
import it.bologna.ausl.middelmine.factories.RedMineCallerManagerFactory;
import it.bologna.ausl.middelmine.factories.RedMineRequestManagerFactory;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.RedmineRequestManager;
import it.bologna.ausl.middelmine.managers.configuration.LoadableParametersManager;
import it.bologna.ausl.middelmine.rest.RedMineCallManager;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Salo
 */
public class RedmineNewIssueManager extends RedmineAbstractManager {

    public RedmineNewIssueManager(ParametersManagerInterface pm) {
        super(pm);
    }

    public void apriNuovaSegnalazione(String issue, List<MultipartFile> allegati) throws Exception {
        RedMineCallManager redMineCallerManager = RedMineCallerManagerFactory.getRedMineCallerManager(parametersManager);
        ResponseEntity<String> postNewIssue = redMineCallerManager.postNewIssue(issue, allegati);
    }

}
