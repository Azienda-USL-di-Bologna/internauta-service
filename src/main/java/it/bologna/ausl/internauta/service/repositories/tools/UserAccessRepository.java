package it.bologna.ausl.internauta.service.repositories.tools;

import it.bologna.ausl.model.entities.tools.QUserAccess;
import it.bologna.ausl.model.entities.tools.UserAccess;
import it.bologna.ausl.model.entities.tools.projections.generated.UserAccessWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.math.BigInteger;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${tools.mapping.url.root}/useraccess", defaultProjection = UserAccessWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "useraccess", path = "useraccess", exported = false, excerptProjection = UserAccessWithPlainFields.class)
public interface UserAccessRepository extends
        NextSdrQueryDslRepository<UserAccess, BigInteger, QUserAccess>,
        JpaRepository<UserAccess, BigInteger> {

}
