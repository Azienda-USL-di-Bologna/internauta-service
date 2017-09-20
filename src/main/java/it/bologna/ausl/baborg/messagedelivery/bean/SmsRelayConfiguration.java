package it.bologna.ausl.baborg.messagedelivery.bean;


import org.springframework.http.HttpMethod;

/**
 * File di configurazione per l'invio di sms tramite servizi rest esterni, Skebby
 */
public class SmsRelayConfiguration {

    //default
    public static final String SMS_METHOD_CLASSIC="send_sms_classic";
    public static final String SMS_METHOD_BASIC="send_sms_basic";
    public static final int SMS_TEXT_MAX_CHARACTER=1500;
    public static final String DEFAULT_CHARSET="UTF-8";


    private String gateway;
    private String username;
    private String password;

    private String smsMethod;

    private HttpMethod httpMethod;

    private int maxTextCharacter;

    private String charset;

    public SmsRelayConfiguration() {
        this.smsMethod=SMS_METHOD_CLASSIC;
        this.maxTextCharacter=SMS_TEXT_MAX_CHARACTER;
        this.charset=DEFAULT_CHARSET;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Vedi costanti di classe
     * @return
     */
    public String getSmsMethod() {
        return smsMethod;
    }

    public void setSmsMethod(String smsMethod) {
        this.smsMethod = smsMethod;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public int getMaxTextCharacter() {
        return maxTextCharacter;
    }

    public void setMaxTextCharacter(int maxTextCharacter) {
        this.maxTextCharacter = maxTextCharacter;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
}
