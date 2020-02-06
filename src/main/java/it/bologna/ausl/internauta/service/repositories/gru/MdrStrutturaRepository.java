package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrStruttura;
import it.bologna.ausl.model.entities.gru.QMdrStruttura;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrStrutturaWithPlainFields;
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
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdrstruttura", defaultProjection = MdrStrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrstruttura", path = "mdrstruttura", exported = false, excerptProjection = MdrStrutturaWithPlainFields.class)
public interface MdrStrutturaRepository extends
        NextSdrQueryDslRepository<MdrStruttura, Integer, QMdrStruttura>,
        JpaRepository<MdrStruttura, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_struttura where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
}
