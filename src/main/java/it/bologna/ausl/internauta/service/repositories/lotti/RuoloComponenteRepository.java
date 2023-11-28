package it.bologna.ausl.internauta.service.repositories.lotti;

import it.bologna.ausl.model.entities.lotti.projections.generated.RuoloComponenteWithPlainFields;
import it.bologna.ausl.model.entities.lotti.RuoloComponente;
import it.bologna.ausl.model.entities.lotti.QRuoloComponente;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author QB
 */
@NextSdrRepository(repositoryPath = "${lotti.mapping.url.root}/ruolocomponente", defaultProjection = RuoloComponenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "ruolocomponente", path = "ruolocomponente", exported = false, excerptProjection = RuoloComponenteWithPlainFields.class)
public interface RuoloComponenteRepository extends 
        NextSdrQueryDslRepository<RuoloComponente, Integer, QRuoloComponente>,
        JpaRepository<RuoloComponente, Integer> {
    
    @Query(value = "select * from lotti.ruoli where nome = ?1", nativeQuery = true)
    public RuoloComponente findByNome(String nome);
}