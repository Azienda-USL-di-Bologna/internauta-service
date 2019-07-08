package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.Address;
import it.bologna.ausl.model.entities.shpeck.QAddress;
import it.bologna.ausl.model.entities.shpeck.projections.generated.AddressWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/address", defaultProjection = AddressWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "address", path = "address", exported = false, excerptProjection = AddressWithPlainFields.class)
public interface AddressRepository extends
        NextSdrQueryDslRepository<Address, Integer, QAddress>, 
        JpaRepository<Address, Integer> {
}
