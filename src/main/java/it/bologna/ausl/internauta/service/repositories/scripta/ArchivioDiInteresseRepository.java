package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.ArchivioDiInteresse;
import it.bologna.ausl.model.entities.scripta.QArchivioDiInteresse;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDiInteresseWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/archiviodiinteresse", defaultProjection = ArchivioDiInteresseWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "archiviodiinteresse", path = "archiviodiinteresse", exported = false, excerptProjection = ArchivioDiInteresseWithPlainFields.class)
public interface ArchivioDiInteresseRepository extends
        NextSdrQueryDslRepository<ArchivioDiInteresse, Integer, QArchivioDiInteresse>,
        JpaRepository<ArchivioDiInteresse, Integer> {
    
    @Query(value = "select scripta.aggiungi_archivio_recente(?1, ?2)", nativeQuery = true)
    public void aggiungiArchivioRecente(
        Integer idArchivio,
        Integer idPersona
    );
}
