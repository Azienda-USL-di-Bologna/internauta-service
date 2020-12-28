package it.bologna.ausl.internauta.service.repositories.scrivania;

import it.bologna.ausl.model.entities.scrivania.Bmenu;
import it.bologna.ausl.model.entities.scrivania.QBmenu;
import it.bologna.ausl.model.entities.scrivania.projections.generated.BmenuWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author gusgus
 */
@NextSdrRepository(repositoryPath = "${scrivania.mapping.url.root}/bmenu", defaultProjection = BmenuWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "bmenu", path = "bmenu", exported = false, excerptProjection = BmenuWithPlainFields.class)
public interface BmenuRepository extends 
        NextSdrQueryDslRepository<Bmenu, Integer, QBmenu>,
        JpaRepository<Bmenu, Integer>{
    
}
