package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author MicheleD'Onza
 * classe che si usa per caricarsi dal db o da redis i parametri per le chiamate
 * 
 */
public class InadParameters {

    public InadParameters() {
    }
    
    
    private String kid;
    private String issuer;
    private String subject;
    private String audience;
    private String urlVoucher;
    private String alg;
    private String typ;
    private Integer delta;
    private String urlDocomicilioDigitale;

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getUrlVoucher() {
        return urlVoucher;
    }

    public void setUrlVoucher(String urlVoucher) {
        this.urlVoucher = urlVoucher;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }

    public String getUrlDocomicilioDigitale() {
        return urlDocomicilioDigitale;
    }

    public void setUrlDocomicilioDigitale(String urlDocomicilioDigitale) {
        this.urlDocomicilioDigitale = urlDocomicilioDigitale;
    }    
    
    @JsonIgnore
    public static InadParameters build(Integer idAzienda,ParametriAziendeReader parametriAziendeReader, ObjectMapper objectMapper){
        List<ParametroAziende> parameters = parametriAziendeReader.getParameters(
                "inad",
                new Integer[]{idAzienda},
                new String[]{Applicazione.Applicazioni.rubrica.toString()});
        if (parameters.size() == 1){
            ParametroAziende parametroAziende = parameters.get(0);
            InadParameters inadParameters = objectMapper.convertValue(parametroAziende, InadParameters.class);
            return inadParameters;
        }
        return null;
    }
    
    
    /**
     * 
     * Funzione che ritorna il client assertion cioè la stringa che ti consente di ottenere il Voucher
     * @return clientAssertion
     */
    @JsonIgnore
    public String generateClientAssertion(){
        String clientAssertion = "";
        
        return clientAssertion;
    }
}
