package it.bologna.ausl.internauta.service.repositories.scrivania;

import it.bologna.ausl.model.entities.scrivania.QRichiestaSmartWorking;
import it.bologna.ausl.model.entities.scrivania.RichiestaSmartWorking;
import it.bologna.ausl.model.entities.scrivania.projections.generated.RichiestaSmartWorkingWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author gusgus
 */
@NextSdrRepository(repositoryPath = "${scrivania.mapping.url.root}/richiestasmartworking", defaultProjection = RichiestaSmartWorkingWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "richiestasmartworking", path = "richiestasmartworking", exported = false, excerptProjection = RichiestaSmartWorkingWithPlainFields.class)
public interface RichiestaSmartWorkingRepository extends 
        NextSdrQueryDslRepository<RichiestaSmartWorking, Integer, QRichiestaSmartWorking>,
        JpaRepository<RichiestaSmartWorking, Integer>{
    
}
