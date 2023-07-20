/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections.azienda;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "CustomAziendaLogin", types = Azienda.class)
public interface CustomAziendaLogin extends AziendaWithPlainFields {

    @Value("#{@aziendaProjectionUtils.getParametriAziendaFrontEnd()}")
    public Object getParametriMap();

    @Value("#{@aziendaProjectionUtils.getUrlCommands(target)}")
    public Map<String, String> getUrlCommands();

    @Value("#{@aziendaProjectionUtils.getBaseUrl(target)}")
    public String getBaseUrl();

    @Value("#{@aziendaProjectionUtils.getParametriAzienda(target)}")
    public Map<String,Object> getParametriAzienda();
}
