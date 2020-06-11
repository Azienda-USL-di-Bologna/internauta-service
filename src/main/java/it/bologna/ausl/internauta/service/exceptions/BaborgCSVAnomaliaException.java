/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author mdonza
 */
public class BaborgCSVAnomaliaException extends Throwable{
    
    public BaborgCSVAnomaliaException(String message) {
        super(message);
    }

    public BaborgCSVAnomaliaException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
