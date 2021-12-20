package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.views.QMessageWithFolderView;
import it.bologna.ausl.model.entities.shpeck.views.MessageWithFolderView;
import it.bologna.ausl.model.entities.shpeck.views.projections.generated.MessageWithFolderViewWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/messagewithfolderview", defaultProjection = MessageWithFolderViewWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "messagewithfolderview", path = "messagewithfolderview", exported = false, excerptProjection = MessageWithFolderViewWithPlainFields.class)
public interface MessageWithFolderViewRepository extends
        NextSdrQueryDslRepository<MessageWithFolderView, Integer, QMessageWithFolderView>,
        JpaRepository<MessageWithFolderView, Integer>,
        CrudRepository<MessageWithFolderView, Integer> {
}
