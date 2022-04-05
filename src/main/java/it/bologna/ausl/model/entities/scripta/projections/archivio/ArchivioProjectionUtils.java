/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.scripta.projections.archivio;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.model.entities.scripta.projections.*;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.ArchivioDetailInterface;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Matteo
 */
@Component
public class ArchivioProjectionUtils {

    @Autowired
    PermessoArchivioRepository permessoArchivioRepository;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    public Boolean getIsArchivioNero(ArchivioDetail archivio) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        BooleanExpression filter = QPermessoArchivio.permessoArchivio.idArchivioDetail.id.eq(archivio.getId())
                .and(QPermessoArchivio.permessoArchivio.idPersona.id.eq(persona.getId()))
                .and(QPermessoArchivio.permessoArchivio.idAzienda.id.eq((archivio.getIdAzienda().getId())))
                .and(QPermessoArchivio.permessoArchivio.dataCreazione.eq(archivio.getDataCreazione()));
        PermessoArchivio pA = new PermessoArchivio();
        Iterable<PermessoArchivio> permessiArchivi = permessoArchivioRepository.findAll(filter);
        return !(permessiArchivi.iterator().hasNext());
    }

    public Boolean getIsArchivioNeroView(ArchivioDetailView archivio) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        BooleanExpression filter = QPermessoArchivio.permessoArchivio.idArchivioDetail.id.eq(archivio.getId())
                .and(QPermessoArchivio.permessoArchivio.idPersona.id.eq(persona.getId()))
                .and(QPermessoArchivio.permessoArchivio.idAzienda.id.eq((archivio.getIdAzienda().getId())))
                .and(QPermessoArchivio.permessoArchivio.dataCreazione.eq(archivio.getDataCreazione()));
        PermessoArchivio pA = new PermessoArchivio();
        Iterable<PermessoArchivio> permessiArchivi = permessoArchivioRepository.findAll(filter);
        return !(permessiArchivi.iterator().hasNext());
    }

}
