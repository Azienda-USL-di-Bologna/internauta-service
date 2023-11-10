package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.scripta.MessageDoc;
import it.bologna.ausl.model.entities.scripta.QMessageDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.MessageDocWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/messagedoc", defaultProjection = MessageDocWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "messagedoc", path = "messagedoc", exported = false, excerptProjection = MessageDocWithPlainFields.class)
public interface MessageDocRepository extends
        NextSdrQueryDslRepository<MessageDoc, Integer, QMessageDoc>,
        JpaRepository<MessageDoc, Integer> {

}
