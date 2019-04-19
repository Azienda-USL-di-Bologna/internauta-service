package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.bologna.ausl.model.entities.shpeck.QMessageFolder;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageFolderWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/messagefolder", defaultProjection = MessageFolderWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "messagefolder", path = "messagefolder", exported = false, excerptProjection = MessageFolderWithPlainFields.class)
public interface MessageFolderRespository extends
        NextSdrQueryDslRepository<MessageFolder, Integer, QMessageFolder>, 
        JpaRepository<MessageFolder, Integer> {
}
