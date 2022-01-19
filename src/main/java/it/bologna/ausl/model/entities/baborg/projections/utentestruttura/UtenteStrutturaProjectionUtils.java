/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections.utentestruttura;

import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.struttura.StrutturaWithUtentiResponsabiliCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;

/**
 *
 * @author Top
 */
public class UtenteStrutturaProjectionUtils {

    @Autowired
    protected StrutturaRepository strutturaRepository;
    
    @Autowired
    protected ProjectionFactory projectionFactory;

    public String getCountUtentiStruttura(Struttura struttura) {
        return strutturaRepository.getCountUtentiStruttura(struttura.getId());
    }
    
     public StrutturaWithUtentiResponsabiliCustom getStrutturaWithUtentiReponsabili(UtenteStruttura utenteStruttura) {
        StrutturaWithUtentiResponsabiliCustom res = null;
        Struttura idStruttura = utenteStruttura.getIdStruttura();
        if (idStruttura != null) {
            res = projectionFactory.createProjection(StrutturaWithUtentiResponsabiliCustom.class, idStruttura);
        }
        return res;
    }
    
}
