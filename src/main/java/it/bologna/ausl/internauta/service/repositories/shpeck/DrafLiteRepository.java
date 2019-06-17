/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.views.DraftLite;
import it.bologna.ausl.model.entities.shpeck.views.QDraftLite;
import it.bologna.ausl.model.entities.shpeck.views.projections.generated.DraftLiteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/draftlite", defaultProjection = DraftLiteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "draftlite", path = "draftlite", exported = false, excerptProjection = DraftLiteWithPlainFields.class)
public interface DrafLiteRepository extends 
        NextSdrQueryDslRepository<DraftLite, Integer, QDraftLite>,
        JpaRepository<DraftLite, Integer>{
    
}
