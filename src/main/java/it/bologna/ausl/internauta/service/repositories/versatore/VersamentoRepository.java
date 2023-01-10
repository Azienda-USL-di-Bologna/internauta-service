package it.bologna.ausl.internauta.service.repositories.versatore;

import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.projections.generated.VersamentoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${versatore.mapping.url.root}/versamento", defaultProjection = VersamentoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "versamento", path = "versamento", exported = false, excerptProjection = VersamentoWithPlainFields.class)
public interface VersamentoRepository extends
        NextSdrQueryDslRepository<Versamento, Integer, QVersamento>,
        JpaRepository<Versamento, Integer> {

}
