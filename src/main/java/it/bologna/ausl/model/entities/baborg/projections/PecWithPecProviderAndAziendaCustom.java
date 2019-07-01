/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecAziendaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecWithIdPecProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */
@Projection(name = "PecWithPecProviderAndAziendaCustom", types = Pec.class)
public interface PecWithPecProviderAndAziendaCustom extends PecWithIdPecProvider{
    
    @Value("#{@projectionBeans.getPecAziendaListWithIdAzienda(target.getPecAziendaList())}")
    public List<PecAziendaWithIdAzienda> getPecAziendaList();
    
    @Value("#{target.getUsername()}")
    public String getUsername();
    
    @Value("#{target.getPassword()}")
    public String getPassword();
}
