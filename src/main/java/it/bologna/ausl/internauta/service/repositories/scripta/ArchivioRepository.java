package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional
    @Modifying
    @Query(value = "update scripta.archivi set stato = ?1 where id_archivio_radice = ?2", nativeQuery = true)
    public void chiudiRiapriArchivioRadice(String stato, Integer idArchivioRadice);
    
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM scripta.archivi WHERE stato = ?1 AND id_archivio_radice = ?2", nativeQuery = true)
    public void eliminaBozzeDaArchivioRadice(String stato, Integer idArchivioRadice);

//    @Procedure("permessi.calcola_permessi_espliciti")
//    public void calcolaPermessiEsplicitiGerarchia(
//        @Param("id_archivio") Integer idArchivio
//    );
    @Query(value = "select permessi.calcola_permessi_espliciti(a.id) from scripta.archivi a where id_archivio_radice = ?1",
            nativeQuery = true)
    public void calcolaPermessiEsplicitiGerarchia(
            Integer idArchivioRadice
    );
    
    @Query(value = "select permessi.calcola_permessi_espliciti(?1)",
            nativeQuery = true)
    public void calcolaPermessiEspliciti(
            Integer idArchivio
    );
    
    @Query(value = "select * from permessi.copia_permessi_archivi(?1, ?2)",
            nativeQuery = true)
    public void copiaPermessiArchivi(
            Integer idArchivioCopiato,
            Integer idArchivioCopia
    );
    
    @Query(value = "select scripta.aggiorna_gerarchia_entita_archivio_radice(?1)",
            nativeQuery = true)
    public void calcolaGerarchiaArchivio(
            Integer idArchivioRadice
    );
    
    @Procedure("permessi.copia_permessi_e_rendi_fascicolo")
    public void copiaPermessiRendiFascicolo(
            @Param("id_archivio") Integer id_archivio
    );
    
    @Query(value = "SELECT id FROM scripta.archivi a WHERE id_archivio_radice = ?1", nativeQuery = true)
    public Set<Integer> getSetAlberaturaArchivioRadice(
            Integer idArchivioRadice
    );
    
    @Query(value = "SELECT * FROM scripta.archivi a WHERE numerazione_gerarchica = ?1 and id_azienda = ?2", nativeQuery = true)
    public Archivio findByNumerazioneGerarchicaAndIdAzienda(String numerazioneGerarchica, Integer idAzienda);
    
    public List<Archivio> findByIdArchivioPadre(Archivio idArchivioPadre);
    
    public List<Archivio> findByIdArchivioPadreAndStatoIsNot(Archivio idArchivioPadre, String stato);
    
    @Query(value = "SELECT * FROM scripta.archivi a WHERE id_archivio_padre = ?1 and oggetto like ?2", nativeQuery = true)
    public Archivio findByPadreAndPatternOggetto(Integer idArchivioPadre, String patternOggetto);
    
    @Query(value = "select * from scripta.numera_tutti_documents_archivio_radice(?1, ?2, ?3);", nativeQuery = true)
    public void numeraTuttiDocumentsArchivioRadice(Integer idArchivio, Integer idPersona, Integer idStruttura);
    
    @Query(value = "select * from scripta.numera_tutti_documents_archivio_radice(?1, ?2);", nativeQuery = true)
    public void numeraTuttiDocumentsArchivioRadice(Integer idArchivio, Integer idPersona);
}
