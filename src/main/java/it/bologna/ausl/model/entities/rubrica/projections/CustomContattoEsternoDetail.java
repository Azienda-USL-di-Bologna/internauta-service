/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithEmailListAndGruppiDelContattoListAndIdPersonaCreazioneAndIdUtenteCreazioneAndIndirizziListAndTelefonoList;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdGruppo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Salo
 */
@Projection(name = "CustomContattoEsternoDetail", types = Contatto.class)
public interface CustomContattoEsternoDetail extends ContattoWithEmailListAndGruppiDelContattoListAndIdPersonaCreazioneAndIdUtenteCreazioneAndIndirizziListAndTelefonoList {

    @Value("#{@projectionBeans.getGruppiDelContattoWithIdGruppo(target)}")
    @Override
    public List<GruppiContattiWithIdGruppo> getGruppiDelContattoList();
}
