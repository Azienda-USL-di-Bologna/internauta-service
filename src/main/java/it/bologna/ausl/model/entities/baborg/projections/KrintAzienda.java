/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Azienda;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */
@Projection(name = "KrintPersona", types = Azienda.class)
public interface KrintAzienda {
    
    Integer getId();
    
    String getCodice();    
        
}
