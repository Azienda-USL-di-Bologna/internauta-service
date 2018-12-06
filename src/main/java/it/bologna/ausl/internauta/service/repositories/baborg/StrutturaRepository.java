package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/struttura", defaultProjection = StrutturaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "struttura", path = "struttura", exported = false, excerptProjection = StrutturaWithPlainFields.class)
public interface StrutturaRepository extends
        NextSdrQueryDslRepository<Struttura, Integer, QStruttura>,
        JpaRepository<Struttura, Integer> {

    // la store procedure prende in ingresso un idStruttura e restituisce una stringa in cui sono presenti gli id delle strutture antenate della struttura passata
    // la stringa è formata dagli id numerici separati dal carattere ','
    // l'id della struttura pasasta è in prima posizione, quello della struttura radice in ultima
    @Procedure("baborg.get_strutture_antenate")
    String getStruttureAntenate(Integer idStruttura);

}
