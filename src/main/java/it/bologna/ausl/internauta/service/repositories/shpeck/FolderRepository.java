package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.QFolder;
import it.bologna.ausl.model.entities.shpeck.projections.generated.FolderWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/folder", defaultProjection = FolderWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "folder", path = "folder", exported = false, excerptProjection = FolderWithPlainFields.class)
public interface FolderRepository extends
        NextSdrQueryDslRepository<Folder, Integer, QFolder>,
        JpaRepository<Folder, Integer> {

    @Query(value = "select * from shpeck.folders where id_pec=?1 and type = ?2", nativeQuery = true)
    Folder findByIdPecAndType(@Param("id_pec") Integer idPec, @Param("type") String type);
}
