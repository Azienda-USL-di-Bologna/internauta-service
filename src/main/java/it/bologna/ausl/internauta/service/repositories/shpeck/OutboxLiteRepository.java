/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.views.OutboxLite;
import it.bologna.ausl.model.entities.shpeck.views.QOutboxLite;
import it.bologna.ausl.model.entities.shpeck.views.projections.generated.OutboxLiteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author Top
 */
@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/outboxLite", defaultProjection = OutboxLiteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "outboxLite", path = "outboxLite", exported = false, excerptProjection = OutboxLiteWithPlainFields.class)
public interface OutboxLiteRepository extends NextSdrQueryDslRepository<OutboxLite, Integer, QOutboxLite>, JpaRepository<OutboxLite, Integer> {

}
