package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.ProfiliPredicatiRuoli;
import it.bologna.ausl.model.entities.baborg.QProfiliPredicatiRuoli;
import it.bologna.ausl.model.entities.baborg.projections.generated.ProfiliPredicatiRuoliWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/profilipredicatiruoli", defaultProjection = ProfiliPredicatiRuoliWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "profilipredicatiruoli", path = "profilipredicatiruoli", exported = false, excerptProjection = ProfiliPredicatiRuoliWithPlainFields.class)
public interface ProfiliPredicatiRuoliRepository extends
        NextSdrQueryDslRepository<ProfiliPredicatiRuoli, Integer, QProfiliPredicatiRuoli>,
        JpaRepository<ProfiliPredicatiRuoli, Integer> {
    
}
