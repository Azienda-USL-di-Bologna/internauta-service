package it.bologna.ausl.internauta.service.repositories.versatore;

import it.bologna.ausl.model.entities.versatore.QVersamentoAllegato;
import it.bologna.ausl.model.entities.versatore.VersamentoAllegato;
import it.bologna.ausl.model.entities.versatore.projections.generated.VersamentoAllegatoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/versamentoallegato", defaultProjection = VersamentoAllegatoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "versamentoallegato", path = "versamentoallegato", exported = false, excerptProjection = VersamentoAllegatoWithPlainFields.class)
public interface VersamentoAllegatoRepository extends
        NextSdrQueryDslRepository<VersamentoAllegato, Integer, QVersamentoAllegato>,
        JpaRepository<VersamentoAllegato, Integer> {

}
