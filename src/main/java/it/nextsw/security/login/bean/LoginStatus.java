package it.nextsw.security.login.bean;

import it.nextsw.rest.commons.message.MessageStatus;

/**
 * Semplice enum che comuncia che il login è andato a buon fine o meno
 * Created by f.longhitano on 14/07/2017.
 */
public enum LoginStatus implements MessageStatus {

    LOGIN_OK("Login ok",false),
    LOGIN_ERROR("Login error",true);

    private String message;
    private boolean error;

    LoginStatus(String message, boolean error) {
        this.message = message;
        this.error = error;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isError() {
        return error;
    }
}
