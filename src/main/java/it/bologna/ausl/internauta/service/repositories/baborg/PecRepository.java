package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.QPec;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/pec", defaultProjection = PecWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "pec", path = "pec", exported = false, excerptProjection = PecWithPlainFields.class)
public interface PecRepository extends
        NextSdrQueryDslRepository<Pec, Integer, QPec>,
        JpaRepository<Pec, Integer> {
    
    @Procedure("baborg.get_pec_azienda")
    String getPecAzienda(Integer idAzienda);
    
    
}
