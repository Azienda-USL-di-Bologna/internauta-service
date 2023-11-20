package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.views.QMessageWithTagView;
import it.bologna.ausl.model.entities.shpeck.views.MessageWithTagView;
import it.bologna.ausl.model.entities.shpeck.views.projections.generated.MessageWithTagViewWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/messagewithtagview", defaultProjection = MessageWithTagViewWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "messagewithtagview", path = "messagewithtagview", exported = false, excerptProjection = MessageWithTagViewWithPlainFields.class)
public interface MessageWithTagViewRepository extends
        NextSdrQueryDslRepository<MessageWithTagView, Integer, QMessageWithTagView>,
        JpaRepository<MessageWithTagView, Integer>,
        CrudRepository<MessageWithTagView, Integer> {
}
