/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.shpeck.utils;

import com.google.common.base.CaseFormat;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.model.entities.shpeck.Message;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author utente
 */
public class EmlData extends EmlHandlerResult {

    public EmlData(EmlHandlerResult emlHandlerResult) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Field[] declaredFields = emlHandlerResult.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            String fieldNameUpperCamel = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, declaredField.getName());
            String getMethodName = "get" + fieldNameUpperCamel;
            String setMethodName = "set" + fieldNameUpperCamel;
            this.getClass().getMethod(setMethodName, declaredField.getType()).invoke(this, emlHandlerResult.getClass().getMethod(getMethodName).invoke(emlHandlerResult));
        }
    }
    
    
    private Message message;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
    
}
