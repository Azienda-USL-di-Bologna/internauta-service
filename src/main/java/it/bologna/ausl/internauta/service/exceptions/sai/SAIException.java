/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.exceptions.sai;

/**
 *
 * @author Salo
 */
public abstract class SAIException extends Exception {

    public SAIException(String message) {
        super(message);
    }

    public SAIException(String message, Throwable t) {
        super(message, t);
    }

}
