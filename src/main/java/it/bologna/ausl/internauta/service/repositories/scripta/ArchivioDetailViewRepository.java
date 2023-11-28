package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView;
import it.bologna.ausl.model.entities.scripta.views.QArchivioDetailView;
import it.bologna.ausl.model.entities.scripta.views.projections.generated.ArchivioDetailViewWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/archiviodetailview", defaultProjection = ArchivioDetailViewWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "archiviodetailview", path = "archiviodetailview", exported = false, excerptProjection = ArchivioDetailViewWithPlainFields.class)
public interface ArchivioDetailViewRepository extends
        NextSdrQueryDslRepository<ArchivioDetailView, Integer, QArchivioDetailView>, 
        JpaRepository<ArchivioDetailView, Integer> {

}
