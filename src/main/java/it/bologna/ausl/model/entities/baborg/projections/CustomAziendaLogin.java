/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Azienda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "CustomAziendaLogin", types = Azienda.class)
public interface CustomAziendaLogin {
    
    public Integer getId();
    
    public String getCodice();
    
    public String getNome();
    
    public String getDescrizione();

    public String getAoo();    
    
    @Value("{@projectionBeans.getUrlCommand(target)}")
    public String getUrlCommand();    
    
    
}
