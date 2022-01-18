/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections.persona;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.utente.UtenteWithStruttureAndResponsabiliCustom;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.projections.generated.ImpostazioniApplicazioniWithPlainFields;
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
public class PersonaProjectionUtils {

    @Autowired
    protected ProjectionFactory projectionFactory;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    public List<ImpostazioniApplicazioniWithPlainFields> getImpostazioniApplicazioniListWithPlainFields(Persona persona) throws BlackBoxPermissionException {
//        setAuthenticatedUserProperties();
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Applicazione.Applicazioni applicazione = authenticatedSessionData.getApplicazione();
        List<ImpostazioniApplicazioni> impostazioniApplicazioniList = persona.getImpostazioniApplicazioniList();
        if (impostazioniApplicazioniList != null && !impostazioniApplicazioniList.isEmpty()) {
            return impostazioniApplicazioniList.stream().filter(imp -> imp.getIdApplicazione().getId().equals(applicazione.toString())).
                    map(imp -> projectionFactory.createProjection(ImpostazioniApplicazioniWithPlainFields.class, imp)
                    ).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public List<UtenteWithStruttureAndResponsabiliCustom> getUtenteWithStruttureAndResponsabiliCustom(Persona persona) {
        List<UtenteWithStruttureAndResponsabiliCustom> res = null;
        List<Utente> utenteList = persona.getUtenteList();
        if (utenteList != null && !utenteList.isEmpty()) {
            res = utenteList.stream().map(utente -> {
                return projectionFactory.createProjection(UtenteWithStruttureAndResponsabiliCustom.class, utente);
            }).collect(Collectors.toList());
        }
        return res;
    }
}
