package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.DocList;
import it.bologna.ausl.model.entities.scripta.QDocList;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocListWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/doclist", defaultProjection = DocListWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "doclist", path = "doclist", exported = false, excerptProjection = DocListWithPlainFields.class)
public interface DocListRepository extends
        NextSdrQueryDslRepository<DocList, Integer, QDocList>,
        JpaRepository<DocList, Integer> {
}