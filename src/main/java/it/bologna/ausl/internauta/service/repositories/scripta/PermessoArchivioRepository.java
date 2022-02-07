package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.PermessoArchivioWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/permessoarchivio", defaultProjection = PermessoArchivioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "permessoarchivio", path = "permessoarchivio", exported = false, excerptProjection = PermessoArchivioWithPlainFields.class)
public interface PermessoArchivioRepository extends
        NextSdrQueryDslRepository<PermessoArchivio, Integer, QPermessoArchivio>, 
        JpaRepository<PermessoArchivio, Integer> {

}
