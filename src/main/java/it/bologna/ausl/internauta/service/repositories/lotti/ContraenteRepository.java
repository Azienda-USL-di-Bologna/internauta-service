package it.bologna.ausl.internauta.service.repositories.lotti;

import it.bologna.ausl.model.entities.lotti.projections.generated.ContraenteWithPlainFields;
import it.bologna.ausl.model.entities.lotti.Contraente;
import it.bologna.ausl.model.entities.lotti.QContraente;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author QB
 */
@NextSdrRepository(repositoryPath = "${lotti.mapping.url.root}/contraente", defaultProjection = ContraenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "contraente", path = "contraente", exported = false, excerptProjection = ContraenteWithPlainFields.class)
public interface ContraenteRepository extends 
        NextSdrQueryDslRepository<Contraente, Integer, QContraente>,
        JpaRepository<Contraente, Integer> {
    
    @Query(value = "select * from lotti.contraente where nome = ?1", nativeQuery = true)
    public Contraente findByNome(String nome);
}