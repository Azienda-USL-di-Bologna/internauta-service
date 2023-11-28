package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/doc", defaultProjection = DocWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "doc", path = "doc", exported = false, excerptProjection = DocWithPlainFields.class)
public interface DocRepository extends
        NextSdrQueryDslRepository<Doc, Integer, QDoc>,
        JpaRepository<Doc, Integer> {
    
    public Doc findByIdEsterno(String idEsterno);
    
    @Query(value = "select * from scripta.numerazione_doc(?1);", nativeQuery = true)
    public Integer numeraDoc(Integer idDoc);
    
    @Query(value = "select * from scripta.numerazione_doc(?1, ?2);", nativeQuery = true)
    public Integer numeraDoc(Integer idDoc, Integer idPersona);
    
    @Query(value = "select * from scripta.numerazione_doc(?1, ?2, ?3);", nativeQuery = true)
    public Integer numeraDoc(Integer idDoc, Integer idPersona, Integer idStruttura);

    public Doc findByIdEsternoAndIdAzienda(String idEsterno, Azienda idAzienda);
}
