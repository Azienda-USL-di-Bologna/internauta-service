/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.middlemine.communications;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Salo
 */
public class MiddleMineNewIssueResponseManager {

    public MiddleMineNewIssueResponseManager() {
    }

    public Integer getNewIssueIdByResponse(ResponseEntity<String> response) {
        String body = response.getBody();
        JSONObject jo = new JSONObject(body);
        JSONArray fullResponse = (JSONArray) jo.get("FullResponse");
        JSONObject issueDetailContainer = (JSONObject) fullResponse.get(0);
        JSONObject issue = (JSONObject) issueDetailContainer.get("issue");
        int id = issue.getInt("id");
        return id;
    }

}
