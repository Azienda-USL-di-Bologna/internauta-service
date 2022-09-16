package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/archivio", defaultProjection = ArchivioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "archivio", path = "archivio", exported = false, excerptProjection = ArchivioWithPlainFields.class)
public interface ArchivioRepository extends
        NextSdrQueryDslRepository<Archivio, Integer, QArchivio>,
        JpaRepository<Archivio, Integer> {

    @Query(value = "select * from scripta.numerazione_archivio(?1);",
            nativeQuery = true)
    public Integer numeraArchivio(Integer idArchivio);
    
//    @Procedure("permessi.calcola_permessi_espliciti")
//    public void calcolaPermessiEspliciti(
//        @Param("id_archivio") Integer idArchivio
//    );
    @Query(value = "select permessi.calcola_permessi_espliciti(a.id) from scripta.archivi a where id_archivio_radice = ?1",
            nativeQuery = true)
    public void calcolaPermessiEspliciti(
        Integer idArchivioRadice
    );
}
