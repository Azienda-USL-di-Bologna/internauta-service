package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.QRawMessage;
import it.bologna.ausl.model.entities.shpeck.RawMessage;
import it.bologna.ausl.model.entities.shpeck.projections.generated.RawMessageWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/rawmessage", defaultProjection = RawMessageWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "rawmessage", path = "rawmessage", exported = false, excerptProjection = RawMessageWithPlainFields.class)
public interface RawMessageRepository extends
        NextSdrQueryDslRepository<RawMessage, Integer, QRawMessage>, 
        JpaRepository<RawMessage, Integer> {
}
