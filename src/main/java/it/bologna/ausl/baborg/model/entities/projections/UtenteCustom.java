/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.baborg.model.entities.projections;

import org.springframework.data.rest.core.config.Projection;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteWithIdPersona;

/**
 *
 * @author gdm
 */
@Projection(name = "UtenteCustom", types = it.bologna.ausl.baborg.model.entities.Utente.class)
public interface UtenteCustom extends UtenteWithIdPersona{

    @Override
    public Object getIdPersona();
    
}
