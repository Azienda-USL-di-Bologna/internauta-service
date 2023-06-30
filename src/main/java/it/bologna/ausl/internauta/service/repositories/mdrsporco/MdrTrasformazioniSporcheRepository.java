package it.bologna.ausl.internauta.service.repositories.mdrsporco;

import it.bologna.ausl.model.entities.mdrsporco.MdrTrasformazioniSporche;
import it.bologna.ausl.model.entities.mdrsporco.QMdrTrasformazioniSporche;
import it.bologna.ausl.model.entities.mdrsporco.projections.generated.MdrTrasformazioniSporcheWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author mido
 */
@NextSdrRepository(repositoryPath = "${mdrsporco.mapping.url.root}/mdrtrasformazionisporche", defaultProjection = MdrTrasformazioniSporcheWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "mdrtrasformazioni", path = "mdrtrasformazioni", exported = false, excerptProjection = MdrTrasformazioniSporcheWithPlainFields.class)
public interface MdrTrasformazioniSporcheRepository extends
        NextSdrQueryDslRepository<MdrTrasformazioniSporche, Integer, QMdrTrasformazioniSporche>,
        JpaRepository<MdrTrasformazioniSporche, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM mdr_sporco.mdr_trasformazioni where id_azienda = ?1", nativeQuery = true)
    public void deleteByIdAzienda(Integer idAzienda);
    
    @Query(value = "select max(progressivo_riga) FROM mdr_sporco.mdr_trasformazioni where id_azienda = ?1", nativeQuery = true)
    public Integer getLastProgressivoRiga(Integer idAzienda);
    
}

