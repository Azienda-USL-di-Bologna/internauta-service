/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections.utente;

import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.utentestruttura.UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class UtenteProjectionUtils {

    @Autowired
    protected ProjectionFactory projectionFactory;

    public UtenteLoginCustom getUtenteRealeWithIdPersonaImpostazioniApplicazioniList(Utente utente) {
        if (utente.getUtenteReale() != null) {
            return projectionFactory.createProjection(UtenteLoginCustom.class, utente.getUtenteReale());
        } else {
            return null;
        }
    }
    
    public List<UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom> getStruttureUtenteWithAfferenzaAndReponsabili(Utente utente) {
        List<UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom> res = null;
        List<UtenteStruttura> utenteStrutturaList = utente.getUtenteStrutturaList();
        if (utenteStrutturaList != null && !utenteStrutturaList.isEmpty()) {
            res = utenteStrutturaList.stream().map(utenteStruttura -> {
                return projectionFactory.createProjection(UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom.class, utenteStruttura);
            }).collect(Collectors.toList());
        }
        return res;
    }
}
