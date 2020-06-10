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
public class BaborgCSVException extends Throwable{

    public BaborgCSVException(String message) {
        super(message);
    }

    public BaborgCSVException(String message, Throwable cause) {
        super(message, cause);
    }
    
    
}
