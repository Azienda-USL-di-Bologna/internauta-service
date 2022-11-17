package it.bologna.ausl.internauta.service.repositories.versatore;

import it.bologna.ausl.model.entities.versatore.QSessioneVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.projections.generated.SessioneVersamentoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${versatore.mapping.url.root}/sessioneversamento", defaultProjection = SessioneVersamentoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "sessioneversamento", path = "sessioneversamento", exported = false, excerptProjection = SessioneVersamentoWithPlainFields.class)
public interface SessioneVersamentoRepository extends
        NextSdrQueryDslRepository<SessioneVersamento, Integer, QSessioneVersamento>,
        JpaRepository<SessioneVersamento, Integer> {

}
