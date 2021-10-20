package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.views.DocDetailView;
import it.bologna.ausl.model.entities.scripta.views.QDocDetailView;
import it.bologna.ausl.model.entities.scripta.views.projections.generated.DocDetailViewWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/docdetailview", defaultProjection = DocDetailViewWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "docdetailview", path = "docdetailview", exported = false, excerptProjection = DocDetailViewWithPlainFields.class)
public interface DocDetailViewRepository extends
        NextSdrQueryDslRepository<DocDetailView, Integer, QDocDetailView>,
        JpaRepository<DocDetailView, Integer> {
}
