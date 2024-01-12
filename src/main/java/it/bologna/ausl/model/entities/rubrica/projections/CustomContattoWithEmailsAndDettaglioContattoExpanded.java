/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithEmailListAndIdPersonaCreazioneAndIndirizziListAndTelefonoList;
import it.bologna.ausl.model.entities.rubrica.projections.generated.EmailWithIdDettaglioContatto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author utente
 */
@Projection(name = "CustomContattoWithEmailsAndDettaglioContattoExpanded", types = Contatto.class)
public interface CustomContattoWithEmailsAndDettaglioContattoExpanded extends ContattoWithEmailListAndIdPersonaCreazioneAndIndirizziListAndTelefonoList{
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getEmailList', 'EmailWithIdDettaglioContatto')}")
    @Override
    public List<EmailWithIdDettaglioContatto> getEmailList();
}
