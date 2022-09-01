package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.Draft;
import it.bologna.ausl.model.entities.shpeck.QDraft;
import it.bologna.ausl.model.entities.shpeck.projections.generated.DraftWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Repository per le bozze
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/draft", defaultProjection = DraftWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "draft", path = "draft", exported = false, excerptProjection = DraftWithPlainFields.class)
public interface DraftRepository extends 
        NextSdrQueryDslRepository<Draft, Integer, QDraft>,
        JpaRepository<Draft, Integer> {
    
}
