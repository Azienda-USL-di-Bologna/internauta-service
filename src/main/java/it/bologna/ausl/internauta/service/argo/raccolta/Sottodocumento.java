/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.raccolta;

/**
 *
 * @author Matteo Next
 */
public class Sottodocumento {
    
    private String nome; 
    
    private String nomeOriginale;
    
    private String mimeTypeOriginale;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeOriginale() {
        return nomeOriginale;
    }

    public void setNomeOriginale(String nomeOriginale) {
        this.nomeOriginale = nomeOriginale;
    }

    public String getMimeTypeOriginale() {
        return mimeTypeOriginale;
    }

    public void setMimeTypeOriginale(String mymeTypeOriginale) {
        this.mimeTypeOriginale = mymeTypeOriginale;
    }
    
}
