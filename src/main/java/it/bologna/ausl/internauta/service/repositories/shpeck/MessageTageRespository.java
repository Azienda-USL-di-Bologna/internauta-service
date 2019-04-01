package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.QMessageTag;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageTagWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/messagetag", defaultProjection = MessageTagWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "tag", path = "tag", exported = false, excerptProjection = MessageTagWithPlainFields.class)
public interface MessageTageRespository extends
        NextSdrQueryDslRepository<MessageTag, Integer, QMessageTag>, 
        JpaRepository<MessageTag, Integer> {
}
