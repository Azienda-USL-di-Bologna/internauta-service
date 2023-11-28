package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.MessageAddress;
import it.bologna.ausl.model.entities.shpeck.QMessageAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageAddressWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/messageaddress", defaultProjection = MessageAddressWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "messageaddress", path = "messageaddress", exported = false, excerptProjection = MessageAddressWithPlainFields.class)
public interface MessageAddressRepository extends
        NextSdrQueryDslRepository<MessageAddress, Integer, QMessageAddress>, 
        JpaRepository<MessageAddress, Integer> {
    
    @Query(value = " SELECT ma.message "
            + "FROM shpeck.messages_addresses ma JOIN shpeck.messages_addresses ma2 ON ma.message = ma2.message "
            + "WHERE ma.address  = ?1 AND ma.address_role = 'FROM' and ma2.address = ?2 AND ma2.address_role = 'TO'", nativeQuery = true)
    public List<Integer> getIdMessagesByAddressFromAndAddressTO(
            @Param("addressFrom") Integer addressFrom,
            @Param("addressTO") Integer addressTO);
   
}
