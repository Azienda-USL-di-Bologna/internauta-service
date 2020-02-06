package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrAppartenenti;
import it.bologna.ausl.model.entities.gru.QMdrAppartenenti;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrAppartenentiWithPlainFields;
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
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdrappartenenti", defaultProjection = MdrAppartenentiWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrappartenenti", path = "mdrappartenenti", exported = false, excerptProjection = MdrAppartenentiWithPlainFields.class)
public interface MdrAppartenentiRepository extends
        NextSdrQueryDslRepository<MdrAppartenenti, Integer, QMdrAppartenenti>,
        JpaRepository<MdrAppartenenti, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_appartenenti where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
}
