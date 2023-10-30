package it.bologna.ausl.internauta.service.repositories.lotti;

import it.bologna.ausl.model.entities.lotti.projections.generated.TipologiaWithPlainFields;
import it.bologna.ausl.model.entities.lotti.Tipologia;
import it.bologna.ausl.model.entities.lotti.QTipologia;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author QB
 */
@NextSdrRepository(repositoryPath = "${lotti.mapping.url.root}/tipologia", defaultProjection = TipologiaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "tipologia", path = "tipologia", exported = false, excerptProjection = TipologiaWithPlainFields.class)
public interface TipologiaRepository extends 
        NextSdrQueryDslRepository<Tipologia, Integer, QTipologia>,
        JpaRepository<Tipologia, Integer> {
    
    @Query(value = "select * from lotti.tipologia where nome = ?1", nativeQuery = true)
    public Tipologia findByNome(String nome);
}