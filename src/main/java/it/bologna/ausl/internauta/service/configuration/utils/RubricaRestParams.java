package it.bologna.ausl.internauta.service.configuration.utils;

import it.bologna.ausl.rubrica.maven.client.RestClient;


/**
 *
 * @author gusgus
 */
public class RubricaRestParams {
    
    private String rubricaUrl;
    private String rubricaUsername;
    private String rubricaPassword;
    private RestClient restClient;
    

    public RubricaRestParams() {
    }

    public String getRubricaUrl() {
        return rubricaUrl;
    }

    public void setRubricaUrl(String rubricaUrl) {
        this.rubricaUrl = rubricaUrl;
    }

    public String getRubricaUsername() {
        return rubricaUsername;
    }

    public void setRubricaUsername(String rubricaUsername) {
        this.rubricaUsername = rubricaUsername;
    }

    public String getRubricaPassword() {
        return rubricaPassword;
    }

    public void setRubricaPassword(String rubricaPassword) {
        this.rubricaPassword = rubricaPassword;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
    
    public void init() {
        this.restClient.init(rubricaUrl, rubricaUsername, rubricaPassword);
    }
}
