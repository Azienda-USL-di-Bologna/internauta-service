/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.baborg.model.entities.projections;

import it.bologna.ausl.baborg.model.entities.Struttura;
import it.bologna.ausl.baborg.model.entities.Utente;
import it.bologna.ausl.baborg.model.entities.UtenteStruttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.StrutturaWithIdAzienda;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteWithIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Utente
 */
@Component
public class ProjectionBeans {
    
    @Autowired
    protected ProjectionFactory factory;

    
    public UtenteWithIdPersona getUtenteConPersona(Utente utente){
        return factory.createProjection(UtenteWithIdPersona.class, utente);
    }
    
    public UtenteStrutturaCustom getUtenteStrutturaCustom(UtenteStruttura utenteStruttura){
        return factory.createProjection(UtenteStrutturaCustom.class, utenteStruttura);
    }
    
    public StrutturaWithIdAzienda getStrutturaConAzienda(Struttura struttura){
        return factory.createProjection(StrutturaWithIdAzienda.class, struttura);
    }
}
