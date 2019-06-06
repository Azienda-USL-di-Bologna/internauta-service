package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/message", defaultProjection = MessageWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "message", path = "message", exported = false, excerptProjection = MessageWithPlainFields.class)
public interface MessageRespository extends
        NextSdrQueryDslRepository<Message, Integer, QMessage>,
        JpaRepository<Message, Integer>,
        CrudRepository<Message, Integer> {

    @Procedure("shpeck.get_id_azienda_repository")
    public Integer getIdAziendaRepository(
            @Param("id_message") Integer idMessage
    );

//    @Override
//    default void customize(QuerydslBindings bindings, QMessage entityPath) {
//         bindings.bind(entityPath.id).firstOptional((path, value) -> {
//             System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaa");
//             System.out.println(path);
//             System.out.println(value);
//             return null;
//         });
//    }
}
