package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.shpeck.QTag;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.bologna.ausl.model.entities.shpeck.projections.generated.TagWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/tag", defaultProjection = TagWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "tag", path = "tag", exported = false, excerptProjection = TagWithPlainFields.class)
public interface TagRepository extends
        NextSdrQueryDslRepository<Tag, Integer, QTag>,
        JpaRepository<Tag, Integer> {
    
    public Tag findByidPecAndName(Pec pec, String name);
    
}
