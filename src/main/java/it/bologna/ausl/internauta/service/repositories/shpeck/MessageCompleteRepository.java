package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.views.MessageComplete;
import it.bologna.ausl.model.entities.shpeck.views.QMessageComplete;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import it.bologna.ausl.model.entities.shpeck.views.projections.generated.MessageCompleteWithPlainFields;

/**
 *
 * @author gdm
 */
@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/messagecomplete", defaultProjection = MessageCompleteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "messagecomplete", path = "messagecomplete", exported = false, excerptProjection = MessageCompleteWithPlainFields.class)
public interface MessageCompleteRepository extends
        NextSdrQueryDslRepository<MessageComplete, Integer, QMessageComplete>,
        JpaRepository<MessageComplete, Integer>,
        CrudRepository<MessageComplete, Integer> {
}
