/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.middlemine.communications;

import it.bologna.ausl.internauta.service.utils.redmine.factories.IssueWrapperFactory;
import it.bologna.ausl.internauta.service.utils.redmine.wrappers.IssueWrapper;
import it.bologna.ausl.middelmine.builders.LoadableParametersManagerBuilder;
import it.bologna.ausl.middelmine.factories.ParametersManagerFactory;
import it.bologna.ausl.middelmine.factories.RedMineCallerManagerFactory;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.configuration.LoadableParametersManager;
import it.bologna.ausl.middelmine.rest.RedMineCallManager;
import it.bologna.ausl.model.entities.forms.Segnalazione;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Salo
 */
public class MiddleMineNewIssueManager {

    private ParametersManagerInterface pm;

    public MiddleMineNewIssueManager(ParametersManagerInterface pm) {
        this.pm = pm;
    }

    public void setPm(ParametersManagerInterface pm) {
        this.pm = pm;
    }

    private org.json.JSONObject getJsonIssueParamsBySegnalazione(Segnalazione segnalazioneUtente) throws Exception {
        try {
            IssueWrapper issueWrapper = IssueWrapperFactory.getIssueWrapper(pm);
            org.json.JSONObject issueJson = issueWrapper.buildAndReturnMiddleMineIssueBySegnalazione(segnalazioneUtente);
            System.out.println(issueJson.toString(4));
            org.json.JSONObject jsonParams = new org.json.JSONObject();
            jsonParams.put("issue", issueJson);
            return jsonParams;
        } catch (Exception ex) {
            throw new Exception("Errore durante la lettura dei parametri dalla segnlazione utente: " + ex.getMessage(), ex);
        }
    }

    private List<MultipartFile> getAllegatiListFromSegnalazione(Segnalazione segnalazioneUtente) {
        List<MultipartFile> allegati = new ArrayList<>();
        if (segnalazioneUtente.getAllegati() != null) {
            for (MultipartFile multipartFile : segnalazioneUtente.getAllegati()) {
                allegati.add(multipartFile);
            }
        } else {
            allegati = null;
        }
        return allegati;
    }

    public ResponseEntity<String> postNewIssue(Segnalazione segnalazioneUtente) throws Exception {
        try {
            org.json.JSONObject jsonParams = getJsonIssueParamsBySegnalazione(segnalazioneUtente);
            List<MultipartFile> allegati = getAllegatiListFromSegnalazione(segnalazioneUtente);
            RedMineCallManager redMineCallerManager = RedMineCallerManagerFactory.getRedMineCallerManager(pm);
            ResponseEntity<String> postNewIssue = redMineCallerManager.postNewIssue(jsonParams.toString(), allegati);
            return postNewIssue;
        } catch (Exception ex) {
            throw new Exception("Errore in fase di post della nuova segnalazione: " + ex.getMessage(), ex);
        }
    }

}
