package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.QRecepit;
import it.bologna.ausl.model.entities.shpeck.Recepit;
import it.bologna.ausl.model.entities.shpeck.projections.generated.RecepitWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/recepit", defaultProjection = RecepitWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "recepit", path = "recepit", exported = false, excerptProjection = RecepitWithPlainFields.class)
public interface RecepitRepository extends
        NextSdrQueryDslRepository<Recepit, Integer, QRecepit>,
        JpaRepository<Recepit, Integer> {
}
