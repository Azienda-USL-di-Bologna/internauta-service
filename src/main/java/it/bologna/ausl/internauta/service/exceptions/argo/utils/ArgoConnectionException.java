/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.exceptions.argo.utils;

/**
 *
 * @author Salo
 */
public class ArgoConnectionException extends Exception {

    public ArgoConnectionException(String message) {
        super(message);
    }

    public ArgoConnectionException(String message, Throwable t) {
        super(message, t);
    }
}
