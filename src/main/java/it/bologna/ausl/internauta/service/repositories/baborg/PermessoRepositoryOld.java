package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QPermessoBaborg;
import it.bologna.ausl.model.entities.baborg.PermessoBaborg;
import it.bologna.ausl.model.entities.baborg.projections.generated.PermessoBaborgWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/permessoold", defaultProjection = PermessoBaborgWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "permessoold", path = "permessoold", exported = false, excerptProjection = PermessoBaborgWithPlainFields.class)
public interface PermessoRepositoryOld extends
        NextSdrQueryDslRepository<PermessoBaborg, Integer, QPermessoBaborg>,
        JpaRepository<PermessoBaborg, Integer> {
}
