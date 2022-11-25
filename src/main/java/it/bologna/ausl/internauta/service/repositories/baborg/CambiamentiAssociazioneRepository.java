/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.CambiamentiAssociazione;
import it.bologna.ausl.model.entities.baborg.QCambiamentiAssociazione;
import it.bologna.ausl.model.entities.baborg.projections.generated.CambiamentiAssociazioneWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author utente
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/cambiamentiassociazione", defaultProjection = CambiamentiAssociazioneWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "cambiamentiassociazione", path = "cambiamentiassociazione", exported = false)
public interface CambiamentiAssociazioneRepository extends
        NextSdrQueryDslRepository<CambiamentiAssociazione, Integer, QCambiamentiAssociazione>,
        JpaRepository<CambiamentiAssociazione, Integer> {
}
