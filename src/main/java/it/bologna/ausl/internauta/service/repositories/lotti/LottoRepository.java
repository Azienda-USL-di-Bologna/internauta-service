package it.bologna.ausl.internauta.service.repositories.lotti;

import it.bologna.ausl.model.entities.lotti.projections.generated.LottoWithGruppiListAndIdContraenteAndIdTipologia;
import it.bologna.ausl.model.entities.lotti.Lotto;
import it.bologna.ausl.model.entities.lotti.QLotto;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author QB
 */
@NextSdrRepository(repositoryPath = "${lotti.mapping.url.root}/lotto", defaultProjection = LottoWithGruppiListAndIdContraenteAndIdTipologia.class)
@RepositoryRestResource(collectionResourceRel = "lotto", path = "lotto", exported = false, excerptProjection = LottoWithGruppiListAndIdContraenteAndIdTipologia.class)
public interface LottoRepository extends 
        NextSdrQueryDslRepository<Lotto, Integer, QLotto>,
        JpaRepository<Lotto, Integer> {
    
    @Query(value = "select * from lotti.lotti where cig = ?1", nativeQuery = true)
    public Lotto findByCIG(String cig);

    @Query(value = "select * from lotti.lotti where id_doc IN (SELECT dd.id FROM scripta.docs_details dd WHERE guid_documento = ?1)", nativeQuery = true)
    public List<Lotto> findByGuidDocumento(String guidDocumento);
}