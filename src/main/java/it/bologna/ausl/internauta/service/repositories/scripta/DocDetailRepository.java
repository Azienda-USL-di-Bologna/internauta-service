package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocDetailWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
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
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/docdetail", defaultProjection = DocDetailWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "docdetail", path = "docdetail", exported = false, excerptProjection = DocDetailWithPlainFields.class)
public interface DocDetailRepository extends
        NextSdrQueryDslRepository<DocDetail, Integer, QDocDetail>, 
        JpaRepository<DocDetail, Integer> {
    
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM scripta.docs_details WHERE guid_documento = ?1", nativeQuery = true)
    public void deleteByGuidDocumento(String guidDocumento);
}
