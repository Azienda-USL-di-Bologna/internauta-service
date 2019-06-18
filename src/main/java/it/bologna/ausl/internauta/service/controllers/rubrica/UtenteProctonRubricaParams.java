package it.bologna.ausl.internauta.service.controllers.rubrica;


/**
 *
 * @author gusgus
 */
public class UtenteProctonRubricaParams {
    
    private String idUtente;
    private String idStruttura;
    private String rubricaUrl;
    private String rubricaUsername;
    private String rubricaPassword;
    

    public UtenteProctonRubricaParams() {
    }

    public String getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(String idUtente) {
        this.idUtente = idUtente;
    }

    public String getIdStruttura() {
        return idStruttura;
    }

    public void setIdStruttura(String idStruttura) {
        this.idStruttura = idStruttura;
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
    
}
