package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.MessageAddress;
import it.bologna.ausl.model.entities.shpeck.QMessageAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageAddressWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/messageaddress", defaultProjection = MessageAddressWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "messageaddress", path = "messageaddress", exported = false, excerptProjection = MessageAddressWithPlainFields.class)
public interface MessageAddressRepository extends
        NextSdrQueryDslRepository<MessageAddress, Integer, QMessageAddress>, 
        JpaRepository<MessageAddress, Integer> {
}
