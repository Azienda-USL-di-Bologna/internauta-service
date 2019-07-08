package it.bologna.ausl.internauta.service.repositories.ribaltoneutils;


import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.QRibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.projections.generated.RibaltoneDaLanciareWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${ribaltoneutils.mapping.url.root}/ribaltonedalanciare", defaultProjection = RibaltoneDaLanciareWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "ribaltonedalanciare", path = "ribaltonedalanciare", exported = false, excerptProjection = RibaltoneDaLanciareWithPlainFields.class)
public interface RibaltoneDaLanciareRepository extends
        NextSdrQueryDslRepository<RibaltoneDaLanciare, Integer, QRibaltoneDaLanciare>,
        JpaRepository<RibaltoneDaLanciare, Integer> {
    
    
    @Query(
    value = "SELECT distinct on(id_azienda) id FROM ribaltone_utils.ribaltoni_da_lanciare order by id_azienda, data_inserimento_riga desc", nativeQuery = true)
    List<Integer> getUltimoStato();
}
