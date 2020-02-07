package it.bologna.ausl.internauta.service.repositories.gru;

import it.bologna.ausl.model.entities.gru.MdrTrasformazioni;
import it.bologna.ausl.model.entities.gru.QMdrTrasformazioni;
import it.bologna.ausl.model.entities.gru.projections.generated.MdrTrasformazioniWithPlainFields;
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
@NextSdrRepository(repositoryPath = "${gru.mapping.url.root}/mdrtrasformazioni", defaultProjection = MdrTrasformazioniWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrtrasformazioni", path = "mdrtrasformazioni", exported = false, excerptProjection = MdrTrasformazioniWithPlainFields.class)
public interface MdrTrasformazioniRepository extends
        NextSdrQueryDslRepository<MdrTrasformazioni, Integer, QMdrTrasformazioni>,
        JpaRepository<MdrTrasformazioni, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM gru.mdr_trasformazioni where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
}
