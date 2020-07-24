/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.permessi;

import java.time.LocalDate;

/**
 *
 * @author mdonza
 */
public class PermessoError {

    private String soggetto;
    private String oggetto;
    private String predicato;
    private String ambito;
    private String tipo;
    private LocalDate dataPermesso;
    private LocalDate dataFinePermesso;

    public PermessoError(String soggetto, String oggetto, String predicato, String ambito, String tipo, LocalDate dataPermesso, LocalDate dataFinePermesso) {
        this.soggetto = soggetto;
        this.oggetto = oggetto;
        this.predicato = predicato;
        this.ambito = ambito;
        this.tipo = tipo;
        this.dataPermesso = dataPermesso;
        this.dataFinePermesso = dataFinePermesso;
    }

    public String getSoggetto() {
        return soggetto;
    }

    public void setSoggetto(String soggetto) {
        this.soggetto = soggetto;
    }

    public String getOggetto() {
        return oggetto;
    }

    public void setOggetto(String oggetto) {
        this.oggetto = oggetto;
    }

    public String getPredicato() {
        return predicato;
    }

    public void setPredicato(String predicato) {
        this.predicato = predicato;
    }

    public String getAmbito() {
        return ambito;
    }

    public void setAmbito(String ambito) {
        this.ambito = ambito;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getDataPermesso() {
        return dataPermesso;
    }

    public void setDataPermesso(LocalDate dataPermesso) {
        this.dataPermesso = dataPermesso;
    }

    public LocalDate getDataFinePermesso() {
        return dataFinePermesso;
    }

    public void setDataFinePermesso(LocalDate dataFinePermesso) {
        this.dataFinePermesso = dataFinePermesso;
    }
}
